package br.com.tecpontes.appfinanceiro.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.data.local.entity.PendingTransaction;
import br.com.tecpontes.appfinanceiro.databinding.ActivityDashboardBinding;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.DashboardDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.model.TransactionTransferRequestDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.ui.auth.LoginActivity;
import br.com.tecpontes.appfinanceiro.ui.help.FeatureTour;
import br.com.tecpontes.appfinanceiro.ui.help.HelpGuide;
import br.com.tecpontes.appfinanceiro.ui.help.HelpPreferences;
import br.com.tecpontes.appfinanceiro.ui.help.HelpStep;
import br.com.tecpontes.appfinanceiro.ui.notifications.NotificationHistoryActivity;
import br.com.tecpontes.appfinanceiro.notifications.PushRegistrationManager;
import br.com.tecpontes.appfinanceiro.ui.transactions.TransactionsActivity;
import br.com.tecpontes.appfinanceiro.ui.transactions.CreateTransactionDialog;
import br.com.tecpontes.appfinanceiro.utils.PrivacyMode;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.viewmodel.DashboardViewModel;
import br.com.tecpontes.appfinanceiro.viewmodel.TransactionsViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;

/**
 * Activity para tela do dashboard
 */
@AndroidEntryPoint
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private ActivityDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private TransactionsViewModel transactionsViewModel;
    @Nullable private CreateTransactionDialog createTransactionDialog;

    @Inject
    TokenManager tokenManager;

    @Inject
    PushRegistrationManager pushRegistrationManager;

    @Inject
    HelpPreferences helpPreferences;
    @Nullable private FeatureTour activeHelpTour;

    @Nullable private DashboardDto lastDashboardData;
    private int selectedPeriodPosition = 0;

    private List<TransactionListItemDto> recentApiTransactions = new ArrayList<>();
    private List<PendingTransaction> pendingTransactions = new ArrayList<>();
    private boolean skipNextResumeRefresh = true;
    private final Runnable pendingSyncReload = this::refreshDashboardData;
    private static final int[] FALLBACK_CHART_COLORS = {
            R.color.brand_primary, R.color.brand_secondary, R.color.password_strength_medium,
            R.color.money_negative, R.color.brand_primary_dark, R.color.money_neutral
    };

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configura Edge-to-Edge
        EdgeToEdge.enable(this);

        // Infla layout com ViewBinding
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BottomNavigationHelper.setup(this, R.id.navigation_dashboard);

        // Configura window insets
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());
        binding.bottomNavigation.getRoot().addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> positionAddFabAboveBottomNavigation());
        binding.addTransactionFab.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> positionAddFabAboveBottomNavigation());
        binding.getRoot().post(this::positionAddFabAboveBottomNavigation);

        // Inicializa ViewModel
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);

        // Configura SwipeRefreshLayout
        setupSwipeRefresh();

        // Configura observadores
        setupObservers();
        SessionExpiredNavigation.observe(this, tokenManager);

        // Configura listeners
        setupListeners();
        updateMaskButton();

        setupPeriodFilter();

        // Carrega dados iniciais
        loadInitialData();
        scheduleDashboardGuide();

        Log.d(TAG, "DashboardActivity criada");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) {
            updateMaskButton();
            if (lastDashboardData != null) renderDashboard(lastDashboardData);
        }
        if (dashboardViewModel != null) {
            dashboardViewModel.loadUnreadNotifications();
            // A primeira execução já carrega tudo em onCreate; nas voltas para esta tela
            // (por exemplo, depois de criar um lançamento) os dados precisam ser refeitos.
            if (skipNextResumeRefresh) {
                skipNextResumeRefresh = false;
                dashboardViewModel.loadRecentTransactions();
            } else {
                refreshDashboardData();
            }
        }
        scheduleDashboardGuide();
    }

    /** Recarrega os totais do período e a lista de lançamentos recentes. */
    private void refreshDashboardData() {
        if (binding == null) return;
        loadDashboardForPeriod(selectedPeriodPosition);
        dashboardViewModel.loadRecentTransactions();
        transactionsViewModel.requestPendingSync();
    }

    /**
     * Configura SwipeRefreshLayout
     */
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.brand_primary,
            R.color.brand_secondary,
            R.color.password_strength_medium,
            R.color.money_negative
        );

        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshDashboardData);
    }

    /**
     * Configura observadores do ViewModel
     */
    private void setupObservers() {
        // Observa estado de loading
        dashboardViewModel.loading.observe(this, loading -> {
            if (binding != null) {
                boolean refreshing = Boolean.TRUE.equals(loading);
                binding.swipeRefreshLayout.setRefreshing(refreshing);
                // O overlay só aparece na primeira carga; recargas (voltar de outra tela,
                // pull-to-refresh) usam apenas o indicador do SwipeRefreshLayout.
                boolean showOverlay = refreshing && lastDashboardData == null;
                binding.loadingView.getRoot().setVisibility(showOverlay ? View.VISIBLE : View.GONE);
            }
        });

        // Observa erros
        dashboardViewModel.error.observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        // Observa sucesso na atualização
        dashboardViewModel.refreshSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show();
            }
        });

        dashboardViewModel.dashboardData.observe(this, dashboardData -> {
            if (binding != null && dashboardData != null) {
                lastDashboardData = dashboardData;
                renderDashboard(dashboardData);
            }
        });

        // Observa transações recentes
        dashboardViewModel.recentTransactions.observe(this, transactions -> {
            if (binding == null) return;
            recentApiTransactions = transactions != null ? transactions : new ArrayList<>();
            renderRecentTransactions();
        });

        // Lançamentos ainda na fila local: aparecem na hora e, quando o envio termina,
        // os totais são recarregados para refletir o que a API confirmou.
        dashboardViewModel.pendingTransactions.observe(this, pending -> {
            if (binding == null) return;
            int previousCount = pendingTransactions.size();
            pendingTransactions = visiblePendingTransactions(pending);
            renderRecentTransactions();
            if (pendingTransactions.size() < previousCount) {
                binding.getRoot().removeCallbacks(pendingSyncReload);
                binding.getRoot().postDelayed(pendingSyncReload, 700);
            }
        });

        dashboardViewModel.unreadNotifications.observe(this, this::renderNotificationBadge);

        transactionsViewModel.formAccounts.observe(this, accounts -> updateTransactionDialogOptions());
        transactionsViewModel.formCategories.observe(this, categories -> updateTransactionDialogOptions());
        transactionsViewModel.formCards.observe(this, cards -> updateTransactionDialogOptions());
        transactionsViewModel.formOptionsLoading.observe(this, loading -> {
            if (createTransactionDialog != null) {
                createTransactionDialog.setOptionsLoading(Boolean.TRUE.equals(loading));
            }
        });
        transactionsViewModel.formOptionsError.observe(this, error -> {
            if (createTransactionDialog != null && error != null && !error.trim().isEmpty()) {
                createTransactionDialog.showError(error);
            }
        });

    }

    /**
     * Combina os lançamentos confirmados pela API com os que ainda estão na fila
     * local, para que um lançamento recém-criado apareça imediatamente.
     */
    private void renderRecentTransactions() {
        if (binding == null) return;
        List<TransactionListItemDto> combined = new ArrayList<>(
                pendingTransactions.size() + recentApiTransactions.size());
        // A fila é ordenada por criação ascendente; no dashboard o mais novo vem primeiro.
        for (int i = pendingTransactions.size() - 1; i >= 0; i--) {
            combined.add(pendingTransactions.get(i).toListItemDto());
        }
        combined.addAll(recentApiTransactions);
        if (combined.size() > DashboardViewModel.RECENT_TRANSACTIONS_LIMIT) {
            combined = new ArrayList<>(
                    combined.subList(0, DashboardViewModel.RECENT_TRANSACTIONS_LIMIT));
        }
        LinearLayout container = binding.recentTransactionsContainer;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (TransactionListItemDto item : combined) {
            View row = inflater.inflate(R.layout.item_recent_transaction, container, false);
            // Reaproveita o binding do adapter (descrição, categoria, valor e data).
            new RecentTransactionsAdapter.ViewHolder(row).bind(item);
            container.addView(row);
        }
        updateTransactionsVisibility(combined.isEmpty());
    }

    /** Mantém apenas as pendências do usuário logado, como na tela de transações. */
    @NonNull
    private List<PendingTransaction> visiblePendingTransactions(
            @Nullable List<PendingTransaction> pending) {
        List<PendingTransaction> visible = new ArrayList<>();
        if (pending == null) return visible;
        String owner = tokenManager.getUserIdentity();
        for (PendingTransaction item : pending) {
            if (item == null) continue;
            if (owner == null || !owner.equals(item.ownerEmail)) continue;
            visible.add(item);
        }
        return visible;
    }

    private void renderNotificationBadge(@Nullable Integer count) {
        if (binding == null) return;
        int unread = count == null ? 0 : count;
        if (unread <= 0) {
            binding.notificationsBadgeTextView.setVisibility(View.GONE);
            return;
        }
        binding.notificationsBadgeTextView.setText(unread > 9 ? "9+" : String.valueOf(unread));
        binding.notificationsBadgeTextView.setVisibility(View.VISIBLE);
    }

    private void updateMaskButton() {
        if (binding == null) return;
        boolean masked = PrivacyMode.isMasked(this);
        binding.maskValuesButton.setImageResource(masked
                ? R.drawable.ic_visibility_off
                : R.drawable.ic_visibility);
        binding.maskValuesButton.setContentDescription(masked
                ? "Mostrar valores"
                : "Ocultar valores");
    }

    /**
     * Configura listeners dos componentes
     */
    private void setupListeners() {
        // Botão de ver todas as transações
        binding.viewAllTransactionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionsActivity.class);
            startActivity(intent);
        });

        // Botão de ocultar/mostrar valores
        binding.maskValuesButton.setOnClickListener(v -> {
            PrivacyMode.toggle(this);
            updateMaskButton();
            if (lastDashboardData != null) renderDashboard(lastDashboardData);
        });

        // Botão de sincronizar
        binding.syncButton.setOnClickListener(v -> {
            dashboardViewModel.syncAccounts();
        });

        // Sino de notificações
        binding.notificationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationHistoryActivity.class);
            startActivity(intent);
        });

        // Abre o formulário diretamente, sem navegar para a tela de transações.
        binding.addTransactionFab.setOnClickListener(v -> showCreateTransactionDialog());
    }

    private void showCreateTransactionDialog() {
        if (createTransactionDialog != null && createTransactionDialog.isShowing()) return;

        createTransactionDialog = new CreateTransactionDialog(this,
                new CreateTransactionDialog.OnTransactionCreatedListener() {
                    @Override
                    public void onCreateTransaction(TransactionWriteDto transaction) {
                        if (createTransactionDialog == null) return;
                        createTransactionDialog.setSaving(true);
                        transactionsViewModel.createTransactionFromList(transaction,
                                new TransactionsViewModel.ActionCallback() {
                                    @Override public void onSuccess() {
                                        runOnUiThread(() -> {
                                            closeCreateTransactionDialog();
                                            Toast.makeText(DashboardActivity.this,
                                                    "Lançamento salvo. Sincronização em andamento.",
                                                    Toast.LENGTH_SHORT).show();
                                            loadDashboardForPeriod(selectedPeriodPosition);
                                            dashboardViewModel.loadRecentTransactions();
                                        });
                                    }

                                    @Override public void onError(String error) {
                                        runOnUiThread(() -> {
                                            if (createTransactionDialog != null) {
                                                createTransactionDialog.showError(error);
                                            }
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onCreateTransfer(TransactionTransferRequestDto transfer) {
                        if (createTransactionDialog == null) return;
                        createTransactionDialog.setSaving(true);
                        transactionsViewModel.createTransactionTransfer(transfer,
                                new TransactionsViewModel.ActionCallback() {
                                    @Override public void onSuccess() {
                                        runOnUiThread(() -> {
                                            closeCreateTransactionDialog();
                                            Toast.makeText(DashboardActivity.this,
                                                    "Transferência salva", Toast.LENGTH_SHORT).show();
                                            loadDashboardForPeriod(selectedPeriodPosition);
                                            dashboardViewModel.loadRecentTransactions();
                                        });
                                    }

                                    @Override public void onError(String error) {
                                        runOnUiThread(() -> {
                                            if (createTransactionDialog != null) {
                                                createTransactionDialog.showError(error);
                                            }
                                        });
                                    }
                                });
                    }
                });
        createTransactionDialog.setOnDismissListener(dialog -> createTransactionDialog = null);
        createTransactionDialog.show();
        createTransactionDialog.setOptionsLoading(true);
        transactionsViewModel.loadTransactionFormOptions();
    }

    private void updateTransactionDialogOptions() {
        if (createTransactionDialog == null) return;
        List<AccountDto> accounts = transactionsViewModel.formAccounts.getValue();
        List<CategoryDto> categories = transactionsViewModel.formCategories.getValue();
        List<CreditCardDto> cards = transactionsViewModel.formCards.getValue();
        createTransactionDialog.setOptions(accounts, categories, cards);
    }

    private void closeCreateTransactionDialog() {
        if (createTransactionDialog == null) return;
        CreateTransactionDialog dialog = createTransactionDialog;
        createTransactionDialog = null;
        dialog.dismiss();
    }

    private void positionAddFabAboveBottomNavigation() {
        if (binding == null || binding.bottomNavigation.getRoot().getHeight() == 0) return;
        ViewGroup.LayoutParams rawParams = binding.addTransactionFab.getLayoutParams();
        if (!(rawParams instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rawParams;
        int targetBottomMargin = binding.bottomNavigation.getRoot().getHeight() + dp(8);
        if (params.bottomMargin != targetBottomMargin) {
            params.bottomMargin = targetBottomMargin;
            binding.addTransactionFab.setLayoutParams(params);
        }

        // Reserva no fim do conteúdo a altura do FAB mais o equivalente a um item
        // da lista, para o último cartão poder rolar acima do botão sem cobrir valores.
        int fabHeight = binding.addTransactionFab.getHeight() > 0
                ? binding.addTransactionFab.getHeight()
                : dp(40);
        int itemHeight = dp(96);
        int contentBottomPadding = targetBottomMargin + fabHeight + itemHeight;
        View content = binding.dashboardContent;
        if (content.getPaddingBottom() != contentBottomPadding) {
            content.setPadding(
                    content.getPaddingLeft(),
                    content.getPaddingTop(),
                    content.getPaddingRight(),
                    contentBottomPadding);
        }
    }

    /**
     * Carrega dados iniciais
     */
    private void loadInitialData() {
        loadDashboardForPeriod(selectedPeriodPosition);
    }

    private void scheduleDashboardGuide() {
        if (binding == null) return;
        binding.getRoot().postDelayed(() -> {
            if (binding == null || isFinishing() || isDestroyed()) return;
            if (activeHelpTour != null && activeHelpTour.isShowing()) return;
            if (!helpPreferences.shouldShow(HelpGuide.DASHBOARD)) return;

            List<HelpStep> steps = new ArrayList<>();
            steps.add(new HelpStep(
                    binding.balanceTextView,
                    getString(R.string.help_dashboard_balance_title),
                    getString(R.string.help_dashboard_balance_message)));
            steps.add(new HelpStep(
                    binding.periodSpinner,
                    getString(R.string.help_dashboard_period_title),
                    getString(R.string.help_dashboard_period_message)));
            steps.add(new HelpStep(
                    binding.maskValuesButton,
                    getString(R.string.help_dashboard_privacy_title),
                    getString(R.string.help_dashboard_privacy_message)));
            steps.add(new HelpStep(
                    binding.addTransactionFab,
                    getString(R.string.help_dashboard_add_title),
                    getString(R.string.help_dashboard_add_message))
                    .nextLabel(getString(R.string.help_dashboard_add_action))
                    .allowTargetClick());

            activeHelpTour = FeatureTour.show(this, steps, new FeatureTour.Listener() {
                @Override public void onCompleted() {
                    finishDashboardGuideAndOpenTransactionDialog();
                }

                @Override public void onSkipped() {
                    helpPreferences.markCompleted(HelpGuide.DASHBOARD);
                }

                @Override public void onTargetClicked() {
                    finishDashboardGuideAndOpenTransactionDialog();
                }
            });
        }, 700);
    }

    private void finishDashboardGuideAndOpenTransactionDialog() {
        helpPreferences.markCompleted(HelpGuide.DASHBOARD);
        if (binding == null || isFinishing() || isDestroyed()) return;
        binding.addTransactionFab.performClick();
    }

    private void setupPeriodFilter() {
        String[] periods = {"Este mês", "Últimos 3 meses", "Últimos 12 meses"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.item_spinner_period, periods);
        adapter.setDropDownViewResource(R.layout.item_spinner_period_dropdown);
        binding.periodSpinner.setAdapter(adapter);
        binding.periodSpinner.setSelection(0, false);
        binding.periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == selectedPeriodPosition) return;
                selectedPeriodPosition = position;
                loadDashboardForPeriod(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Mantém o período selecionado.
            }
        });
    }

    private void loadDashboardForPeriod(int periodPosition) {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        if (periodPosition == 1) {
            start.add(Calendar.MONTH, -2);
        } else if (periodPosition == 2) {
            start.add(Calendar.MONTH, -11);
        }

        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        dashboardViewModel.loadDashboardData(
                Instant.ofEpochMilli(start.getTimeInMillis()).toString(),
                Instant.ofEpochMilli(end.getTimeInMillis()).toString(),
                null
        );
    }

    private void renderDashboard(@NonNull DashboardDto data) {
        double currentBalance = parseAmount(data.getCurrentSavingsBalance());
        double netBalance = parseAmount(data.getNetBalance());
        double income = parseAmount(data.getTotalIncome());
        double expenses = parseAmount(data.getTotalExpenses());
        double availableCredit = parseAmount(data.getAvailableCredit());
        double totalCreditLimit = parseAmount(data.getTotalCreditLimit());

        binding.balanceTextView.setText(formatCurrency(currentBalance));
        binding.balanceTextView.setTextColor(moneyColor(currentBalance));
        binding.accountsInfoTextView.setText(
                data.getAccountsDetail().size() + " contas de caixa · sem lançamentos futuros");
        binding.netBalanceTextView.setText(formatCurrency(netBalance));
        binding.netBalanceTextView.setTextColor(moneyColor(netBalance));
        binding.investmentsBalanceTextView.setText(formatCurrency(parseAmount(data.getInvestmentsBalance())));
        binding.effectiveBalanceTextView.setText(formatCurrency(parseAmount(data.getEffectiveSavingsBalance())));
        binding.effectiveBalanceTextView.setTextColor(moneyColor(parseAmount(data.getEffectiveSavingsBalance())));
        binding.futureExpensesTextView.setText(formatCurrency(Math.abs(parseAmount(data.getFutureExpenses()))));
        binding.creditCardDebtTextView.setText(formatCurrency(Math.abs(parseAmount(data.getCreditCardDebt()))));
        binding.incomeTextView.setText(formatCurrency(income));
        binding.expensesTextView.setText(formatCurrency(Math.abs(expenses)));
        binding.periodResultTextView.setText(formatCurrency(parseAmount(data.getTotalBalance())));
        binding.periodResultTextView.setTextColor(moneyColor(parseAmount(data.getTotalBalance())));

        String creditLabel = formatCurrency(availableCredit);
        if (totalCreditLimit > 0) {
            int percent = (int) Math.round((availableCredit / totalCreditLimit) * 100);
            creditLabel += " / " + formatCurrency(totalCreditLimit) + " (" + percent + "%)";
        }
        binding.availableCreditTextView.setText(creditLabel);

        renderMonthlyEvolution(data.getMonthlyEvolution());
        renderCategoryChart(data.getExpensesByCategory(), binding.expensesChartView,
                binding.expensesLegend, binding.expensesChartEmptyTextView);
        renderCategoryChart(data.getIncomeByCategory(), binding.incomeChartView,
                binding.incomeLegend, binding.incomeChartEmptyTextView);
    }

    private void renderMonthlyEvolution(List<DashboardDto.MonthlyEvolution> monthlyData) {
        List<MonthlyEvolutionChartView.Entry> entries = new ArrayList<>();
        if (monthlyData != null) {
            for (DashboardDto.MonthlyEvolution item : monthlyData) {
                if (item == null) continue;
                entries.add(new MonthlyEvolutionChartView.Entry(
                        item.month, parseAmount(item.income), parseAmount(item.expenses)));
            }
        }
        binding.monthlyEvolutionChartView.setEntries(entries);
        boolean empty = entries.isEmpty();
        binding.monthlyEvolutionChartView.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.monthlyEvolutionEmptyTextView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void renderCategoryChart(
            List<DashboardDto.CategoryAmount> categories,
            CategoryPieChartView chart,
            LinearLayout legend,
            TextView emptyText) {
        List<CategoryPieChartView.Entry> entries = buildCategoryEntries(categories);
        chart.setEntries(entries);
        boolean empty = entries.isEmpty();
        chart.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        populateCategoryLegend(legend, entries);
    }

    private List<CategoryPieChartView.Entry> buildCategoryEntries(
            @Nullable List<DashboardDto.CategoryAmount> categories) {
        List<CategoryPieChartView.Entry> parsed = new ArrayList<>();
        if (categories == null) return parsed;

        for (DashboardDto.CategoryAmount item : categories) {
            if (item == null) continue;
            double amount = parseAmount(item.amount);
            if (amount <= 0) continue;
            String name = item.category == null || item.category.trim().isEmpty()
                    ? "Sem categoria" : item.category;
            parsed.add(new CategoryPieChartView.Entry(
                    name, amount, parseColor(item.color,
                    getColor(FALLBACK_CHART_COLORS[parsed.size() % FALLBACK_CHART_COLORS.length]))));
        }
        Collections.sort(parsed, Comparator.comparingDouble(
                (CategoryPieChartView.Entry entry) -> entry.value).reversed());

        if (parsed.size() <= 5) return parsed;
        List<CategoryPieChartView.Entry> top = new ArrayList<>(parsed.subList(0, 5));
        double others = 0;
        for (int i = 5; i < parsed.size(); i++) others += parsed.get(i).value;
        if (others > 0) {
            top.add(new CategoryPieChartView.Entry("Outros (demais)", others, getColor(R.color.outline)));
        }
        return top;
    }

    private void populateCategoryLegend(
            LinearLayout container, List<CategoryPieChartView.Entry> entries) {
        container.removeAllViews();
        double total = 0;
        for (CategoryPieChartView.Entry entry : entries) total += entry.value;
        if (total <= 0) return;

        for (CategoryPieChartView.Entry entry : entries) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(5), 0, dp(5));

            View marker = new View(this);
            GradientDrawable markerBackground = new GradientDrawable();
            markerBackground.setColor(entry.color);
            markerBackground.setCornerRadius(dp(3));
            marker.setBackground(markerBackground);
            row.addView(marker, new LinearLayout.LayoutParams(dp(12), dp(12)));

            TextView name = new TextView(this);
            name.setText(entry.label);
            name.setTextSize(13);
            name.setTextColor(getColor(R.color.on_surface));
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            nameParams.setMargins(dp(8), 0, dp(8), 0);
            row.addView(name, nameParams);

            TextView amount = new TextView(this);
            int percent = (int) Math.round(entry.value / total * 100);
            amount.setText(percent + "% · " + formatCurrency(entry.value));
            amount.setTextSize(12);
            amount.setTextColor(getColor(R.color.on_surface_variant));
            row.addView(amount, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            container.addView(row);
        }
    }

    private double parseAmount(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            String normalized = value.trim();
            if (normalized.contains(",")) {
                normalized = normalized.replace(".", "").replace(',', '.');
            }
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String formatCurrency(double value) {
        return PrivacyMode.format(this, value);
    }

    private int moneyColor(double value) {
        return getColor(value >= 0 ? R.color.money_positive : R.color.money_negative);
    }

    private int parseColor(@Nullable String value, int fallback) {
        try {
            return value == null ? fallback : Color.parseColor(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Atualiza visibilidade da seção de transações
     */
    private void updateTransactionsVisibility(boolean isEmpty) {
        binding.recentTransactionsSection.setVisibility(View.VISIBLE);
        binding.recentTransactionsContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.emptyTransactionsTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    /**
     * Realiza logout
     */
    private void performLogout() {
        Log.d(TAG, "Realizando logout");

        pushRegistrationManager.unregisterCurrentSession();
        // Limpa token
        tokenManager.clearToken();

        // Navega para tela de login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (activeHelpTour != null) activeHelpTour.dismiss();
        if (binding != null) binding.getRoot().removeCallbacks(pendingSyncReload);
        closeCreateTransactionDialog();
        super.onDestroy();
        binding = null;
        Log.d(TAG, "DashboardActivity destruída");
    }
}
