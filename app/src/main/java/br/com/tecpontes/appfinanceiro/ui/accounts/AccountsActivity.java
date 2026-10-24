package br.com.tecpontes.appfinanceiro.ui.accounts;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.data.repository.AccountsRepository;
import br.com.tecpontes.appfinanceiro.databinding.ActivityAccountsBinding;
import br.com.tecpontes.appfinanceiro.databinding.DialogAccountFormBinding;
import br.com.tecpontes.appfinanceiro.databinding.DialogBalanceHistoryBinding;
import br.com.tecpontes.appfinanceiro.databinding.DialogCreditCardFormBinding;
import br.com.tecpontes.appfinanceiro.model.AccountBalanceHistoryDto;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.AccountRequestDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardRequestDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationSummaryDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.ui.transactions.TransactionsActivity;
import br.com.tecpontes.appfinanceiro.utils.PrivacyMode;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.AccountsViewModel;
import com.google.android.material.card.MaterialCardView;
import dagger.hilt.android.AndroidEntryPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;

/** Página de contas e cartões equivalente à seção web /accounts. */
@AndroidEntryPoint
public class AccountsActivity extends AppCompatActivity {
    public static final String EXTRA_ACCOUNT_ID = "account_id";
    private ActivityAccountsBinding binding;
    private AccountsViewModel viewModel;
    private List<AccountDto> accounts = new ArrayList<>();
    private List<CreditCardDto> cards = new ArrayList<>();
    private boolean includeInactive;
    private boolean firstResume = true;
    private AccountDto selectedHistoryAccount;
    @Nullable private ReconciliationSummaryDto lastReconciliationSummary;
    private LinearLayout historyRows;
    private TextView historyState;

    @Inject TokenManager tokenManager;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAccountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BottomNavigationHelper.setup(this, R.id.navigation_accounts);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(AccountsViewModel.class);
        setupObservers();
        setupListeners();
        SessionExpiredNavigation.observe(this, tokenManager);
        viewModel.load(false);
    }

    private void setupObservers() {
        viewModel.accountsData.observe(this, values -> {
            accounts = values == null ? new ArrayList<>() : new ArrayList<>(values);
            accounts.sort(Comparator.comparing(a -> safe(a.getName()), String.CASE_INSENSITIVE_ORDER));
            renderAccounts();
        });
        viewModel.cardsData.observe(this, values -> {
            cards = values == null ? new ArrayList<>() : new ArrayList<>(values);
            cards.sort(Comparator.comparing(c -> safe(c.getName()), String.CASE_INSENSITIVE_ORDER));
            renderCards();
        });
        viewModel.reconciliationData.observe(this, summary -> {
            lastReconciliationSummary = summary;
            renderReconciliationSummary(summary);
        });
        viewModel.isLoading.observe(this, loading ->
                binding.swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(loading)));
        viewModel.errorMessage.observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                binding.errorTextView.setVisibility(View.GONE);
            } else {
                binding.errorTextView.setText(message);
                binding.errorTextView.setVisibility(View.VISIBLE);
                if (selectedHistoryAccount != null && historyState != null) {
                    historyState.setText(message);
                }
            }
        });
        viewModel.balanceHistoryData.observe(this, this::renderHistory);
    }

    private void setupListeners() {
        binding.swipeRefreshLayout.setColorSchemeResources(
                R.color.brand_primary, R.color.money_positive, R.color.money_negative);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.load(includeInactive));
        binding.showInactiveCheckBox.setOnCheckedChangeListener((button, checked) -> {
            includeInactive = checked;
            viewModel.load(includeInactive);
        });
        binding.addAccountButton.setOnClickListener(v -> showAccountForm(null));
        binding.addCardButton.setOnClickListener(v -> showCardForm(null));
        binding.recalculateButton.setOnClickListener(v -> confirmRecalculation());
        binding.viewPendingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionsActivity.class);
            intent.putExtra(TransactionsActivity.EXTRA_RECONCILIATION_FILTER, "false");
            startActivity(intent);
        });
    }

    private void renderReconciliationSummary(@Nullable ReconciliationSummaryDto summary) {
        if (summary == null || summary.pending == null || summary.pending <= 0) {
            binding.reconciliationCard.setVisibility(View.GONE);
            return;
        }
        binding.reconciliationCard.setVisibility(View.VISIBLE);
        String message = summary.pending + (summary.pending == 1 ? " lançamento pendente" : " lançamentos pendentes")
                + " · " + (summary.percent == null ? 0 : summary.percent) + "% conciliado";
        if (summary.totalPendingAmount != null) {
            message += " · " + formatCurrency(parseAmount(summary.totalPendingAmount)) + " pendente";
        }
        binding.reconciliationSummaryTextView.setText(message);
    }

    private void renderAccounts() {
        LinearLayout container = binding.accountsContainer;
        container.removeAllViews();
        binding.accountsEmptyTextView.setVisibility(accounts.isEmpty() ? View.VISIBLE : View.GONE);
        for (AccountDto account : accounts) container.addView(createAccountCard(account));
        renderSummary();
    }

    private View createAccountCard(AccountDto account) {
        MaterialCardView card = createCard();
        LinearLayout content = verticalLayout();
        content.setPadding(dp(16), dp(16), dp(16), dp(12));
        card.addView(content);

        LinearLayout titleRow = horizontalLayout();
        titleRow.addView(iconCircle(R.drawable.ic_nav_accounts),
                new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout titleColumn = verticalLayout();
        titleColumn.setPadding(dp(12), 0, 0, 0);
        titleColumn.addView(titleText(safe(account.getName()), 16));
        TextView type = secondaryText(accountTypeLabel(account.getType()));
        type.setTextSize(12);
        titleColumn.addView(type);
        titleRow.addView(titleColumn, new LinearLayout.LayoutParams(0, -2, 1));
        if (!account.isActive()) titleRow.addView(statusBadge("Inativa", true));
        content.addView(titleRow);

        LinearLayout balances = horizontalLayout();
        LinearLayout currentColumn = verticalLayout();
        TextView currentLabel = secondaryText("Saldo atual");
        currentLabel.setTextSize(11);
        currentColumn.addView(currentLabel);
        TextView currentValue = titleText(formatCurrency(account.getBalance()), 19);
        currentValue.setTextColor(moneyColor(account.getBalance()));
        currentColumn.addView(currentValue);
        balances.addView(currentColumn, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout effectiveColumn = verticalLayout();
        TextView effectiveLabel = secondaryText("Saldo final");
        effectiveLabel.setTextSize(11);
        effectiveColumn.addView(effectiveLabel);
        double effective = account.getEffectiveBalance() == null
                ? account.getBalance() : parseAmount(account.getEffectiveBalance());
        TextView effectiveValue = titleText(formatCurrency(effective), 19);
        effectiveValue.setTextColor(moneyColor(effective));
        effectiveColumn.addView(effectiveValue);
        balances.addView(effectiveColumn, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(balances, marginTop(dp(14)));

        double pending = parseAmount(account.getPendingInvoices());
        if (pending > 0) {
            TextView pendingLabel = secondaryText("Faturas pendentes afetam o saldo: −" + formatCurrency(pending));
            pendingLabel.setTextColor(getColor(R.color.money_negative));
            pendingLabel.setTextSize(12);
            content.addView(pendingLabel, marginTop(dp(6)));
        }

        String bankDetails = account.getBranch() == null && account.getAccountNumber() == null ? ""
                : "Agência " + safe(account.getBranch()) + " · Conta " + safe(account.getAccountNumber());
        if (!bankDetails.trim().isEmpty()) {
            TextView details = secondaryText(bankDetails);
            details.setTextSize(12);
            content.addView(details, marginTop(dp(4)));
        }

        content.addView(divider(), dividerParams(dp(12)));

        LinearLayout actions = horizontalLayout();
        TextView history = actionButton("Histórico", v -> showBalanceHistory(account));
        actions.addView(history, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView edit = actionButton("Editar", v -> showAccountForm(account));
        edit.setEnabled(account.isActive());
        edit.setAlpha(account.isActive() ? 1f : .45f);
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView importButton = actionButton("Importar",
                v -> showImportOptions(v, account));
        importButton.setEnabled(account.isActive() && account.getAccountNumber() != null
                && !account.getAccountNumber().trim().isEmpty());
        importButton.setAlpha(importButton.isEnabled() ? 1f : .45f);
        actions.addView(importButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        ImageView more = moreButton(v -> showAccountMenu(v, account));
        actions.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));
        content.addView(actions, marginTop(dp(6)));
        return card;
    }

    private void renderCards() {
        LinearLayout container = binding.cardsContainer;
        container.removeAllViews();
        binding.cardsEmptyTextView.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);
        for (CreditCardDto creditCard : cards) container.addView(createCreditCard(creditCard));
        renderSummary();
    }

    private void renderSummary() {
        boolean empty = accounts.isEmpty() && cards.isEmpty();
        binding.summaryCard.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) return;

        double totalBalance = 0;
        double pendingInvoices = 0;
        int activeAccounts = 0;
        for (AccountDto account : accounts) {
            if (!account.isActive()) continue;
            activeAccounts++;
            totalBalance += account.getBalance();
            pendingInvoices += parseAmount(account.getPendingInvoices());
        }

        double availableLimit = 0;
        int activeCards = 0;
        for (CreditCardDto creditCard : cards) {
            if (!creditCard.isActive()) continue;
            activeCards++;
            availableLimit += parseAmount(creditCard.getAvailableLimit());
        }

        binding.summaryBalanceTextView.setText(formatCurrency(totalBalance));
        binding.summaryBalanceTextView.setTextColor(moneyColor(totalBalance));
        binding.summaryAccountsInfoTextView.setText(activeAccounts
                + (activeAccounts == 1 ? " conta ativa" : " contas ativas"));
        binding.summaryAvailableLimitTextView.setText(formatCurrency(availableLimit));
        binding.summaryAvailableLimitTextView.setTextColor(moneyColor(availableLimit));
        binding.summaryCardsInfoTextView.setText(activeCards
                + (activeCards == 1 ? " cartão ativo" : " cartões ativos"));

        if (pendingInvoices > 0) {
            binding.summaryPendingRow.setVisibility(View.VISIBLE);
            binding.summaryPendingTextView.setText("Faturas pendentes: " + formatCurrency(pendingInvoices));
        } else {
            binding.summaryPendingRow.setVisibility(View.GONE);
        }
    }

    private View createCreditCard(CreditCardDto creditCard) {
        MaterialCardView card = createCard();
        LinearLayout content = verticalLayout();
        content.setPadding(dp(16), dp(16), dp(16), dp(12));
        card.addView(content);
        LinearLayout titleRow = horizontalLayout();
        titleRow.addView(iconCircle(R.drawable.ic_nav_cards),
                new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout titleColumn = verticalLayout();
        titleColumn.setPadding(dp(12), 0, 0, 0);
        titleColumn.addView(titleText(safe(creditCard.getName()), 16));
        TextView brand = secondaryText(safe(creditCard.getBandeira()));
        brand.setTextSize(12);
        titleColumn.addView(brand);
        titleRow.addView(titleColumn, new LinearLayout.LayoutParams(0, -2, 1));
        if (!creditCard.isActive()) titleRow.addView(statusBadge("Inativo", true));
        content.addView(titleRow);

        LinearLayout limitRow = horizontalLayout();
        LinearLayout limitColumn = verticalLayout();
        TextView limitLabel = secondaryText("Limite de crédito");
        limitLabel.setTextSize(11);
        limitColumn.addView(limitLabel);
        limitColumn.addView(titleText(formatCurrency(parseAmount(creditCard.getLimit())), 16));
        limitRow.addView(limitColumn, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout availableColumn = verticalLayout();
        TextView availableLabel = secondaryText("Disponível");
        availableLabel.setTextSize(11);
        availableColumn.addView(availableLabel);
        double availableValue = parseAmount(creditCard.getAvailableLimit());
        TextView available = titleText(formatCurrency(availableValue), 16);
        available.setTextColor(moneyColor(availableValue));
        availableColumn.addView(available);
        limitRow.addView(availableColumn, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(limitRow, marginTop(dp(14)));

        String debitName = creditCard.getDebitAccount() == null ? null : creditCard.getDebitAccount().getName();
        if (debitName == null) debitName = accountName(creditCard.getDebitAccountId());
        String days = "Fecha dia " + (creditCard.getClosingDay() == null ? "—" : creditCard.getClosingDay())
                + " · vence dia " + (creditCard.getDueDay() == null ? "—" : creditCard.getDueDay());
        TextView details = secondaryText(days + (debitName == null ? "" : "\nConta de débito: " + debitName));
        details.setTextSize(12);
        content.addView(details, marginTop(dp(6)));

        content.addView(divider(), dividerParams(dp(12)));

        LinearLayout actions = horizontalLayout();
        TextView edit = actionButton("Editar", v -> showCardForm(creditCard));
        edit.setEnabled(creditCard.isActive());
        edit.setAlpha(creditCard.isActive() ? 1f : .45f);
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));
        ImageView more = moreButton(v -> showCardMenu(v, creditCard));
        actions.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));
        content.addView(actions, marginTop(dp(6)));
        return card;
    }

    private void showAccountForm(@Nullable AccountDto account) {
        DialogAccountFormBinding form = DialogAccountFormBinding.inflate(getLayoutInflater());
        Dialog dialog = buildDialog(form.getRoot());
        form.formTitleTextView.setText(account == null ? "Nova conta bancária" : "Editar conta");
        form.nameEditText.setText(account == null ? "" : safe(account.getName()));
        form.balanceEditText.setText(account == null ? "0,00"
                : String.format(Locale.US, "%.2f", account.getBalance()));
        String[] labels = {"Conta Corrente", "Poupança", "Investimento", "Dinheiro"};
        String[] types = {"CHECKING", "SAVINGS", "INVESTMENT", "CASH"};
        setSpinnerItems(form.typeSpinner, labels);
        if (account != null) form.typeSpinner.setSelection(indexOf(types, account.getType()));
        form.branchEditText.setText(account == null ? "" : safe(account.getBranch()));
        form.numberEditText.setText(account == null ? "" : safe(account.getAccountNumber()));

        form.closeButton.setOnClickListener(v -> dialog.dismiss());
        form.cancelButton.setOnClickListener(v -> dialog.dismiss());
        form.saveButton.setText(account == null ? "Criar conta" : "Atualizar");
        form.saveButton.setOnClickListener(v -> {
            form.nameLayout.setError(null);
            form.balanceLayout.setError(null);
            String accountName = form.nameEditText.getText().toString().trim();
            if (accountName.isEmpty()) {
                form.nameLayout.setError("Informe o nome da conta");
                return;
            }
            double amount;
            try {
                amount = parseLocalizedAmount(form.balanceEditText.getText().toString());
            } catch (NumberFormatException error) {
                form.balanceLayout.setError("Informe um saldo válido");
                return;
            }
            AccountRequestDto request = new AccountRequestDto();
            request.name = accountName;
            request.balance = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
            int typeIndex = Math.max(0, form.typeSpinner.getSelectedItemPosition());
            request.type = types[typeIndex];
            request.branch = form.branchEditText.getText().toString().trim();
            request.accountNumber = form.numberEditText.getText().toString().trim();
            dialog.dismiss();
            viewModel.saveAccount(account == null ? null : account.getId(), request,
                    actionCallback(account == null ? "Conta criada." : "Conta atualizada."));
        });

        showSizedDialog(dialog, 0.88f);
    }

    private void showCardForm(@Nullable CreditCardDto card) {
        DialogCreditCardFormBinding form = DialogCreditCardFormBinding.inflate(getLayoutInflater());
        Dialog dialog = buildDialog(form.getRoot());
        form.formTitleTextView.setText(card == null ? "Novo cartão de crédito" : "Editar cartão");
        form.nameEditText.setText(card == null ? "" : safe(card.getName()));
        form.limitEditText.setText(card == null ? "0,00" : safe(card.getLimit()));
        form.brandEditText.setText(card == null ? "" : safe(card.getBandeira()));
        form.closingDayEditText.setText(card == null ? "1" : safe(card.getClosingDay()));
        form.dueDayEditText.setText(card == null ? "10" : safe(card.getDueDay()));

        List<String> accountOptions = new ArrayList<>();
        accountOptions.add("Nenhuma conta");
        for (AccountDto accountDto : accounts) accountOptions.add(safe(accountDto.getName()));
        setSpinnerItems(form.debitAccountSpinner, accountOptions.toArray(new String[0]));
        int selectedAccount = 0;
        if (card != null && card.getDebitAccountId() != null) {
            for (int i = 0; i < accounts.size(); i++) {
                if (card.getDebitAccountId().equals(accounts.get(i).getId())) selectedAccount = i + 1;
            }
        }
        form.debitAccountSpinner.setSelection(selectedAccount);

        form.closeButton.setOnClickListener(v -> dialog.dismiss());
        form.cancelButton.setOnClickListener(v -> dialog.dismiss());
        form.saveButton.setOnClickListener(v -> {
            form.nameLayout.setError(null);
            form.limitLayout.setError(null);
            form.closingDayLayout.setError(null);
            form.dueDayLayout.setError(null);

            String cardName = form.nameEditText.getText().toString().trim();
            if (cardName.length() < 2) {
                form.nameLayout.setError("Informe ao menos 2 caracteres");
                return;
            }
            double limitAmount;
            try {
                limitAmount = parseLocalizedAmount(form.limitEditText.getText().toString());
            } catch (NumberFormatException error) {
                form.limitLayout.setError("Informe um limite válido");
                return;
            }
            int closeDay;
            int paymentDay;
            try {
                closeDay = Integer.parseInt(form.closingDayEditText.getText().toString().trim());
                paymentDay = Integer.parseInt(form.dueDayEditText.getText().toString().trim());
            } catch (NumberFormatException error) {
                Toast.makeText(this, "Confira os dias da fatura.", Toast.LENGTH_LONG).show();
                return;
            }
            if (limitAmount < 0) {
                form.limitLayout.setError("O limite deve ser positivo");
                return;
            }
            if (closeDay < 1 || closeDay > 31) {
                form.closingDayLayout.setError("Informe um dia entre 1 e 31");
                return;
            }
            if (paymentDay < 1 || paymentDay > 31) {
                form.dueDayLayout.setError("Informe um dia entre 1 e 31");
                return;
            }
            CreditCardRequestDto request = new CreditCardRequestDto();
            request.name = cardName;
            request.limit = BigDecimal.valueOf(limitAmount).setScale(2, RoundingMode.HALF_UP).toPlainString();
            request.bandeira = form.brandEditText.getText().toString().trim();
            request.closingDay = closeDay;
            request.dueDay = paymentDay;
            int accountIndex = form.debitAccountSpinner.getSelectedItemPosition() - 1;
            request.debitAccountId = accountIndex >= 0 && accountIndex < accounts.size()
                    ? accounts.get(accountIndex).getId() : null;
            dialog.dismiss();
            viewModel.saveCard(card == null ? null : card.getId(), request,
                    actionCallback(card == null ? "Cartão criado." : "Cartão atualizado."));
        });

        showSizedDialog(dialog, 0.88f);
    }

    private void showAccountMenu(View anchor, AccountDto account) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(account.isActive() ? "Inativar" : "Reativar").setOnMenuItemClickListener(item -> {
            confirmAccountActive(account, !account.isActive());
            return true;
        });
        menu.getMenu().add("Excluir").setOnMenuItemClickListener(item -> {
            confirmDeleteAccount(account);
            return true;
        });
        menu.show();
    }

    private void showCardMenu(View anchor, CreditCardDto card) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(card.isActive() ? "Inativar" : "Reativar").setOnMenuItemClickListener(item -> {
            confirmCardActive(card, !card.isActive());
            return true;
        });
        menu.getMenu().add("Excluir").setOnMenuItemClickListener(item -> {
            confirmDeleteCard(card);
            return true;
        });
        menu.show();
    }

    private void confirmAccountActive(AccountDto account, boolean active) {
        new AlertDialog.Builder(this).setTitle(active ? "Reativar conta?" : "Inativar conta?")
                .setMessage("Conta: " + safe(account.getName()))
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(active ? "Reativar" : "Inativar", (dialog, which) ->
                        viewModel.setAccountActive(account.getId(), active,
                                actionCallback(active ? "Conta reativada." : "Conta inativada."))).show();
    }

    private void confirmCardActive(CreditCardDto card, boolean active) {
        new AlertDialog.Builder(this).setTitle(active ? "Reativar cartão?" : "Inativar cartão?")
                .setMessage("Cartão: " + safe(card.getName()))
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(active ? "Reativar" : "Inativar", (dialog, which) ->
                        viewModel.setCardActive(card.getId(), active,
                                actionCallback(active ? "Cartão reativado." : "Cartão inativado."))).show();
    }

    private void confirmDeleteAccount(AccountDto account) {
        new AlertDialog.Builder(this).setTitle("Excluir conta?")
                .setMessage("A conta \"" + safe(account.getName()) + "\" será inativada e deixará de aparecer nas listas ativas.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> viewModel.deleteAccount(account.getId(),
                        actionCallback("Conta excluída."))).show();
    }

    private void confirmDeleteCard(CreditCardDto card) {
        new AlertDialog.Builder(this).setTitle("Excluir cartão?")
                .setMessage("Excluir \"" + safe(card.getName()) + "\"? Cartões com faturas ou pagamentos podem não ser excluídos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> viewModel.deleteCard(card.getId(),
                        actionCallback("Cartão excluído."))).show();
    }

    private void confirmRecalculation() {
        new AlertDialog.Builder(this).setTitle("Recalcular saldos?")
                .setMessage("Os saldos das contas, os totais das faturas e o histórico diário serão recalculados.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Recalcular", (dialog, which) -> {
                    setRecalculateEnabled(false);
                    viewModel.recalculate(new AccountsRepository.ActionCallback() {
                        @Override public void onSuccess() {
                            setRecalculateEnabled(true);
                            Toast.makeText(AccountsActivity.this, "Saldos e faturas recalculados.", Toast.LENGTH_LONG).show();
                            viewModel.load(includeInactive);
                        }
                        @Override public void onError(@NonNull String message) {
                            setRecalculateEnabled(true);
                            Toast.makeText(AccountsActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }).show();
    }

    private void setRecalculateEnabled(boolean enabled) {
        binding.recalculateButton.setEnabled(enabled);
        binding.recalculateButton.setAlpha(enabled ? 1f : .4f);
    }

    private void showImportOptions(View anchor, AccountDto account) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Importar OFX").setOnMenuItemClickListener(item -> {
            openStatementImport(account, "ofx");
            return true;
        });
        menu.getMenu().add("Importar CSV").setOnMenuItemClickListener(item -> {
            openStatementImport(account, "csv");
            return true;
        });
        menu.show();
    }

    private void openStatementImport(AccountDto account, String format) {
        Intent intent = new Intent(this, StatementImportActivity.class);
        intent.putExtra(StatementImportActivity.EXTRA_ACCOUNT_ID, account.getId());
        intent.putExtra(StatementImportActivity.EXTRA_ACCOUNT_NAME, account.getName());
        intent.putExtra(StatementImportActivity.EXTRA_FORMAT, format);
        startActivity(intent);
    }

    private void showBalanceHistory(AccountDto account) {
        selectedHistoryAccount = account;
        DialogBalanceHistoryBinding historyBinding =
                DialogBalanceHistoryBinding.inflate(getLayoutInflater());
        historyRows = historyBinding.historyRowsContainer;
        historyState = historyBinding.historyStateTextView;
        historyState.setText("Carregando histórico…");
        historyBinding.historySubtitleTextView.setText(safe(account.getName()));
        Dialog dialog = buildDialog(historyBinding.getRoot());
        historyBinding.closeButton.setOnClickListener(v -> dialog.dismiss());
        historyBinding.footerCloseButton.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(ignored -> clearHistoryDialog());
        showSizedDialog(dialog, 0.72f);
        viewModel.loadHistory(account.getId());
    }

    private void renderHistory(@Nullable AccountBalanceHistoryDto data) {
        if (historyRows == null || selectedHistoryAccount == null) return;
        if (data == null) return;
        historyRows.removeAllViews();
        if (data.getHistory().isEmpty()) {
            historyState.setText("Nenhum histórico de saldo disponível para esta conta.");
            return;
        }
        historyState.setText("Evolução do saldo ao longo do tempo");
        for (AccountBalanceHistoryDto.Item item : data.getHistory()) {
            LinearLayout row = horizontalLayout();
            TextView date = bodyText(formatDate(item.date));
            date.setTextSize(12);
            row.addView(date, new LinearLayout.LayoutParams(0, -2, 1));
            TextView balance = bodyText(formatCurrency(parseAmount(item.balance)));
            balance.setTextSize(12);
            balance.setTypeface(null, Typeface.BOLD);
            balance.setGravity(Gravity.END);
            balance.setTextColor(moneyColor(parseAmount(item.balance)));
            row.addView(balance, new LinearLayout.LayoutParams(0, -2, 1));
            TextView change = bodyText(formatCurrency(parseAmount(item.change)));
            change.setTextSize(12);
            change.setGravity(Gravity.END);
            change.setTextColor(moneyColor(parseAmount(item.change)));
            row.addView(change, new LinearLayout.LayoutParams(0, -2, 1));
            row.setPadding(0, dp(10), 0, dp(10));
            historyRows.addView(row);
            historyRows.addView(divider(), new LinearLayout.LayoutParams(-1, dp(1)));
        }
    }

    private void clearHistoryDialog() {
        selectedHistoryAccount = null;
        historyRows = null;
        historyState = null;
    }

    private AccountsRepository.ActionCallback actionCallback(String successMessage) {
        return new AccountsRepository.ActionCallback() {
            @Override public void onSuccess() {
                Toast.makeText(AccountsActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                viewModel.load(includeInactive);
            }
            @Override public void onError(@NonNull String message) {
                Toast.makeText(AccountsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };
    }

    private MaterialCardView createCard() {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(getColor(R.color.surface));
        card.setRadius(dp(16));
        card.setCardElevation(dp(1));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(R.color.outline));
        return card;
    }

    private View iconCircle(int drawableRes) {
        LinearLayout circle = new LinearLayout(this);
        circle.setBackgroundResource(R.drawable.bg_icon_circle);
        circle.setGravity(Gravity.CENTER);
        ImageView icon = new ImageView(this);
        icon.setImageResource(drawableRes);
        icon.setColorFilter(getColor(R.color.brand_primary));
        icon.setContentDescription(null);
        circle.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));
        return circle;
    }

    private LinearLayout verticalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView titleText(String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.on_surface));
        text.setTextSize(size);
        text.setTypeface(null, Typeface.BOLD);
        return text;
    }

    private TextView bodyText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.on_surface));
        text.setTextSize(13);
        return text;
    }

    private TextView secondaryText(String value) {
        TextView text = bodyText(value);
        text.setTextColor(getColor(R.color.on_surface_variant));
        return text;
    }

    private TextView actionButton(String value, View.OnClickListener listener) {
        TextView text = bodyText(value);
        text.setTextColor(getColor(R.color.brand_primary));
        text.setTextSize(12);
        text.setTypeface(null, Typeface.BOLD);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(4), 0, dp(4), 0);
        text.setBackgroundResource(R.drawable.bg_text_action);
        text.setClickable(true);
        text.setFocusable(true);
        text.setOnClickListener(listener);
        return text;
    }

    private ImageView moreButton(View.OnClickListener listener) {
        ImageView more = new ImageView(this);
        more.setImageResource(R.drawable.ic_transaction_more);
        more.setColorFilter(getColor(R.color.brand_primary));
        more.setContentDescription("Mais ações");
        more.setPadding(dp(10), dp(10), dp(10), dp(10));
        more.setBackgroundResource(R.drawable.bg_text_action);
        more.setClickable(true);
        more.setFocusable(true);
        more.setOnClickListener(listener);
        return more;
    }

    private TextView statusBadge(String value, boolean error) {
        TextView text = bodyText(value);
        text.setTextSize(11);
        text.setTextColor(getColor(error ? R.color.on_error_container : R.color.on_success_container));
        text.setTypeface(null, Typeface.BOLD);
        text.setPadding(dp(8), dp(4), dp(8), dp(4));
        text.setBackgroundResource(error
                ? R.drawable.bg_status_badge_error
                : R.drawable.bg_status_badge_success);
        return text;
    }

    private View divider() {
        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.outline));
        return divider;
    }

    private LinearLayout.LayoutParams marginTop(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = margin;
        return params;
    }

    private LinearLayout.LayoutParams dividerParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.topMargin = topMargin;
        return params;
    }

    private Dialog buildDialog(View content) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        return dialog;
    }

    private void showSizedDialog(Dialog dialog, float heightFactor) {
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) return;
        int width = getResources().getDisplayMetrics().widthPixels - dp(32);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * heightFactor);
        window.setLayout(width, height);
    }

    private void setSpinnerItems(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner_form, values);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        spinner.setAdapter(adapter);
    }

    private int indexOf(String[] values, @Nullable String target) {
        if (target != null) for (int i = 0; i < values.length; i++) if (target.equalsIgnoreCase(values[i])) return i;
        return 0;
    }

    private String accountTypeLabel(@Nullable String type) {
        if ("CHECKING".equals(type)) return "Conta Corrente";
        if ("SAVINGS".equals(type)) return "Poupança";
        if ("INVESTMENT".equals(type)) return "Investimento";
        if ("CASH".equals(type)) return "Dinheiro";
        return type == null ? "Sem tipo" : type;
    }

    @Nullable private String accountName(@Nullable String accountId) {
        if (accountId == null) return null;
        for (AccountDto account : accounts) if (accountId.equals(account.getId())) return account.getName();
        return null;
    }

    private double parseLocalizedAmount(String raw) {
        String value = raw.replace("R$", "").replace(" ", "").trim();
        if (value.contains(",")) value = value.replace(".", "").replace(',', '.');
        return Double.parseDouble(value);
    }

    private double parseAmount(@Nullable String value) {
        if (value == null) return 0;
        try { return Double.parseDouble(value.replace(',', '.')); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private String formatCurrency(double value) {
        return PrivacyMode.format(this, value);
    }

    private int moneyColor(double value) {
        if (value > 0) return getColor(R.color.money_positive);
        if (value < 0) return getColor(R.color.money_negative);
        return getColor(R.color.money_neutral);
    }

    private String formatDate(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) return "—";
        try {
            Date date;
            try { date = Date.from(java.time.Instant.parse(value)); }
            catch (RuntimeException ignored) { date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value.substring(0, 10)); }
            return new SimpleDateFormat("dd MMM yyyy", new Locale("pt", "BR")).format(date);
        } catch (Exception ignored) { return value; }
    }

    private String safe(@Nullable Object value) { return value == null ? "" : String.valueOf(value); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override protected void onResume() {
        super.onResume();
        if (binding != null) {
            renderAccounts();
            renderCards();
            renderReconciliationSummary(lastReconciliationSummary);
        }
        if (firstResume) firstResume = false;
        else if (viewModel != null) viewModel.load(includeInactive);
    }
}
