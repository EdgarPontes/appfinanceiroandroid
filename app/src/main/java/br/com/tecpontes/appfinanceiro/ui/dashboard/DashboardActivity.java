package br.com.tecpontes.appfinanceiro.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tecpontes.appfinanceiro.databinding.ActivityDashboardBinding;
import br.com.tecpontes.appfinanceiro.model.DashboardDto;
import br.com.tecpontes.appfinanceiro.ui.auth.LoginActivity;
import br.com.tecpontes.appfinanceiro.ui.transactions.TransactionsActivity;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.DashboardViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;

/**
 * Activity para tela do dashboard
 */
@AndroidEntryPoint
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private ActivityDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;

    @Inject
    TokenManager tokenManager;

    // Adapter para transações recentes
    private RecentTransactionsAdapter transactionsAdapter;
    private int selectedPeriodPosition = 0;
    private static final int[] FALLBACK_CHART_COLORS = {
            Color.rgb(20, 160, 135), Color.rgb(58, 141, 255), Color.rgb(245, 166, 35),
            Color.rgb(221, 91, 120), Color.rgb(142, 93, 214), Color.rgb(120, 130, 145)
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

        // Configura window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa ViewModel
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Configura RecyclerView
        setupRecyclerView();

        // Configura SwipeRefreshLayout
        setupSwipeRefresh();

        // Configura observadores
        setupObservers();

        // Configura listeners
        setupListeners();

        setupPeriodFilter();

        // Carrega dados iniciais
        loadInitialData();

        Log.d(TAG, "DashboardActivity criada");
    }

    /**
     * Configura RecyclerView para transações recentes
     */
    private void setupRecyclerView() {
        transactionsAdapter = new RecentTransactionsAdapter(new ArrayList<>());
        binding.recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recentTransactionsRecyclerView.setAdapter(transactionsAdapter);
    }

    /**
     * Configura SwipeRefreshLayout
     */
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadDashboardForPeriod(selectedPeriodPosition);
        });
    }

    /**
     * Configura observadores do ViewModel
     */
    private void setupObservers() {
        // Observa estado de loading
        dashboardViewModel.loading.observe(this, loading -> {
            binding.swipeRefreshLayout.setRefreshing(loading);
            binding.loadingView.getRoot().setVisibility(loading ? View.VISIBLE : View.GONE);
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
            if (dashboardData != null) {
                renderDashboard(dashboardData);
            }
        });

        // Observa transações recentes
        dashboardViewModel.recentTransactions.observe(this, transactions -> {
            if (transactions != null) {
                transactionsAdapter.updateTransactions(transactions);
                updateTransactionsVisibility(transactions.isEmpty());
            }
        });

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

        // Botão de sincronizar
        binding.syncButton.setOnClickListener(v -> {
            dashboardViewModel.syncAccounts();
        });

        // Card de adicionar transação
        binding.addTransactionCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Carrega dados iniciais
     */
    private void loadInitialData() {
        loadDashboardForPeriod(selectedPeriodPosition);
    }

    private void setupPeriodFilter() {
        String[] periods = {"Este mês", "Últimos 3 meses", "Últimos 12 meses"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
                    name, amount, parseColor(item.color, FALLBACK_CHART_COLORS[parsed.size() % 5])));
        }
        Collections.sort(parsed, Comparator.comparingDouble(
                (CategoryPieChartView.Entry entry) -> entry.value).reversed());

        if (parsed.size() <= 5) return parsed;
        List<CategoryPieChartView.Entry> top = new ArrayList<>(parsed.subList(0, 5));
        double others = 0;
        for (int i = 5; i < parsed.size(); i++) others += parsed.get(i).value;
        if (others > 0) {
            top.add(new CategoryPieChartView.Entry("Outros (demais)", others, Color.LTGRAY));
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
            amount.setTextColor(Color.DKGRAY);
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
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
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
        binding.recentTransactionsSection.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.emptyTransactionsTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    /**
     * Realiza logout
     */
    private void performLogout() {
        Log.d(TAG, "Realizando logout");

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
        super.onDestroy();
        binding = null;
        Log.d(TAG, "DashboardActivity destruída");
    }
}
