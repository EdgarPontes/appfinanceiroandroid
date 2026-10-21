package br.com.tecpontes.appfinanceiro.ui.transactions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.data.local.entity.Account;
import br.com.tecpontes.appfinanceiro.databinding.ActivityTransactionsBinding;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.viewmodel.TransactionsViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lista mobile de transações com filtros e dados detalhados fornecidos pela API. */
@AndroidEntryPoint
public class TransactionsActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_IMPORT = "open_ofx_import";
    private static final String TAG = "TransactionsActivity";

    private ActivityTransactionsBinding binding;
    private TransactionsViewModel transactionsViewModel;
    private TransactionsAdapter transactionsAdapter;

    private ActivityResultLauncher<String[]> ofxFilePickerLauncher;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private List<Account> accounts = new ArrayList<>();
    private List<String> selectedAccountIds = new ArrayList<>();
    private boolean accountSelectionInitialized;
    private List<TransactionListItemDto> apiTransactions = new ArrayList<>();
    private final List<CategoryOption> categoryOptions = new ArrayList<>();

    private int selectedPeriodPosition;
    private int selectedCategoryPosition;
    private String selectedType;
    private String selectedCategoryId;
    private String selectedReconciliation;
    private String selectedSort = "date_asc";
    private String searchQuery = "";
    private Calendar periodStart;
    private Calendar periodEnd;
    private double selectedBalance;
    private boolean balanceAvailable;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BottomNavigationHelper.setup(this, R.id.navigation_transactions);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());

        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        transactionsAdapter = new TransactionsAdapter(
                this::showTransactionDetails, this::showTransactionOptions);
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.transactionsRecyclerView.setAdapter(transactionsAdapter);

        setupSwipeRefresh();
        setupOfxFilePicker();
        setupFilters();
        setupObservers();
        setupListeners();
        loadInitialData();

        if (getIntent().getBooleanExtra(EXTRA_OPEN_IMPORT, false)) {
            binding.importOfxButton.post(this::openOfxFilePicker);
        }
        Log.d(TAG, "TransactionsActivity criada");
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        binding.swipeRefreshLayout.setOnRefreshListener(this::reloadData);
    }

    private void setupOfxFilePicker() {
        ofxFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::handleOfxFileSelection);
    }

    private void setupFilters() {
        setupPeriodSpinner();
        setupTypeSpinner();
        setupReconciliationSpinner();
        setupSortSpinner();
        setupCategorySpinner();

        binding.searchTransactionsEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim();
                if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
                pendingSearch = TransactionsActivity.this::applyLocalFilters;
                searchHandler.postDelayed(pendingSearch, 250);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupPeriodSpinner() {
        String[] labels = {"Este mês", "Mês anterior", "Últimos 3 meses", "Todo o período"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.periodSpinner.setAdapter(adapter);
        binding.periodSpinner.setSelection(0, false);
        binding.periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == selectedPeriodPosition) return;
                selectedPeriodPosition = position;
                updatePeriodRange();
                reloadData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupTypeSpinner() {
        setSpinner(binding.typeFilterSpinner,
                new String[]{"Todos os tipos", "Receitas", "Despesas"}, position -> {
                    selectedType = position == 1 ? "INCOME" : position == 2 ? "EXPENSE" : null;
                    applyLocalFilters();
                });
    }

    private void setupReconciliationSpinner() {
        setSpinner(binding.reconciliationFilterSpinner,
                new String[]{"Conciliação", "Pendentes", "Conciliados"}, position -> {
                    selectedReconciliation = position == 1 ? "false" : position == 2 ? "true" : null;
                    applyLocalFilters();
                });
    }

    private void setupSortSpinner() {
        setSpinner(binding.sortFilterSpinner,
                new String[]{"Data: antigas primeiro", "Data: recentes primeiro",
                        "Valor: maior primeiro", "Valor: menor primeiro", "Descrição: A–Z"},
                position -> {
                    selectedSort = position == 1 ? "date_desc"
                            : position == 2 ? "amount_desc"
                            : position == 3 ? "amount_asc"
                            : position == 4 ? "description_asc" : "date_asc";
                    applyLocalFilters();
                });
    }

    private void setSpinner(Spinner spinner, String[] labels, OnSpinnerSelection listener) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listener.onSelected(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCategorySpinner() {
        categoryOptions.clear();
        categoryOptions.add(new CategoryOption(null, "Todas categorias"));
        ArrayAdapter<CategoryOption> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoryOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categoryFilterSpinner.setAdapter(adapter);
        binding.categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == selectedCategoryPosition) return;
                selectedCategoryPosition = position;
                selectedCategoryId = categoryOptions.get(position).id;
                applyLocalFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupObservers() {
        transactionsViewModel.loading.observe(this, loading -> {
            binding.swipeRefreshLayout.setRefreshing(loading);
            binding.loadingView.getRoot().setVisibility(loading ? View.VISIBLE : View.GONE);
        });
        transactionsViewModel.error.observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
        transactionsViewModel.filteredTransactions.observe(this, transactions -> {
            apiTransactions = transactions != null ? transactions : new ArrayList<>();
            updateCategoryOptions();
            applyLocalFilters();
        });
        transactionsViewModel.accounts.observe(this, loadedAccounts -> {
            if (loadedAccounts == null) return;
            accounts = loadedAccounts;
            if (!accounts.isEmpty() && !accountSelectionInitialized) {
                selectedAccountIds = new ArrayList<>();
                for (Account account : accounts) selectedAccountIds.add(account.getId());
                updateAccountButton();
                requestBalanceForPeriod();
            } else {
                updateAccountButton();
            }
        });
        transactionsViewModel.balanceAtDate.observe(this, balance -> {
            if (balance == null) return;
            selectedBalance = 0;
            if (balance.byAccount != null) {
                for (String accountId : selectedAccountIds) {
                    selectedBalance += parseAmount(balance.byAccount.get(accountId));
                }
            }
            balanceAvailable = true;
            applyLocalFilters();
        });
        transactionsViewModel.importSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Arquivo OFX importado com sucesso!", Toast.LENGTH_SHORT).show();
                reloadData();
            }
        });
    }

    private void setupListeners() {
        binding.backButton.setOnClickListener(v -> finish());
        binding.addTransactionFab.setOnClickListener(v -> showCreateTransactionDialog());
        binding.addManualButton.setOnClickListener(v -> showCreateTransactionDialog());
        binding.importOfxButton.setOnClickListener(v -> openOfxFilePicker());
        binding.syncButton.setOnClickListener(v -> reloadData());
        binding.accountsFilterButton.setOnClickListener(v -> showAccountsDialog());
    }

    private void loadInitialData() {
        updatePeriodRange();
        transactionsViewModel.syncAccounts();
        reloadData();
    }

    private void reloadData() {
        updatePeriodRange();
        String start = periodStart == null ? null : periodStart.toInstant().toString();
        String end = periodEnd == null ? null : periodEnd.toInstant().toString();
        // A tela web carrega todas as contas e aplica a seleção do usuário na lista.
        transactionsViewModel.loadFilteredTransactions(null, start, end, null, null, null, null);
        requestBalanceForPeriod();
    }

    private void updatePeriodRange() {
        Calendar now = Calendar.getInstance();
        periodEnd = (Calendar) now.clone();
        periodEnd.set(Calendar.HOUR_OF_DAY, 23);
        periodEnd.set(Calendar.MINUTE, 59);
        periodEnd.set(Calendar.SECOND, 59);
        periodEnd.set(Calendar.MILLISECOND, 999);

        if (selectedPeriodPosition == 3) {
            periodStart = null;
            return;
        }

        Calendar start = (Calendar) now.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        if (selectedPeriodPosition == 1) {
            start.add(Calendar.MONTH, -1);
            periodEnd.set(Calendar.DAY_OF_MONTH, 1);
            periodEnd.add(Calendar.DAY_OF_MONTH, -1);
            periodEnd.set(Calendar.HOUR_OF_DAY, 23);
            periodEnd.set(Calendar.MINUTE, 59);
            periodEnd.set(Calendar.SECOND, 59);
            periodEnd.set(Calendar.MILLISECOND, 999);
        } else if (selectedPeriodPosition == 2) {
            start.add(Calendar.MONTH, -2);
        }
        periodStart = start;
    }

    private void requestBalanceForPeriod() {
        if (selectedAccountIds.isEmpty()) {
            selectedBalance = 0;
            balanceAvailable = false;
            updateSummary(new ArrayList<>());
            return;
        }
        balanceAvailable = false;
        Calendar balanceDate = periodEnd != null ? periodEnd : Calendar.getInstance();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(balanceDate.getTime());
        transactionsViewModel.loadBalanceAtDate(joinAccountIds(), date);
    }

    private String joinAccountIds() {
        StringBuilder result = new StringBuilder();
        for (String accountId : selectedAccountIds) {
            if (result.length() > 0) result.append(',');
            result.append(accountId);
        }
        return result.toString();
    }

    private void showAccountsDialog() {
        if (accounts.isEmpty()) {
            Toast.makeText(this, "Nenhuma conta disponível para filtrar", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[accounts.size()];
        boolean[] checked = new boolean[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            names[i] = account.getName();
            checked[i] = selectedAccountIds.contains(account.getId());
        }

        new AlertDialog.Builder(this)
                .setTitle("Contas da lista")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNeutralButton("Todas", (dialog, which) -> {
                    selectedAccountIds = new ArrayList<>();
                    for (Account account : accounts) selectedAccountIds.add(account.getId());
                    accountSelectionInitialized = true;
                    updateAccountButton();
                    requestBalanceForPeriod();
                    applyLocalFilters();
                })
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aplicar", (dialog, which) -> {
                    selectedAccountIds = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) selectedAccountIds.add(accounts.get(i).getId());
                    }
                    accountSelectionInitialized = true;
                    updateAccountButton();
                    requestBalanceForPeriod();
                    applyLocalFilters();
                })
                .show();
    }

    private void updateAccountButton() {
        if (!accountSelectionInitialized || selectedAccountIds.size() == accounts.size()) {
            binding.accountsFilterButton.setText("Todas as contas");
        } else if (selectedAccountIds.isEmpty()) {
            binding.accountsFilterButton.setText("Nenhuma conta");
        } else if (selectedAccountIds.size() == 1) {
            String name = "1 conta";
            for (Account account : accounts) {
                if (selectedAccountIds.contains(account.getId())) name = account.getName();
            }
            binding.accountsFilterButton.setText(name);
        } else {
            binding.accountsFilterButton.setText(selectedAccountIds.size() + " contas");
        }
    }

    private void updateCategoryOptions() {
        Map<String, String> labelsById = new LinkedHashMap<>();
        for (TransactionListItemDto transaction : apiTransactions) {
            if (transaction.categoryId != null && transaction.category != null
                    && transaction.category.name != null) {
                labelsById.put(transaction.categoryId, transaction.category.name);
            }
        }

        if (selectedCategoryId != null && !labelsById.containsKey(selectedCategoryId)) {
            selectedCategoryId = null;
        }
        categoryOptions.clear();
        categoryOptions.add(new CategoryOption(null, "Todas categorias"));
        for (Map.Entry<String, String> entry : labelsById.entrySet()) {
            categoryOptions.add(new CategoryOption(entry.getKey(), entry.getValue()));
        }
        selectedCategoryPosition = 0;
        for (int i = 0; i < categoryOptions.size(); i++) {
            if (categoryOptions.get(i).id != null && categoryOptions.get(i).id.equals(selectedCategoryId)) {
                selectedCategoryPosition = i;
                break;
            }
        }
        ArrayAdapter<CategoryOption> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoryOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categoryFilterSpinner.setAdapter(adapter);
        binding.categoryFilterSpinner.setSelection(selectedCategoryPosition, false);
    }

    private void applyLocalFilters() {
        List<TransactionListItemDto> filtered = new ArrayList<>();
        String term = searchQuery.toLowerCase(Locale.ROOT);

        for (TransactionListItemDto item : apiTransactions) {
            if (item == null) continue;
            if (accountSelectionInitialized) {
                if (selectedAccountIds.isEmpty()) continue;
                boolean belongsToSelectedAccount = item.accountId != null
                        && selectedAccountIds.contains(item.accountId);
                if (!belongsToSelectedAccount && item.card != null && item.card.debitAccountId != null) {
                    belongsToSelectedAccount = selectedAccountIds.contains(item.card.debitAccountId);
                }
                if (!belongsToSelectedAccount) continue;
            }
            if (selectedType != null && !selectedType.equalsIgnoreCase(item.type)) continue;
            if (selectedCategoryId != null && !selectedCategoryId.equals(item.categoryId)) continue;
            if (selectedReconciliation != null
                    && Boolean.parseBoolean(selectedReconciliation) != item.isReconciled()) continue;
            if (!term.isEmpty()) {
                String searchable = ((item.description == null ? "" : item.description) + " "
                        + item.getCategoryName() + " " + item.getSourceName()).toLowerCase(Locale.ROOT);
                if (!searchable.contains(term)) continue;
            }
            filtered.add(item);
        }

        sortTransactions(filtered);
        applyRunningBalances(filtered);
        transactionsAdapter.updateTransactions(filtered);
        updateEmptyState(filtered.isEmpty());
        updateSummary(filtered);
    }

    private void sortTransactions(List<TransactionListItemDto> values) {
        if ("date_desc".equals(selectedSort)) {
            values.sort(Comparator.comparingLong(TransactionListItemDto::getDateMillis).reversed());
        } else if ("amount_desc".equals(selectedSort)) {
            values.sort(Comparator.comparingDouble(TransactionListItemDto::getAmountValue).reversed());
        } else if ("amount_asc".equals(selectedSort)) {
            values.sort(Comparator.comparingDouble(TransactionListItemDto::getAmountValue));
        } else if ("description_asc".equals(selectedSort)) {
            values.sort(Comparator.comparing((TransactionListItemDto item) ->
                    item.description == null ? "" : item.description.toLowerCase(Locale.ROOT)));
        } else {
            values.sort(Comparator.comparingLong(TransactionListItemDto::getDateMillis));
        }
    }

    private void applyRunningBalances(List<TransactionListItemDto> values) {
        if (!balanceAvailable || (!"date_asc".equals(selectedSort) && !"date_desc".equals(selectedSort))) {
            for (TransactionListItemDto item : values) item.clearRunningBalance();
            return;
        }
        double netChange = 0;
        for (TransactionListItemDto item : values) netChange += signedAmount(item);
        if ("date_desc".equals(selectedSort)) {
            double running = selectedBalance;
            for (TransactionListItemDto item : values) {
                item.setRunningBalance(running);
                running -= signedAmount(item);
            }
        } else {
            double running = selectedBalance - netChange;
            for (TransactionListItemDto item : values) {
                running += signedAmount(item);
                item.setRunningBalance(running);
            }
        }
    }

    private double signedAmount(TransactionListItemDto item) {
        return item.isIncome() ? item.getAmountValue() : -item.getAmountValue();
    }

    private void updateSummary(List<TransactionListItemDto> visible) {
        double periodResult = 0;
        for (TransactionListItemDto item : visible) periodResult += signedAmount(item);
        binding.selectedBalanceTextView.setText(formatCurrency(selectedBalance));
        binding.selectedBalanceTextView.setTextColor(moneyColor(selectedBalance));
        binding.periodResultTextView.setText((periodResult >= 0 ? "+" : "−")
                + formatCurrency(Math.abs(periodResult)));
        binding.periodResultTextView.setTextColor(moneyColor(periodResult));
        binding.transactionsCountTextView.setText(visible.size()
                + (visible.size() == 1 ? " item" : " itens"));
    }

    private void handleOfxFileSelection(Uri uri) {
        if (uri != null) {
            Log.d(TAG, "Arquivo OFX selecionado: " + uri.getPath());
            transactionsViewModel.importOfx(uri, uri.getPath());
        }
    }

    private void openOfxFilePicker() {
        ofxFilePickerLauncher.launch(new String[]{"application/octet-stream", "text/xml", "text/plain"});
    }

    private void showCreateTransactionDialog() {
        CreateTransactionDialog dialog = new CreateTransactionDialog(
                this,
                (amount, category, note, type) -> Toast.makeText(
                        this, "Cadastro de transações pelo app ainda não está disponível", Toast.LENGTH_SHORT).show());
        dialog.show();
    }

    private void showTransactionDetails(TransactionListItemDto item) {
        String description = item.description == null ? item.getCategoryName() : item.description;
        String state = item.isReconciled() ? "Conciliado" : "Pendente de conciliação";
        String source = item.getSourceName();
        String date = formatDate(item.getDateMillis());
        String amount = formatCurrency(item.getAmountValue());
        String details = "Categoria: " + item.getCategoryName()
                + "\nConta/cartão: " + source
                + "\nData: " + date
                + "\nValor: " + (item.isIncome() ? "+" : "−") + amount
                + "\nStatus: " + state;
        if (item.note != null && !item.note.trim().isEmpty()) {
            details += "\nObservação: " + item.note;
        }
        if (item.isInstallmentTransaction() && item.installmentNumber != null) {
            details += "\nParcela: " + item.installmentNumber + "/" + item.totalInstallments;
        }
        new AlertDialog.Builder(this)
                .setTitle(description)
                .setMessage(details)
                .setPositiveButton("Fechar", null)
                .show();
    }

    private void showTransactionOptions(View anchor, TransactionListItemDto item) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Editar").setOnMenuItemClickListener(clicked -> {
            showEditTransactionDialog(item);
            return true;
        });
        menu.getMenu().add("Excluir").setOnMenuItemClickListener(clicked -> {
            requestTransactionDelete(item);
            return true;
        });
        menu.getMenu().add("Clonar").setOnMenuItemClickListener(clicked -> {
            confirmTransactionClone(item);
            return true;
        });
        menu.show();
    }

    private void showEditTransactionDialog(TransactionListItemDto item) {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), dp(8), dp(20), 0);

        EditText descriptionInput = new EditText(this);
        descriptionInput.setSingleLine(true);
        descriptionInput.setHint("Descrição");
        descriptionInput.setText(item.description == null ? "" : item.description);
        fields.addView(descriptionInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText amountInput = new EditText(this);
        amountInput.setSingleLine(true);
        amountInput.setHint("Valor");
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        amountInput.setText(item.amount == null ? "" : item.amount);
        fields.addView(amountInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText noteInput = new EditText(this);
        noteInput.setHint("Observação (opcional)");
        noteInput.setText(item.note == null ? "" : item.note);
        fields.addView(noteInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Editar lançamento")
                .setView(fields)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (dialog, which) -> {
                    String description = descriptionInput.getText().toString().trim();
                    String amount = normalizeAmount(amountInput.getText().toString());
                    if (description.isEmpty() || amount.isEmpty()) {
                        Toast.makeText(this, "Descrição e valor são obrigatórios", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String note = noteInput.getText().toString().trim();
                    if (item.isInstallmentTransaction() || item.isFixedExpenseTransaction()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Aplicar alteração")
                                .setItems(new String[]{"Somente este lançamento", "Este e os futuros"},
                                        (scopeDialog, selected) -> updateRemoteTransaction(
                                                item, description, amount, note,
                                                selected == 0 ? "current" : "future"))
                                .show();
                    } else {
                        updateRemoteTransaction(item, description, amount, note, null);
                    }
                })
                .show();
    }

    private void updateRemoteTransaction(
            TransactionListItemDto item, String description, String amount,
            String note, @Nullable String scope) {
        if (item.id == null || item.id.isEmpty()) {
            Toast.makeText(this, "ID do lançamento indisponível", Toast.LENGTH_LONG).show();
            return;
        }
        TransactionWriteDto request = toWriteDto(item);
        request.description = description;
        request.amount = amount;
        request.note = note;
        request.updateScope = scope;
        request.installmentGroupId = item.installmentGroupId;

        transactionsViewModel.updateTransactionFromList(item.id, request,
                new TransactionsViewModel.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(TransactionsActivity.this,
                                "Lançamento atualizado", Toast.LENGTH_SHORT).show();
                        reloadData();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(TransactionsActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void requestTransactionDelete(TransactionListItemDto item) {
        if (item.isInstallmentTransaction() || item.isFixedExpenseTransaction()) {
            new AlertDialog.Builder(this)
                    .setTitle("Excluir lançamento")
                    .setItems(new String[]{"Somente este lançamento", "Este e os futuros"},
                            (dialog, selected) -> confirmRemoteDelete(
                                    item, selected == 0 ? "current" : "future"))
                    .show();
        } else {
            confirmRemoteDelete(item, null);
        }
    }

    private void confirmRemoteDelete(TransactionListItemDto item, @Nullable String scope) {
        if (item.id == null || item.id.isEmpty()) {
            Toast.makeText(this, "ID do lançamento indisponível", Toast.LENGTH_LONG).show();
            return;
        }
        String description = item.description == null ? item.getCategoryName() : item.description;
        String message = "Deseja excluir \"" + description + "\"?";
        if ("future".equals(scope)) {
            message = "Isso excluirá este lançamento e os futuros do mesmo grupo. Continuar?";
        }
        new AlertDialog.Builder(this)
                .setTitle("Confirmar exclusão")
                .setMessage(message)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) ->
                        transactionsViewModel.deleteRemoteTransaction(item.id, scope,
                                new TransactionsViewModel.ActionCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(TransactionsActivity.this,
                                                "Lançamento excluído", Toast.LENGTH_SHORT).show();
                                        reloadData();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(TransactionsActivity.this,
                                                error, Toast.LENGTH_LONG).show();
                                    }
                                }))
                .show();
    }

    private void confirmTransactionClone(TransactionListItemDto item) {
        if (item.categoryId == null || item.date == null || item.type == null) {
            Toast.makeText(this, "Não é possível clonar: faltam dados obrigatórios", Toast.LENGTH_LONG).show();
            return;
        }
        if (item.isInstallmentTransaction() && item.totalInstallments == null) {
            Toast.makeText(this, "Não é possível clonar: número de parcelas indisponível", Toast.LENGTH_LONG).show();
            return;
        }
        if (!"INCOME".equals(item.type) && !"EXPENSE".equals(item.type)) {
            Toast.makeText(this, "Este tipo de lançamento não pode ser clonado", Toast.LENGTH_LONG).show();
            return;
        }
        String description = item.description == null ? item.getCategoryName() : item.description;
        String message = "Criar uma cópia de \"" + description + "\"?";
        if (item.isInstallmentTransaction()) message += " A nova cópia manterá as parcelas.";
        if (item.isFixedExpenseTransaction()) message += " A nova cópia criará uma nova série fixa.";
        new AlertDialog.Builder(this)
                .setTitle("Clonar lançamento")
                .setMessage(message)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Clonar", (dialog, which) -> {
                    TransactionWriteDto request = toWriteDto(item);
                    request.type = item.type;
                    request.installmentGroupId = null;
                    request.fixedExpenseGroupId = null;
                    if (item.isInstallmentTransaction()) {
                        request.isInstallment = true;
                        request.installmentValueType = "total";
                        if (request.totalValue == null) {
                            request.totalValue = java.math.BigDecimal.valueOf(item.getAmountValue())
                                    .multiply(java.math.BigDecimal.valueOf(item.totalInstallments))
                                    .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
                        }
                    }
                    transactionsViewModel.cloneTransaction(request, new TransactionsViewModel.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(TransactionsActivity.this,
                                    "Lançamento clonado", Toast.LENGTH_SHORT).show();
                            reloadData();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(TransactionsActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    private TransactionWriteDto toWriteDto(TransactionListItemDto item) {
        TransactionWriteDto request = new TransactionWriteDto();
        request.description = item.description == null ? item.getCategoryName() : item.description;
        request.amount = item.amount;
        request.date = item.date;
        request.type = ("INCOME".equals(item.type) || "EXPENSE".equals(item.type))
                ? item.type : null;
        request.categoryId = item.categoryId;
        request.accountId = item.accountId;
        request.cardId = item.cardId;
        request.note = item.note;
        request.fixedExpenseGroupId = item.fixedExpenseGroupId;
        request.isFixedExpense = item.isFixedExpenseTransaction();
        request.isInstallment = item.isInstallmentTransaction();
        request.totalInstallments = item.totalInstallments;
        request.totalValue = item.totalValue;
        request.installmentValueType = item.installmentValueType;
        return request;
    }

    private String normalizeAmount(String value) {
        if (value == null) return "";
        String normalized = value.replace("R$", "").replace(" ", "").trim();
        if (normalized.contains(",")) normalized = normalized.replace(".", "").replace(',', '.');
        try {
            return new java.math.BigDecimal(normalized).abs().toPlainString();
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void updateEmptyState(boolean isEmpty) {
        binding.transactionsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.emptyStateTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            boolean hasActiveFilter = !searchQuery.isEmpty() || selectedType != null
                    || selectedCategoryId != null || selectedReconciliation != null
                    || (accountSelectionInitialized && selectedAccountIds.isEmpty());
            binding.emptyStateTextView.setText(hasActiveFilter
                    ? "Nenhuma transação encontrada. Tente ajustar os filtros."
                    : "Nenhuma transação neste período. Importe um OFX ou adicione um lançamento.");
        }
    }

    private String formatCurrency(double value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    private double parseAmount(@Nullable String value) {
        if (value == null) return 0;
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int moneyColor(double amount) {
        return getColor(amount >= 0 ? R.color.money_positive : R.color.money_negative);
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR")).format(new Date(millis));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra(EXTRA_OPEN_IMPORT, false) && ofxFilePickerLauncher != null) {
            binding.importOfxButton.post(this::openOfxFilePicker);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        binding = null;
    }

    private interface OnSpinnerSelection {
        void onSelected(int position);
    }

    private static class CategoryOption {
        final String id;
        final String label;
        CategoryOption(String id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }
}
