package br.com.tecpontes.appfinanceiro.ui.cards;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.data.repository.CreditCardRepository;
import br.com.tecpontes.appfinanceiro.databinding.ActivityCardsBinding;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.model.InvoiceDetailsDto;
import br.com.tecpontes.appfinanceiro.model.InvoicePaymentRequestDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.model.TransactionTransferRequestDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.ui.transactions.CreateTransactionDialog;
import br.com.tecpontes.appfinanceiro.ui.transactions.TransactionsAdapter;
import br.com.tecpontes.appfinanceiro.utils.PrivacyMode;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.CreditCardsViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;

/** Tela mobile de cartões, faturas e lançamentos associados. */
@AndroidEntryPoint
public class CardsActivity extends AppCompatActivity {

    private ActivityCardsBinding binding;
    private CreditCardsViewModel viewModel;
    private TransactionsAdapter creditsAdapter;
    private TransactionsAdapter expensesAdapter;
    private List<CreditCardDto> cards = new ArrayList<>();
    private List<AccountDto> accounts = new ArrayList<>();
    private List<CategoryDto> categories = new ArrayList<>();
    private String selectedCardId;
    private Calendar selectedMonth = Calendar.getInstance();
    private boolean suppressCardSelection;
    @Nullable private CreateTransactionDialog createTransactionDialog;

    @Inject TokenManager tokenManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCardsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BottomNavigationHelper.setup(this, R.id.navigation_cards);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CreditCardsViewModel.class);
        setupLists();
        setupObservers();
        setupListeners();
        SessionExpiredNavigation.observe(this, tokenManager);

        viewModel.loadPaymentOptions();
        viewModel.loadCards();
    }

    private void setupLists() {
        TransactionsAdapter.OnTransactionClickListener detailsListener = this::showTransactionDetails;
        TransactionsAdapter.OnTransactionOptionsListener optionsListener = this::showTransactionOptions;
        creditsAdapter = new TransactionsAdapter(detailsListener, optionsListener);
        expensesAdapter = new TransactionsAdapter(detailsListener, optionsListener);
        binding.invoiceCreditsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.invoiceCreditsRecyclerView.setAdapter(creditsAdapter);
        binding.invoiceExpensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.invoiceExpensesRecyclerView.setAdapter(expensesAdapter);
    }

    private void setupObservers() {
        viewModel.cardsData.observe(this, loadedCards -> {
            cards = loadedCards == null ? new ArrayList<>() : new ArrayList<>(loadedCards);
            cards.sort(Comparator.comparing(card -> safe(card.getName()), String.CASE_INSENSITIVE_ORDER));
            renderCardSelection();
            updateCreateTransactionDialogOptions();
        });
        viewModel.invoiceData.observe(this, this::renderInvoice);
        viewModel.accountsData.observe(this, loadedAccounts -> {
            accounts = loadedAccounts == null ? new ArrayList<>() : loadedAccounts;
            renderCardOverview(findSelectedCard());
            updateCreateTransactionDialogOptions();
        });
        viewModel.categoriesData.observe(this, loadedCategories -> {
            categories = loadedCategories == null ? new ArrayList<>() : loadedCategories;
            updateCreateTransactionDialogOptions();
        });
        viewModel.isRefreshing.observe(this, loading ->
                binding.swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(loading)));
        viewModel.errorMessage.observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                binding.errorTextView.setVisibility(View.GONE);
            } else {
                binding.errorTextView.setText(message);
                binding.errorTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupListeners() {
        binding.swipeRefreshLayout.setColorSchemeResources(
                R.color.brand_primary, R.color.money_positive, R.color.money_negative);
        binding.swipeRefreshLayout.setOnRefreshListener(this::reloadCurrent);
        binding.cardSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (suppressCardSelection || position < 0 || position >= cards.size()) return;
                CreditCardDto card = cards.get(position);
                if (card.getId() == null || card.getId().equals(selectedCardId)) return;
                selectedCardId = card.getId();
                renderCardOverview(card);
                loadInvoice();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        binding.previousMonthButton.setOnClickListener(v -> moveMonth(-1));
        binding.nextMonthButton.setOnClickListener(v -> moveMonth(1));
        binding.invoicePeriodTextView.setOnClickListener(v -> showMonthPicker());
        binding.invoiceStatusButton.setOnClickListener(v -> changeInvoiceStatus());
        binding.payInvoiceButton.setOnClickListener(v -> showPaymentDialog());
        binding.reversePaymentButton.setOnClickListener(v -> reverseLastPayment());
        binding.addTransactionButton.setOnClickListener(v -> showCreateTransactionDialog());
        binding.exportInvoiceButton.setOnClickListener(v -> exportInvoice());
    }

    private void renderCardSelection() {
        ArrayAdapter<CreditCardDto> adapter = new ArrayAdapter<>(
                this, R.layout.item_spinner_form, cards);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        suppressCardSelection = true;
        binding.cardSpinner.setAdapter(adapter);

        if (cards.isEmpty()) {
            selectedCardId = null;
            binding.noCardsTextView.setVisibility(View.VISIBLE);
            binding.cardOverview.setVisibility(View.GONE);
            binding.invoiceCard.setVisibility(View.GONE);
            binding.transactionsCard.setVisibility(View.GONE);
            binding.categoryCard.setVisibility(View.GONE);
            binding.cardSpinner.setVisibility(View.GONE);
            creditsAdapter.updateTransactions(null);
            expensesAdapter.updateTransactions(null);
            return;
        }

        binding.noCardsTextView.setVisibility(View.GONE);
        binding.cardSpinner.setVisibility(View.VISIBLE);
        binding.cardOverview.setVisibility(View.VISIBLE);
        binding.invoiceCard.setVisibility(View.VISIBLE);
        binding.transactionsCard.setVisibility(View.VISIBLE);
        binding.categoryCard.setVisibility(View.VISIBLE);

        int selectedPosition = findCardPosition(selectedCardId);
        if (selectedPosition < 0) {
            selectedPosition = 0;
            CreditCardDto first = cards.get(0);
            CreditCardDto.CardInvoiceSummary current = first.getCurrentInvoice();
            if (current != null && current.referenceMonth != null && current.referenceYear != null) {
                selectedMonth.set(Calendar.MONTH, current.referenceMonth - 1);
                selectedMonth.set(Calendar.YEAR, current.referenceYear);
            } else {
                selectedMonth = Calendar.getInstance();
            }
            selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
        }
        binding.cardSpinner.setSelection(selectedPosition, false);
        selectedCardId = cards.get(selectedPosition).getId();
        suppressCardSelection = false;
        renderCardOverview(findSelectedCard());
        updatePeriodLabel();
        loadInvoice();
    }

    private int findCardPosition(@Nullable String cardId) {
        if (cardId == null) return -1;
        for (int i = 0; i < cards.size(); i++) {
            if (cardId.equals(cards.get(i).getId())) return i;
        }
        return -1;
    }

    @Nullable private CreditCardDto findSelectedCard() {
        for (CreditCardDto card : cards) {
            if (selectedCardId != null && selectedCardId.equals(card.getId())) return card;
        }
        return cards.isEmpty() ? null : cards.get(0);
    }

    private void renderCardOverview(@Nullable CreditCardDto card) {
        if (card == null) return;
        binding.cardNameTextView.setText(safe(card.getName()));
        String brand = safe(card.getBandeira());
        String debitAccount = card.getDebitAccount() == null ? null : card.getDebitAccount().getName();
        if (debitAccount == null && card.getDebitAccountId() != null) {
            for (AccountDto account : accounts) {
                if (card.getDebitAccountId().equals(account.getId())) {
                    debitAccount = account.getName();
                    break;
                }
            }
        }
        StringBuilder meta = new StringBuilder();
        if (!brand.isEmpty()) meta.append(brand);
        if (debitAccount != null && !debitAccount.trim().isEmpty()) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append("Conta de débito: ").append(debitAccount);
        } else {
            if (meta.length() > 0) meta.append(" · ");
            meta.append("Conta de débito não definida");
        }
        if (card.getClosingDay() != null || card.getDueDay() != null) {
            meta.append("\nFecha dia ").append(card.getClosingDay() == null ? "—" : card.getClosingDay())
                    .append(" · vence dia ").append(card.getDueDay() == null ? "—" : card.getDueDay());
        }
        binding.cardMetaTextView.setText(meta.toString());

        double limit = parseAmount(card.getLimit());
        double used = parseAmount(card.getUsedAmount());
        double available = card.getAvailableLimit() == null ? limit - used : parseAmount(card.getAvailableLimit());
        binding.usedLimitTextView.setText("Utilizado " + formatSummaryCurrency(used) + " / " + formatSummaryCurrency(limit));
        binding.availableLimitTextView.setText("Disponível " + formatSummaryCurrency(available));
        binding.availableLimitTextView.setTextColor(moneyColor(available));
        int percent = limit <= 0 ? 0 : (int) Math.max(0, Math.min(100, Math.round(used * 100 / limit)));
        binding.limitProgressBar.setProgress(percent);
    }

    private void moveMonth(int amount) {
        selectedMonth.add(Calendar.MONTH, amount);
        selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
        updatePeriodLabel();
        loadInvoice();
    }

    private void updatePeriodLabel() {
        String label = new SimpleDateFormat("MMMM 'de' yyyy", new Locale("pt", "BR"))
                .format(selectedMonth.getTime());
        if (!label.isEmpty()) label = label.substring(0, 1).toUpperCase(new Locale("pt", "BR")) + label.substring(1);
        binding.invoicePeriodTextView.setText(label);
    }

    private void showMonthPicker() {
        String[] monthNames = DateFormatSymbols.getInstance(new Locale("pt", "BR")).getMonths();
        String[] monthOptions = new String[12];
        for (int i = 0; i < monthOptions.length; i++) {
            String name = monthNames[i];
            monthOptions[i] = name.substring(0, 1).toUpperCase(new Locale("pt", "BR")) + name.substring(1);
        }
        int firstYear = Math.min(2020, selectedMonth.get(Calendar.YEAR));
        int lastYear = Math.max(Calendar.getInstance().get(Calendar.YEAR) + 5, selectedMonth.get(Calendar.YEAR));
        String[] years = new String[lastYear - firstYear + 1];
        for (int i = 0; i < years.length; i++) years[i] = String.valueOf(firstYear + i);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(8), dp(24), 0);
        TextView monthLabel = new TextView(this);
        monthLabel.setText("Mês");
        monthLabel.setTextColor(getColor(R.color.on_surface_variant));
        monthLabel.setTextSize(12);
        content.addView(monthLabel);
        Spinner monthSpinner = createSpinner(monthOptions);
        monthSpinner.setSelection(selectedMonth.get(Calendar.MONTH));
        content.addView(monthSpinner);
        TextView yearLabel = new TextView(this);
        yearLabel.setText("Ano");
        yearLabel.setTextColor(getColor(R.color.on_surface_variant));
        yearLabel.setTextSize(12);
        yearLabel.setPadding(0, dp(12), 0, 0);
        content.addView(yearLabel);
        Spinner yearSpinner = createSpinner(years);
        yearSpinner.setSelection(selectedMonth.get(Calendar.YEAR) - firstYear);
        content.addView(yearSpinner);

        new AlertDialog.Builder(this).setTitle("Selecionar período").setView(content)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aplicar", (dialog, which) -> {
                    selectedMonth.set(Calendar.YEAR, Integer.parseInt((String) yearSpinner.getSelectedItem()));
                    selectedMonth.set(Calendar.MONTH, monthSpinner.getSelectedItemPosition());
                    selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
                    updatePeriodLabel();
                    loadInvoice();
                }).show();
    }

    private Spinner createSpinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner_form, values);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        spinner.setAdapter(adapter);
        spinner.setBackgroundResource(R.drawable.bg_spinner_selector);
        spinner.setPadding(dp(16), 0, dp(44), 0);
        spinner.setMinimumHeight(dp(52));
        return spinner;
    }

    private void loadInvoice() {
        updatePeriodLabel();
        if (selectedCardId != null) {
            viewModel.loadInvoice(selectedCardId, selectedMonth.get(Calendar.YEAR),
                    selectedMonth.get(Calendar.MONTH) + 1);
        }
    }

    private void reloadCurrent() {
        viewModel.loadPaymentOptions();
        if (selectedCardId == null) viewModel.loadCards();
        else {
            loadInvoice();
            viewModel.loadCards();
        }
    }

    private void renderInvoice(@Nullable InvoiceDetailsDto invoice) {
        LinearLayout summary = binding.invoiceSummaryContainer;
        summary.removeAllViews();
        if (invoice == null || invoice.id == null) {
            binding.invoiceUnavailableTextView.setVisibility(View.VISIBLE);
            summary.setVisibility(View.GONE);
            binding.invoiceSummaryTitle.setVisibility(View.GONE);
            binding.invoiceStatusButton.setVisibility(View.GONE);
            binding.payInvoiceButton.setVisibility(View.GONE);
            binding.reversePaymentButton.setVisibility(View.GONE);
            // Sem conexão a fatura pode não carregar; o lançamento ainda pode
            // ser salvo localmente e sincronizado depois.
            binding.addTransactionButton.setEnabled(findSelectedCard() != null);
            binding.exportInvoiceButton.setEnabled(false);
            binding.transactionsEmptyTextView.setVisibility(View.VISIBLE);
            binding.invoiceCreditsTitle.setVisibility(View.GONE);
            binding.invoiceExpensesTitle.setVisibility(View.GONE);
            creditsAdapter.updateTransactions(null);
            expensesAdapter.updateTransactions(null);
            renderCategoryBreakdown(Collections.emptyList());
            return;
        }

        binding.invoiceUnavailableTextView.setVisibility(View.GONE);
        summary.setVisibility(View.VISIBLE);
        binding.invoiceSummaryTitle.setVisibility(View.VISIBLE);
        binding.invoiceStatusButton.setVisibility(View.VISIBLE);
        binding.payInvoiceButton.setVisibility(View.VISIBLE);
        binding.addTransactionButton.setEnabled(!"CLOSED".equalsIgnoreCase(invoice.status));
        binding.exportInvoiceButton.setEnabled(!invoice.getTransactions().isEmpty());
        addInfoRow(summary, "Fechamento", formatDate(invoice.closingDate), getColor(R.color.on_surface_variant), false);
        addInfoRow(summary, "Vencimento", formatDate(invoice.dueDate), getColor(R.color.on_surface_variant), false);
        CreditCardDto selectedCard = findSelectedCard();
        String debitAccountName = selectedCard == null || selectedCard.getDebitAccount() == null
                ? "Não definida" : safe(selectedCard.getDebitAccount().getName());
        addInfoRow(summary, "Conta de débito", debitAccountName, getColor(R.color.on_surface_variant), false);
        addInfoRow(summary, "Saldo anterior", formatSummaryCurrency(invoice.amount(invoice.previousBalance)),
                getColor(R.color.money_negative), false);
        addInfoRow(summary, "Total pago", formatSummaryCurrency(invoice.amount(invoice.totalPaid)),
                getColor(R.color.money_positive), false);
        addInfoRow(summary, "Total atual", formatSummaryCurrency(invoice.amount(invoice.totalAmount)),
                getColor(R.color.money_negative), true);
        addInfoRow(summary, "Valor a pagar", formatSummaryCurrency(invoice.amount(invoice.remainingBalance)),
                getColor(R.color.money_negative), true);
        if (Boolean.TRUE.equals(invoice.hasPartialPayment)) {
            addInfoRow(summary, "Pagamento", "Parcial", getColor(R.color.brand_primary), false);
        }
        addInfoRow(summary, "Detalhamento da fatura", "", getColor(R.color.on_surface_variant), true);
        addInfoRow(summary, "Despesas conciliadas", formatSummaryCurrency(invoice.amount(invoice.totalConciliado)),
                getColor(R.color.money_positive), false);
        addInfoRow(summary, "Despesas não conciliadas", formatSummaryCurrency(invoice.amount(invoice.totalNaoConciliado)),
                getColor(R.color.money_negative), false);
        addInfoRow(summary, "Despesas fixas no mês", formatSummaryCurrency(invoice.amount(invoice.fixedExpenses)),
                getColor(R.color.money_negative), false);
        addInfoRow(summary, "Parcelas futuras", formatSummaryCurrency(invoice.amount(invoice.futureInstallments)),
                getColor(R.color.money_negative), false);

        boolean fullyPaid = invoice.amount(invoice.remainingBalance) <= 0;
        boolean closed = "CLOSED".equalsIgnoreCase(invoice.status);
        binding.invoiceStatusButton.setText(closed ? "Reabrir fatura" : "Fechar fatura");
        binding.invoiceStatusButton.setEnabled(!fullyPaid);
        binding.payInvoiceButton.setText(fullyPaid ? "Fatura paga" : "Lançar pagamento");
        binding.payInvoiceButton.setEnabled(!fullyPaid && invoice.amount(invoice.remainingBalance) > 0);
        boolean hasReversiblePayment = findLastPaymentTransaction(invoice) != null;
        binding.reversePaymentButton.setVisibility(fullyPaid && hasReversiblePayment ? View.VISIBLE : View.GONE);

        List<TransactionListItemDto> credits = new ArrayList<>();
        List<TransactionListItemDto> expenses = new ArrayList<>();
        for (TransactionListItemDto transaction : invoice.getTransactions()) {
            if (transaction != null && transaction.isIncome()) credits.add(transaction);
            else if (transaction != null) expenses.add(transaction);
        }
        sortTransactions(credits);
        sortTransactions(expenses);
        binding.invoiceCreditsTitle.setVisibility(credits.isEmpty() ? View.GONE : View.VISIBLE);
        binding.invoiceExpensesTitle.setVisibility(expenses.isEmpty() ? View.GONE : View.VISIBLE);
        binding.transactionsEmptyTextView.setVisibility(credits.isEmpty() && expenses.isEmpty() ? View.VISIBLE : View.GONE);
        creditsAdapter.updateTransactions(credits);
        expensesAdapter.updateTransactions(expenses);
        renderCategoryBreakdown(invoice.getTransactions());
    }

    private void sortTransactions(List<TransactionListItemDto> values) {
        values.sort(Comparator.comparingLong(TransactionListItemDto::getDateMillis));
    }

    private void addInfoRow(LinearLayout parent, String label, String value, int color, boolean emphasized) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(emphasized ? 10 : 7), 0, dp(emphasized ? 10 : 7));
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(getColor(R.color.on_surface_variant));
        labelView.setTextSize(emphasized ? 14 : 13);
        if (emphasized) labelView.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(labelView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(color);
        valueView.setTextSize(emphasized ? 15 : 13);
        if (emphasized) valueView.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(valueView);
        parent.addView(row);
    }

    private void renderCategoryBreakdown(List<TransactionListItemDto> transactions) {
        LinearLayout container = binding.categoryBreakdownContainer;
        container.removeAllViews();
        Map<String, CategoryTotal> totals = new LinkedHashMap<>();
        double grandTotal = 0;
        for (TransactionListItemDto transaction : transactions) {
            if (transaction == null || transaction.isIncome()) continue;
            String name = transaction.getCategoryName();
            String key = transaction.categoryId == null ? name : transaction.categoryId;
            CategoryTotal total = totals.get(key);
            if (total == null) {
                total = new CategoryTotal(name,
                        transaction.category == null ? null : transaction.category.color);
                totals.put(key, total);
            }
            double amount = Math.abs(transaction.getAmountValue());
            total.amount += amount;
            grandTotal += amount;
        }
        binding.categoryCard.setVisibility(totals.isEmpty() ? View.GONE : View.VISIBLE);
        for (CategoryTotal total : totals.values()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            LinearLayout heading = new LinearLayout(this);
            heading.setOrientation(LinearLayout.HORIZONTAL);
            heading.setGravity(android.view.Gravity.CENTER_VERTICAL);

            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(parseColor(total.color));
            View dotView = new View(this);
            dotView.setBackground(dot);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
            dotParams.setMarginEnd(dp(8));
            heading.addView(dotView, dotParams);

            TextView categoryName = new TextView(this);
            categoryName.setText(total.name);
            categoryName.setTextColor(getColor(R.color.on_surface));
            categoryName.setTextSize(13);
            heading.addView(categoryName, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            int percent = grandTotal <= 0 ? 0 : (int) Math.round(total.amount * 100 / grandTotal);
            TextView percentView = new TextView(this);
            percentView.setText(percent + "%");
            percentView.setTextColor(getColor(R.color.on_surface_variant));
            percentView.setTextSize(12);
            percentView.setPadding(0, 0, dp(8), 0);
            heading.addView(percentView);

            TextView amount = new TextView(this);
            amount.setText(formatSummaryCurrency(total.amount));
            amount.setTextColor(getColor(R.color.money_negative));
            amount.setTextSize(13);
            amount.setTypeface(null, android.graphics.Typeface.BOLD);
            heading.addView(amount);
            row.addView(heading);

            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100);
            progress.setProgress(percent);
            progress.setProgressTintList(android.content.res.ColorStateList.valueOf(parseColor(total.color)));
            progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.outline)));
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(5));
            progressParams.topMargin = dp(6);
            row.addView(progress, progressParams);
            container.addView(row);
        }
    }

    private static class CategoryTotal {
        final String name;
        final String color;
        double amount;
        CategoryTotal(String name, String color) { this.name = name; this.color = color; }
    }

    private int parseColor(@Nullable String color) {
        try { return Color.parseColor(color); }
        catch (RuntimeException ignored) { return getColor(R.color.brand_primary); }
    }

    private void changeInvoiceStatus() {
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        if (invoice == null || invoice.id == null) return;
        if (invoice.amount(invoice.remainingBalance) <= 0) {
            Toast.makeText(this, "Estorne o pagamento antes de reabrir a fatura.", Toast.LENGTH_LONG).show();
            return;
        }
        boolean closed = "CLOSED".equalsIgnoreCase(invoice.status);
        new AlertDialog.Builder(this)
                .setTitle(closed ? "Reabrir fatura?" : "Fechar fatura?")
                .setMessage(closed ? "Os lançamentos voltarão a aceitar alterações."
                        : "A fatura ficará bloqueada para novos lançamentos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(closed ? "Reabrir" : "Fechar", (dialog, which) -> {
                    CreditCardRepository.ActionCallback callback = actionCallback(
                            closed ? "Fatura reaberta." : "Fatura fechada.");
                    if (closed) viewModel.reopenInvoice(invoice.id, callback);
                    else viewModel.closeInvoice(invoice.id, callback);
                }).show();
    }

    private void showPaymentDialog() {
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        if (invoice == null || invoice.id == null) return;
        if (accounts.isEmpty()) {
            Toast.makeText(this, "Cadastre uma conta para registrar o pagamento.", Toast.LENGTH_LONG).show();
            return;
        }
        LinearLayout form = dialogForm();
        EditText amountInput = new EditText(this);
        amountInput.setSingleLine(true);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setHint("Valor pago (R$)");
        amountInput.setText(String.format(Locale.US, "%.2f", invoice.amount(invoice.remainingBalance)));
        form.addView(amountInput);

        Spinner accountSpinner = createAccountSpinner();
        form.addView(fieldLabel("Conta de débito"));
        form.addView(accountSpinner);
        CreditCardDto card = findSelectedCard();
        if (card != null && card.getDebitAccountId() != null) {
            for (int i = 0; i < accounts.size(); i++) {
                if (card.getDebitAccountId().equals(accounts.get(i).getId())) accountSpinner.setSelection(i);
            }
        }

        List<CategoryDto> expenseCategories = expenseCategories();
        Spinner categorySpinner = createObjectSpinner(expenseCategories);
        form.addView(fieldLabel("Categoria (opcional)"));
        form.addView(categorySpinner);
        TextView paymentDateLabel = fieldLabel("Data do pagamento");
        TextView paymentDateButton = new TextView(this);
        Calendar paymentDate = Calendar.getInstance();
        updateDateButton(paymentDateButton, paymentDate);
        paymentDateButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        paymentDateButton.setBackgroundResource(R.drawable.bg_period_selector);
        paymentDateButton.setOnClickListener(v -> showDatePicker(paymentDate, () -> updateDateButton(paymentDateButton, paymentDate)));
        form.addView(paymentDateLabel);
        form.addView(paymentDateButton);

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Registrar pagamento da fatura")
                .setView(form).setNegativeButton("Cancelar", null).setPositiveButton("Registrar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double amount;
            try { amount = parseLocalizedAmount(amountInput.getText().toString()); }
            catch (NumberFormatException error) {
                amountInput.setError("Informe um valor válido");
                return;
            }
            if (amount <= 0) {
                amountInput.setError("O valor precisa ser maior que zero");
                return;
            }
            AccountDto account = accounts.get(accountSpinner.getSelectedItemPosition());
            CategoryDto category = expenseCategories.isEmpty() ? null : (CategoryDto) categorySpinner.getSelectedItem();
            InvoicePaymentRequestDto request = new InvoicePaymentRequestDto();
            request.amount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
            request.accountId = account.getId();
            request.paymentDate = paymentDate.getTime().toInstant().toString();
            request.categoryId = category == null ? null : category.getId();
            dialog.dismiss();
            viewModel.payInvoice(invoice.id, request, actionCallback("Pagamento registrado."));
        }));
        dialog.show();
    }

    private void reverseLastPayment() {
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        TransactionListItemDto transaction = findLastPaymentTransaction(invoice);
        if (transaction == null || transaction.id == null) return;
        new AlertDialog.Builder(this).setTitle("Estornar pagamento")
                .setMessage("O pagamento será removido e os saldos da fatura e da conta serão atualizados.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Estornar", (dialog, which) -> viewModel.deleteTransaction(
                        transaction.id, null, actionCallback("Pagamento estornado."))).show();
    }

    @Nullable private TransactionListItemDto findLastPaymentTransaction(@Nullable InvoiceDetailsDto invoice) {
        if (invoice == null) return null;
        List<InvoiceDetailsDto.InvoicePaymentEntry> payments = invoice.getPayments();
        for (int i = payments.size() - 1; i >= 0; i--) {
            InvoiceDetailsDto.InvoicePaymentEntry payment = payments.get(i);
            if (payment != null && payment.transaction != null && payment.transaction.id != null) {
                TransactionListItemDto result = new TransactionListItemDto();
                result.id = payment.transaction.id;
                result.description = payment.transaction.description;
                result.accountId = payment.transaction.accountId;
                return result;
            }
        }
        return null;
    }

    private void showTransactionOptions(View anchor, TransactionListItemDto transaction) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Detalhar").setOnMenuItemClickListener(item -> {
            showTransactionDetails(transaction);
            return true;
        });
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        boolean closed = invoice != null && "CLOSED".equalsIgnoreCase(invoice.status);
        if (!closed) {
            menu.getMenu().add(transaction.isReconciled() ? "Desfazer conciliação" : "Conciliar")
                    .setOnMenuItemClickListener(item -> {
                        if (transaction.id != null) viewModel.setReconciled(transaction.id,
                                !transaction.isReconciled(), actionCallback("Conciliação atualizada."));
                        return true;
                    });
            menu.getMenu().add("Editar").setOnMenuItemClickListener(item -> {
                showEditTransactionDialog(transaction);
                return true;
            });
            menu.getMenu().add("Clonar").setOnMenuItemClickListener(item -> {
                showCloneTransactionDialog(transaction);
                return true;
            });
            if (transaction.isReconciled() && !transaction.isIncome()) {
                menu.getMenu().add("Estornar lançamento").setOnMenuItemClickListener(item -> {
                    reverseTransaction(transaction);
                    return true;
                });
            }
            menu.getMenu().add("Excluir").setOnMenuItemClickListener(item -> {
                deleteTransaction(transaction);
                return true;
            });
        }
        menu.show();
    }

    private void showTransactionDetails(TransactionListItemDto transaction) {
        String details = "Categoria: " + transaction.getCategoryName()
                + "\nData: " + formatDate(transaction.date)
                + "\nValor: " + formatCurrency(transaction.getAmountValue())
                + "\nStatus: " + (transaction.isReconciled() ? "Conciliado" : "Pendente de conciliação");
        if (transaction.note != null && !transaction.note.trim().isEmpty()) details += "\nObservação: " + transaction.note;
        if (transaction.isInstallmentTransaction() && transaction.installmentNumber != null) {
            details += "\nParcela: " + transaction.installmentNumber + "/" + transaction.totalInstallments;
        }
        new AlertDialog.Builder(this).setTitle(safe(transaction.description)).setMessage(details)
                .setPositiveButton("Fechar", null).show();
    }

    private void showCreateTransactionDialog() {
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        if (invoice != null && "CLOSED".equalsIgnoreCase(invoice.status)) {
            Toast.makeText(this, "Reabra a fatura antes de adicionar lançamentos.", Toast.LENGTH_LONG).show();
            return;
        }
        CreditCardDto card = findSelectedCard();
        if (card == null || card.getId() == null) return;

        createTransactionDialog = new CreateTransactionDialog(this,
                new CreateTransactionDialog.OnTransactionCreatedListener() {
                    @Override public void onCreateTransaction(TransactionWriteDto transaction) {
                        transaction.accountId = null;
                        transaction.cardId = card.getId();
                        if (createTransactionDialog != null) createTransactionDialog.setSaving(true);
                        viewModel.saveTransaction(null, transaction, new CreditCardRepository.ActionCallback() {
                            @Override public void onSuccess() {
                                runOnUiThread(() -> {
                                    closeCreateTransactionDialog();
                                    Toast.makeText(CardsActivity.this,
                                            "Lançamento salvo. Sincronização em andamento.",
                                            Toast.LENGTH_SHORT).show();
                                    reloadCurrent();
                                });
                            }
                            @Override public void onError(@NonNull String message) {
                                runOnUiThread(() -> {
                                    if (createTransactionDialog != null) {
                                        createTransactionDialog.showError(message);
                                    }
                                });
                            }
                        });
                    }

                    @Override public void onCreateTransfer(TransactionTransferRequestDto transfer) {
                        // Transferências não estão disponíveis no contexto do cartão.
                    }
                }).presetCreditCard(card.getId());
        createTransactionDialog.setOnDismissListener(dialog -> createTransactionDialog = null);
        createTransactionDialog.show();
        createTransactionDialog.setOptionsLoading(true);
        createTransactionDialog.setOptions(accounts, categories, cards);
        createTransactionDialog.setOptionsLoading(false);
        viewModel.loadPaymentOptions();
    }

    private void updateCreateTransactionDialogOptions() {
        if (createTransactionDialog == null) return;
        createTransactionDialog.setOptions(accounts, categories, cards);
        createTransactionDialog.setOptionsLoading(false);
    }

    private void closeCreateTransactionDialog() {
        if (createTransactionDialog == null) return;
        CreateTransactionDialog dialog = createTransactionDialog;
        createTransactionDialog = null;
        dialog.dismiss();
    }

    private void showEditTransactionDialog(@NonNull TransactionListItemDto transaction) {
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        if (invoice == null) return;
        if ("CLOSED".equalsIgnoreCase(invoice.status)) {
            Toast.makeText(this, "Reabra a fatura antes de alterar lançamentos.", Toast.LENGTH_LONG).show();
            return;
        }
        if (transaction.id == null || transaction.id.isEmpty()) {
            Toast.makeText(this, "ID do lançamento indisponível", Toast.LENGTH_LONG).show();
            return;
        }

        String cardId = transaction.cardId != null ? transaction.cardId : selectedCardId;
        createTransactionDialog = new CreateTransactionDialog(this, transaction,
                request -> {
                    if (isSeriesTransaction(transaction)) {
                        closeCreateTransactionDialog();
                        showTransactionUpdateScope(transaction.id, request);
                    } else {
                        saveEditedTransaction(transaction.id, request);
                    }
                }).presetCreditCard(cardId);
        showTransactionForm();
    }

    private void showCloneTransactionDialog(@NonNull TransactionListItemDto transaction) {
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        if (invoice == null) return;
        if ("CLOSED".equalsIgnoreCase(invoice.status)) {
            Toast.makeText(this, "Reabra a fatura antes de alterar lançamentos.", Toast.LENGTH_LONG).show();
            return;
        }

        String cardId = transaction.cardId != null ? transaction.cardId : selectedCardId;
        createTransactionDialog = new CreateTransactionDialog(this, transaction, true,
                new CreateTransactionDialog.OnTransactionCreatedListener() {
                    @Override
                    public void onCreateTransaction(@NonNull TransactionWriteDto request) {
                        CreateTransactionDialog dialog = createTransactionDialog;
                        if (dialog != null) dialog.setSaving(true);
                        viewModel.saveTransaction(null, request,
                                new CreditCardRepository.ActionCallback() {
                                    @Override public void onSuccess() {
                                        closeCreateTransactionDialog();
                                        Toast.makeText(CardsActivity.this,
                                                "Lançamento clonado. Sincronização em andamento.",
                                                Toast.LENGTH_SHORT).show();
                                        reloadCurrent();
                                    }

                                    @Override
                                    public void onError(@NonNull String message) {
                                        if (createTransactionDialog != null) {
                                            createTransactionDialog.showError(message);
                                        } else {
                                            Toast.makeText(CardsActivity.this, message,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onCreateTransfer(@NonNull TransactionTransferRequestDto transfer) {
                        // Transferências não estão disponíveis no contexto do cartão.
                    }
                }).presetCreditCard(cardId);
        showTransactionForm();
    }

    private void showTransactionForm() {
        if (createTransactionDialog == null) return;
        createTransactionDialog.setOnDismissListener(dialog -> createTransactionDialog = null);
        createTransactionDialog.show();
        createTransactionDialog.setOptionsLoading(true);
        createTransactionDialog.setOptions(accounts, categories, cards);
        createTransactionDialog.setOptionsLoading(false);
        if (cards.isEmpty()) viewModel.loadCards();
        viewModel.loadPaymentOptions();
    }

    private void saveEditedTransaction(@NonNull String id, @NonNull TransactionWriteDto request) {
        CreateTransactionDialog dialog = createTransactionDialog;
        if (dialog != null) dialog.setSaving(true);
        viewModel.saveTransaction(id, request, new CreditCardRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                closeCreateTransactionDialog();
                Toast.makeText(CardsActivity.this,
                        "Lançamento atualizado.", Toast.LENGTH_SHORT).show();
                reloadCurrent();
            }

            @Override
            public void onError(@NonNull String message) {
                if (createTransactionDialog != null) {
                    createTransactionDialog.showError(message);
                } else {
                    Toast.makeText(CardsActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private boolean isSeriesTransaction(@NonNull TransactionListItemDto transaction) {
        return transaction.isInstallmentTransaction()
                || transaction.isFixedExpenseTransaction()
                || transaction.installmentGroupId != null
                || transaction.fixedExpenseGroupId != null;
    }

    private TransactionWriteDto buildTransactionRequest(
            @Nullable TransactionListItemDto old,
            boolean clone,
            String description,
            double amount,
            CategoryDto category,
            Calendar date) {
        TransactionWriteDto request = new TransactionWriteDto();
        request.description = description;
        request.amount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
        request.date = date.getTime().toInstant().toString();
        request.type = old != null && old.type != null ? old.type : "EXPENSE";
        request.categoryId = category.getId();
        request.localCategoryName = category.getName();
        CreditCardDto card = findSelectedCard();
        request.cardId = card == null ? null : card.getId();
        if (card != null) {
            request.localCardName = card.getName();
            request.localCardDebitAccountId = card.getDebitAccountId();
        }
        request.note = old == null ? null : old.note;
        if (old != null) {
            request.isInstallment = old.isInstallment;
            request.totalInstallments = old.totalInstallments;
            request.totalValue = old.totalValue;
            request.installmentValueType = old.installmentValueType;
            request.isFixedExpense = old.isFixedExpense;
            if (!clone) {
                request.installmentGroupId = old.installmentGroupId;
                request.fixedExpenseGroupId = old.fixedExpenseGroupId;
            }
        }
        return request;
    }

    private void showTransactionUpdateScope(String id, TransactionWriteDto request) {
        new AlertDialog.Builder(this).setTitle("Aplicar alteração")
                .setItems(new String[]{"Somente este lançamento", "Este e os futuros"}, (dialog, selected) -> {
                    request.updateScope = selected == 0 ? "current" : "future";
                    if (id != null) viewModel.saveTransaction(id, request, actionCallback("Lançamento atualizado."));
                }).show();
    }

    private void deleteTransaction(TransactionListItemDto transaction) {
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        if (invoice != null && invoice.amount(invoice.remainingBalance) <= 0) {
            Toast.makeText(this, "Estorne o pagamento da fatura antes de excluir lançamentos.", Toast.LENGTH_LONG).show();
            return;
        }
        if (transaction.id == null) return;
        if (transaction.isInstallmentTransaction() || transaction.isFixedExpenseTransaction()) {
            new AlertDialog.Builder(this).setTitle("Excluir lançamento")
                    .setItems(new String[]{"Somente este lançamento", "Este e os futuros"}, (dialog, selected) ->
                            confirmDelete(transaction, selected == 0 ? "current" : "future"))
                    .show();
        } else {
            confirmDelete(transaction, null);
        }
    }

    private void confirmDelete(TransactionListItemDto transaction, @Nullable String scope) {
        new AlertDialog.Builder(this).setTitle("Confirmar exclusão")
                .setMessage("Excluir \"" + safe(transaction.description) + "\"?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> viewModel.deleteTransaction(
                        transaction.id, scope, actionCallback("Lançamento excluído."))).show();
    }

    private void reverseTransaction(TransactionListItemDto transaction) {
        if (transaction.id == null) return;
        if (transaction.isInstallmentTransaction() || transaction.isFixedExpenseTransaction()) {
            Toast.makeText(this, "Para estornar uma série, use a edição do lançamento.", Toast.LENGTH_LONG).show();
            return;
        }
        CategoryDto category = findCategory(transaction.categoryId);
        if (category == null) {
            Toast.makeText(this, "Categoria não disponível para estorno.", Toast.LENGTH_LONG).show();
            return;
        }
        Calendar date = Calendar.getInstance();
        TransactionWriteDto reversal = buildTransactionRequest(transaction, true,
                "Estorno · " + safe(transaction.description), Math.abs(transaction.getAmountValue()), category, date);
        reversal.type = "INCOME";
        new AlertDialog.Builder(this).setTitle("Estornar lançamento")
                .setMessage("Será criado um crédito na fatura no valor de " + formatCurrency(transaction.getAmountValue()) + ".")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Estornar", (dialog, which) -> viewModel.saveTransaction(
                        null, reversal, actionCallback("Estorno registrado."))).show();
    }

    private void exportInvoice() {
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        if (invoice == null || invoice.getTransactions().isEmpty()) return;
        StringBuilder csv = new StringBuilder("Data,Descrição,Categoria,Valor,Tipo,Conciliado,Parcela\n");
        for (TransactionListItemDto transaction : invoice.getTransactions()) {
            csv.append(csvEscape(formatDate(transaction.date))).append(',')
                    .append(csvEscape(safe(transaction.description))).append(',')
                    .append(csvEscape(transaction.getCategoryName())).append(',')
                    .append(String.format(Locale.US, "%.2f", transaction.getAmountValue())).append(',')
                    .append(transaction.isIncome() ? "Receita" : "Despesa").append(',')
                    .append(transaction.isReconciled() ? "Sim" : "Não").append(',')
                    .append(transaction.isInstallmentTransaction()
                            ? safe(transaction.installmentNumber) + "/" + safe(transaction.totalInstallments) : "")
                    .append('\n');
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/csv");
        share.putExtra(Intent.EXTRA_SUBJECT, "Fatura " + selectedMonth.get(Calendar.MONTH) + "/" + selectedMonth.get(Calendar.YEAR));
        share.putExtra(Intent.EXTRA_TEXT, csv.toString());
        startActivity(Intent.createChooser(share, "Exportar lançamentos da fatura"));
    }

    private String csvEscape(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

    private CreditCardRepository.ActionCallback actionCallback(String successMessage) {
        return new CreditCardRepository.ActionCallback() {
            @Override public void onSuccess() {
                Toast.makeText(CardsActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                reloadCurrent();
            }
            @Override public void onError(@NonNull String message) {
                Toast.makeText(CardsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };
    }

    private List<CategoryDto> expenseCategories() {
        List<CategoryDto> result = new ArrayList<>();
        for (CategoryDto category : categories) {
            if (category.getType() == null || "EXPENSE".equalsIgnoreCase(category.getType())) result.add(category);
        }
        return result;
    }

    @Nullable private CategoryDto findCategory(@Nullable String id) {
        if (id == null) return null;
        for (CategoryDto category : categories) if (id.equals(category.getId())) return category;
        return null;
    }

    private <T> Spinner createObjectSpinner(List<T> items) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<T> adapter = new ArrayAdapter<>(this, R.layout.item_spinner_form, items);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        spinner.setAdapter(adapter);
        spinner.setBackgroundResource(R.drawable.bg_spinner_selector);
        spinner.setPadding(dp(16), 0, dp(44), 0);
        spinner.setMinimumHeight(dp(52));
        return spinner;
    }

    private Spinner createAccountSpinner() {
        List<String> names = new ArrayList<>();
        for (AccountDto account : accounts) names.add(safe(account.getName()));
        return createSpinner(names.toArray(new String[0]));
    }

    private LinearLayout dialogForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);
        return form;
    }

    private TextView fieldLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(getColor(R.color.on_surface_variant));
        label.setPadding(0, dp(10), 0, dp(4));
        return label;
    }

    private void showDatePicker(Calendar date, Runnable onDateChanged) {
        new android.app.DatePickerDialog(this, (picker, year, month, day) -> {
            date.set(Calendar.YEAR, year);
            date.set(Calendar.MONTH, month);
            date.set(Calendar.DAY_OF_MONTH, day);
            onDateChanged.run();
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButton(TextView button, Calendar date) {
        button.setText(new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR")).format(date.getTime()));
        button.setTextColor(getColor(R.color.on_surface));
        button.setTextSize(14);
        button.setGravity(android.view.Gravity.CENTER_VERTICAL);
    }

    private double parseLocalizedAmount(String raw) {
        String value = raw.replace("R$", "").replace(" ", "").trim();
        if (value.contains(",")) value = value.replace(".", "").replace(',', '.');
        return Double.parseDouble(value);
    }

    private double parseAmount(@Nullable String raw) {
        if (raw == null) return 0;
        try { return Double.parseDouble(raw.replace(',', '.')); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private int moneyColor(double value) {
        return getColor(value < 0 ? R.color.money_negative : R.color.money_positive);
    }

    private String formatCurrency(double value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    private String formatSummaryCurrency(double value) {
        return PrivacyMode.format(this, value);
    }

    private String formatDate(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) return "—";
        try {
            return new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"))
                    .format(Date.from(Instant.parse(value)));
        } catch (RuntimeException ignored) {
            try {
                LocalDate date = LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
                return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignoredDate) { return value; }
        }
    }

    private String safe(@Nullable Object value) { return value == null ? "" : String.valueOf(value); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) return;
        renderCardOverview(findSelectedCard());
        InvoiceDetailsDto invoice = viewModel.invoiceData.getValue();
        if (invoice != null) renderInvoice(invoice);
    }


    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        reloadCurrent();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
