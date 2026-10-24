package br.com.tecpontes.appfinanceiro.ui.accounts;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
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
    private AlertDialog historyDialog;

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
    }

    private View createAccountCard(AccountDto account) {
        MaterialCardView card = createCard();
        LinearLayout content = verticalLayout();
        content.setPadding(dp(14), dp(14), dp(14), dp(12));
        card.addView(content);

        LinearLayout titleRow = horizontalLayout();
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_nav_accounts);
        icon.setColorFilter(getColor(R.color.brand_primary));
        titleRow.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout titleColumn = verticalLayout();
        titleColumn.setPadding(dp(10), 0, 0, 0);
        TextView name = titleText(safe(account.getName()), 17);
        titleColumn.addView(name);
        TextView type = secondaryText(accountTypeLabel(account.getType()));
        titleColumn.addView(type);
        titleRow.addView(titleColumn, new LinearLayout.LayoutParams(0, -2, 1));
        if (!account.isActive()) titleRow.addView(statusBadge("Inativa", R.color.money_negative));
        content.addView(titleRow);

        LinearLayout balances = horizontalLayout();
        LinearLayout currentColumn = verticalLayout();
        TextView currentLabel = secondaryText("Saldo atual");
        currentColumn.addView(currentLabel);
        TextView currentValue = titleText(formatCurrency(account.getBalance()), 20);
        currentValue.setTextColor(moneyColor(account.getBalance()));
        currentColumn.addView(currentValue);
        balances.addView(currentColumn, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout effectiveColumn = verticalLayout();
        TextView effectiveLabel = secondaryText("Saldo final");
        effectiveColumn.addView(effectiveLabel);
        double effective = account.getEffectiveBalance() == null
                ? account.getBalance() : parseAmount(account.getEffectiveBalance());
        TextView effectiveValue = titleText(formatCurrency(effective), 20);
        effectiveValue.setTextColor(moneyColor(effective));
        effectiveColumn.addView(effectiveValue);
        balances.addView(effectiveColumn, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(balances, marginTop(dp(12)));

        double pending = parseAmount(account.getPendingInvoices());
        if (pending > 0) {
            TextView pendingLabel = secondaryText("Faturas pendentes afetam o saldo: −" + formatCurrency(pending));
            pendingLabel.setTextColor(getColor(R.color.money_negative));
            content.addView(pendingLabel, marginTop(dp(6)));
        }

        String bankDetails = account.getBranch() == null && account.getAccountNumber() == null ? ""
                : "Agência " + safe(account.getBranch()) + " · Conta " + safe(account.getAccountNumber());
        if (!bankDetails.trim().isEmpty()) content.addView(secondaryText(bankDetails), marginTop(dp(4)));

        LinearLayout actions = horizontalLayout();
        TextView history = actionText("Histórico");
        history.setOnClickListener(v -> showBalanceHistory(account));
        actions.addView(history, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView edit = actionText("Editar");
        edit.setEnabled(account.isActive());
        edit.setAlpha(account.isActive() ? 1f : .45f);
        edit.setOnClickListener(v -> showAccountForm(account));
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView importButton = actionText("Importar");
        importButton.setEnabled(account.isActive() && account.getAccountNumber() != null
                && !account.getAccountNumber().trim().isEmpty());
        importButton.setAlpha(importButton.isEnabled() ? 1f : .45f);
        importButton.setOnClickListener(v -> showImportOptions(importButton, account));
        actions.addView(importButton, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView more = actionText("•••");
        more.setOnClickListener(v -> showAccountMenu(more, account));
        actions.addView(more, new LinearLayout.LayoutParams(dp(48), dp(42)));
        content.addView(actions, marginTop(dp(10)));
        return card;
    }

    private void renderCards() {
        LinearLayout container = binding.cardsContainer;
        container.removeAllViews();
        binding.cardsEmptyTextView.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);
        for (CreditCardDto creditCard : cards) container.addView(createCreditCard(creditCard));
    }

    private View createCreditCard(CreditCardDto creditCard) {
        MaterialCardView card = createCard();
        LinearLayout content = verticalLayout();
        content.setPadding(dp(14), dp(14), dp(14), dp(12));
        card.addView(content);
        LinearLayout titleRow = horizontalLayout();
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_nav_cards);
        icon.setColorFilter(getColor(R.color.brand_primary));
        titleRow.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout titleColumn = verticalLayout();
        titleColumn.setPadding(dp(10), 0, 0, 0);
        titleColumn.addView(titleText(safe(creditCard.getName()), 17));
        titleColumn.addView(secondaryText(safe(creditCard.getBandeira())));
        titleRow.addView(titleColumn, new LinearLayout.LayoutParams(0, -2, 1));
        if (!creditCard.isActive()) titleRow.addView(statusBadge("Inativo", R.color.money_negative));
        content.addView(titleRow);

        LinearLayout limitRow = horizontalLayout();
        LinearLayout limitColumn = verticalLayout();
        limitColumn.addView(secondaryText("Limite de crédito"));
        limitColumn.addView(titleText(formatCurrency(parseAmount(creditCard.getLimit())), 17));
        limitRow.addView(limitColumn, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout availableColumn = verticalLayout();
        availableColumn.addView(secondaryText("Disponível"));
        TextView available = titleText(formatCurrency(parseAmount(creditCard.getAvailableLimit())), 17);
        available.setTextColor(getColor(R.color.money_positive));
        availableColumn.addView(available);
        limitRow.addView(availableColumn, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(limitRow, marginTop(dp(12)));

        String debitName = creditCard.getDebitAccount() == null ? null : creditCard.getDebitAccount().getName();
        if (debitName == null) debitName = accountName(creditCard.getDebitAccountId());
        String days = "Fecha dia " + (creditCard.getClosingDay() == null ? "—" : creditCard.getClosingDay())
                + " · vence dia " + (creditCard.getDueDay() == null ? "—" : creditCard.getDueDay());
        content.addView(secondaryText(days + (debitName == null ? "" : "\nConta de débito: " + debitName)),
                marginTop(dp(6)));

        LinearLayout actions = horizontalLayout();
        TextView edit = actionText("Editar");
        edit.setEnabled(creditCard.isActive());
        edit.setAlpha(creditCard.isActive() ? 1f : .45f);
        edit.setOnClickListener(v -> showCardForm(creditCard));
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView more = actionText("•••");
        more.setOnClickListener(v -> showCardMenu(more, creditCard));
        actions.addView(more, new LinearLayout.LayoutParams(dp(48), dp(42)));
        content.addView(actions, marginTop(dp(8)));
        return card;
    }

    private void showAccountForm(@Nullable AccountDto account) {
        LinearLayout form = dialogForm();
        EditText name = input("Nome da conta", account == null ? "" : safe(account.getName()), InputType.TYPE_CLASS_TEXT);
        form.addView(fieldLabel("Nome da conta *"));
        form.addView(name);
        EditText balance = input("Saldo atual", account == null ? "0,00"
                : String.format(Locale.US, "%.2f", account.getBalance()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        form.addView(fieldLabel("Saldo atual *"));
        form.addView(balance);
        String[] labels = {"Conta Corrente", "Poupança", "Investimento", "Dinheiro"};
        String[] types = {"CHECKING", "SAVINGS", "INVESTMENT", "CASH"};
        Spinner type = createSpinner(labels);
        if (account != null) type.setSelection(indexOf(types, account.getType()));
        form.addView(fieldLabel("Tipo de conta *"));
        form.addView(type);
        EditText branch = input("Agência", account == null ? "" : safe(account.getBranch()), InputType.TYPE_CLASS_TEXT);
        form.addView(fieldLabel("Agência"));
        form.addView(branch);
        EditText number = input("Número da conta", account == null ? "" : safe(account.getAccountNumber()), InputType.TYPE_CLASS_TEXT);
        form.addView(fieldLabel("Conta"));
        form.addView(number);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(account == null ? "Nova conta bancária" : "Editar conta")
                .setView(form).setNegativeButton("Cancelar", null)
                .setPositiveButton(account == null ? "Criar conta" : "Atualizar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) {
                name.setError("Informe o nome da conta");
                return;
            }
            double amount;
            try { amount = parseLocalizedAmount(balance.getText().toString()); }
            catch (NumberFormatException error) { balance.setError("Informe um saldo válido"); return; }
            AccountRequestDto request = new AccountRequestDto();
            request.name = name.getText().toString().trim();
            request.balance = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
            request.type = types[type.getSelectedItemPosition()];
            request.branch = branch.getText().toString().trim();
            request.accountNumber = number.getText().toString().trim();
            dialog.dismiss();
            viewModel.saveAccount(account == null ? null : account.getId(), request,
                    actionCallback(account == null ? "Conta criada." : "Conta atualizada."));
        }));
        dialog.show();
    }

    private void showCardForm(@Nullable CreditCardDto card) {
        LinearLayout form = dialogForm();
        EditText name = input("Nome do cartão", card == null ? "" : safe(card.getName()), InputType.TYPE_CLASS_TEXT);
        form.addView(fieldLabel("Nome *"));
        form.addView(name);
        EditText limit = input("Limite", card == null ? "0,00" : safe(card.getLimit()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(fieldLabel("Limite *"));
        form.addView(limit);
        EditText brand = input("Bandeira", card == null ? "" : safe(card.getBandeira()), InputType.TYPE_CLASS_TEXT);
        form.addView(fieldLabel("Bandeira"));
        form.addView(brand);
        EditText closingDay = input("Dia de fechamento", card == null ? "1" : safe(card.getClosingDay()), InputType.TYPE_CLASS_NUMBER);
        form.addView(fieldLabel("Dia de fechamento *"));
        form.addView(closingDay);
        EditText dueDay = input("Dia de vencimento", card == null ? "10" : safe(card.getDueDay()), InputType.TYPE_CLASS_NUMBER);
        form.addView(fieldLabel("Dia de vencimento *"));
        form.addView(dueDay);

        List<String> accountOptions = new ArrayList<>();
        accountOptions.add("Nenhuma conta");
        for (AccountDto accountDto : accounts) accountOptions.add(safe(accountDto.getName()));
        Spinner debitAccount = createSpinner(accountOptions.toArray(new String[0]));
        int selectedAccount = 0;
        if (card != null && card.getDebitAccountId() != null) {
            for (int i = 0; i < accounts.size(); i++) {
                if (card.getDebitAccountId().equals(accounts.get(i).getId())) selectedAccount = i + 1;
            }
        }
        debitAccount.setSelection(selectedAccount);
        form.addView(fieldLabel("Conta de débito"));
        form.addView(debitAccount);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(card == null ? "Novo cartão de crédito" : "Editar cartão")
                .setView(form).setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String cardName = name.getText().toString().trim();
            if (cardName.length() < 2) { name.setError("Informe ao menos 2 caracteres"); return; }
            double limitAmount;
            int closeDay;
            int paymentDay;
            try {
                limitAmount = parseLocalizedAmount(limit.getText().toString());
                closeDay = Integer.parseInt(closingDay.getText().toString().trim());
                paymentDay = Integer.parseInt(dueDay.getText().toString().trim());
            } catch (RuntimeException error) { Toast.makeText(this, "Confira limite e dias da fatura.", Toast.LENGTH_LONG).show(); return; }
            if (limitAmount < 0 || closeDay < 1 || closeDay > 31 || paymentDay < 1 || paymentDay > 31) {
                Toast.makeText(this, "O limite deve ser positivo e os dias devem ficar entre 1 e 31.", Toast.LENGTH_LONG).show();
                return;
            }
            CreditCardRequestDto request = new CreditCardRequestDto();
            request.name = cardName;
            request.limit = BigDecimal.valueOf(limitAmount).setScale(2, RoundingMode.HALF_UP).toPlainString();
            request.bandeira = brand.getText().toString().trim();
            request.closingDay = closeDay;
            request.dueDay = paymentDay;
            int accountIndex = debitAccount.getSelectedItemPosition() - 1;
            request.debitAccountId = accountIndex >= 0 && accountIndex < accounts.size()
                    ? accounts.get(accountIndex).getId() : null;
            dialog.dismiss();
            viewModel.saveCard(card == null ? null : card.getId(), request,
                    actionCallback(card == null ? "Cartão criado." : "Cartão atualizado."));
        }));
        dialog.show();
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
                    binding.recalculateButton.setEnabled(false);
                    viewModel.recalculate(new AccountsRepository.ActionCallback() {
                        @Override public void onSuccess() {
                            binding.recalculateButton.setEnabled(true);
                            Toast.makeText(AccountsActivity.this, "Saldos e faturas recalculados.", Toast.LENGTH_LONG).show();
                            viewModel.load(includeInactive);
                        }
                        @Override public void onError(@NonNull String message) {
                            binding.recalculateButton.setEnabled(true);
                            Toast.makeText(AccountsActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }).show();
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
        historyRows = verticalLayout();
        historyState = secondaryText("Carregando histórico…");
        ScrollView scroll = new ScrollView(this);
        scroll.addView(historyRows);
        LinearLayout content = verticalLayout();
        content.addView(historyState);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, dp(420)));
        historyDialog = new AlertDialog.Builder(this).setTitle("Histórico de saldo · " + safe(account.getName()))
                .setView(content).setPositiveButton("Fechar", (dialog, which) -> clearHistoryDialog()).create();
        historyDialog.setOnDismissListener(dialog -> clearHistoryDialog());
        historyDialog.show();
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
            row.addView(date, new LinearLayout.LayoutParams(0, -2, 1));
            TextView balance = bodyText(formatCurrency(parseAmount(item.balance)));
            balance.setTypeface(null, Typeface.BOLD);
            balance.setTextColor(moneyColor(parseAmount(item.balance)));
            row.addView(balance, new LinearLayout.LayoutParams(0, -2, 1));
            TextView change = bodyText(formatCurrency(parseAmount(item.change)));
            change.setTextColor(moneyColor(parseAmount(item.change)));
            row.addView(change, new LinearLayout.LayoutParams(0, -2, 1));
            row.setPadding(0, dp(10), 0, dp(10));
            historyRows.addView(row);
            View divider = new View(this);
            divider.setBackgroundColor(getColor(R.color.outline));
            historyRows.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
    }

    private void clearHistoryDialog() {
        selectedHistoryAccount = null;
        historyRows = null;
        historyState = null;
        historyDialog = null;
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
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(R.color.outline));
        return card;
    }

    private LinearLayout verticalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
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

    private TextView actionText(String value) {
        TextView text = bodyText(value);
        text.setTextColor(getColor(R.color.brand_primary));
        text.setGravity(android.view.Gravity.CENTER);
        text.setBackgroundResource(android.R.drawable.list_selector_background);
        text.setClickable(true);
        text.setFocusable(true);
        return text;
    }

    private TextView statusBadge(String value, int colorResource) {
        TextView text = bodyText(value);
        text.setTextColor(getColor(colorResource));
        text.setTypeface(null, Typeface.BOLD);
        text.setPadding(dp(8), dp(4), dp(8), dp(4));
        text.setBackgroundColor(getColor(R.color.error_container));
        return text;
    }

    private LinearLayout.LayoutParams marginTop(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = margin;
        return params;
    }

    private LinearLayout dialogForm() {
        LinearLayout form = verticalLayout();
        form.setPadding(dp(24), dp(8), dp(24), 0);
        return form;
    }

    private TextView fieldLabel(String label) {
        TextView text = secondaryText(label);
        text.setPadding(0, dp(9), 0, dp(3));
        return text;
    }

    private EditText input(String hint, String value, int inputType) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setText(value);
        return input;
    }

    private Spinner createSpinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
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
        return getColor(value >= 0 ? R.color.money_positive : R.color.money_negative);
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
