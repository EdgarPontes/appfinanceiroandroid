package br.com.tecpontes.appfinanceiro.ui.reports;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.ActivityReportsBinding;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.utils.PrivacyMode;
import br.com.tecpontes.appfinanceiro.utils.ReportExporter;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.ReportsViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

/** Relatório financeiro por período, com filtros, resumo, detalhamento e exportação. */
@AndroidEntryPoint
public class ReportsActivity extends AppCompatActivity {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private ActivityReportsBinding binding;
    private ReportsViewModel viewModel;

    @Inject TokenManager tokenManager;

    private LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
    private LocalDate periodEnd = LocalDate.now();
    private String typeFilter = "ALL";

    private final Set<String> selectedCategoryIds = new HashSet<>();
    private final List<CategoryChoice> categoryChoices = new ArrayList<>();
    private final Map<String, CategoryDto> categoryMap = new HashMap<>();

    private List<TransactionListItemDto> allTransactions = new ArrayList<>();
    private List<TransactionListItemDto> filteredTransactions = new ArrayList<>();
    private List<TransactionListItemDto> detailedExpenses = new ArrayList<>();
    private List<ReportExporter.Aggregate> expenseAggregates = new ArrayList<>();
    private List<ReportExporter.Aggregate> incomeAggregates = new ArrayList<>();
    private ReportExporter.Summary summary = new ReportExporter.Summary(0, 0, 0);
    private double detailedExpensesTotal;

    private int selectedTypePosition;

    private final ActivityResultLauncher<String> csvLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"),
                    this::onCsvDestination);

    private final ActivityResultLauncher<String> pdfLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"),
                    this::onPdfDestination);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        BottomNavigationHelper.setup(this, R.id.navigation_menu);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        restoreState(savedInstanceState);
        setupTypeSpinner();
        setupListeners();
        setupObservers();
        SessionExpiredNavigation.observe(this, tokenManager);

        updatePeriodLabel();
        updateCategoryFilterLabel();
        loadReport();
    }

    private void setupTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.report_type_labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.reportTypeSpinner.setAdapter(adapter);

        String[] values = getResources().getStringArray(R.array.report_type_values);
        selectedTypePosition = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(typeFilter)) {
                selectedTypePosition = i;
                break;
            }
        }
        binding.reportTypeSpinner.setSelection(selectedTypePosition, false);

        binding.reportTypeSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == selectedTypePosition) return;
                        selectedTypePosition = position;
                        String[] values = getResources()
                                .getStringArray(R.array.report_type_values);
                        typeFilter = position >= 0 && position < values.length
                                ? values[position] : "ALL";
                        selectedCategoryIds.clear();
                        updateCategoryFilterLabel();
                        loadReport();
                    }

                    @Override public void onNothingSelected(AdapterView<?> parent) {
                        // Mantém o filtro atual.
                    }
                });
    }

    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        String start = savedInstanceState.getString("periodStart");
        String end = savedInstanceState.getString("periodEnd");
        if (start != null) {
            try {
                periodStart = LocalDate.parse(start);
            } catch (Exception ignored) {
                // Mantém o padrão.
            }
        }
        if (end != null) {
            try {
                periodEnd = LocalDate.parse(end);
            } catch (Exception ignored) {
                // Mantém o padrão.
            }
        }

        typeFilter = savedInstanceState.getString("typeFilter", "ALL");
        ArrayList<String> selected =
                savedInstanceState.getStringArrayList("selectedCategoryIds");
        selectedCategoryIds.clear();
        if (selected != null) selectedCategoryIds.addAll(selected);
    }

    private void setupListeners() {
        binding.periodButton.setOnClickListener(v -> showPeriodPicker());
        binding.categoryFilterButton.setOnClickListener(v -> showCategoryFilterDialog());
        binding.detailExpensesButton.setOnClickListener(v -> openExpensesDialog());
        binding.exportCsvButton.setOnClickListener(v ->
                csvLauncher.launch(exportFileName(".csv")));
        binding.exportPdfButton.setOnClickListener(v ->
                pdfLauncher.launch(exportFileName(".pdf")));
    }

    private void setupObservers() {
        viewModel.categoriesData.observe(this, categories -> {
            categoryChoices.clear();
            categoryMap.clear();

            if (categories != null) {
                for (CategoryDto parent : categories) {
                    if (parent.getId() != null) categoryMap.put(parent.getId(), parent);
                    String parentName = parent.getName() == null ? "" : parent.getName();
                    categoryChoices.add(new CategoryChoice(parent, parentName));

                    for (CategoryDto child : parent.getSubCategories()) {
                        if (child.getId() != null) categoryMap.put(child.getId(), child);
                        String childName = child.getName() == null ? "" : child.getName();
                        categoryChoices.add(new CategoryChoice(child,
                                parentName.isEmpty() ? childName : parentName + " / " + childName));
                    }
                }
            }

            Set<String> validIds = new HashSet<>(categoryMap.keySet());
            selectedCategoryIds.retainAll(validIds);
            updateCategoryFilterLabel();
            updateActionStates();

            // As categorias podem chegar depois das transações; recalcula nomes/cores.
            if (!allTransactions.isEmpty()) {
                applyFiltersAndRender();
            }
        });

        viewModel.transactionsData.observe(this, transactions -> {
            allTransactions = transactions == null ? new ArrayList<>() : transactions;
            applyFiltersAndRender();
        });

        viewModel.isLoading.observe(this, loading -> {
            binding.reportsProgressBar.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            updateActionStates();
        });

        viewModel.errorMessage.observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                binding.reportsErrorTextView.setVisibility(View.GONE);
            } else {
                binding.reportsErrorTextView.setText(message);
                binding.reportsErrorTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadReport() {
        viewModel.load(periodStart, periodEnd, typeFilter);
        updateActionStates();
    }

    private void updatePeriodLabel() {
        binding.periodButton.setText(periodStart.format(DATE_FORMAT)
                + " – " + periodEnd.format(DATE_FORMAT));
    }

    private void showPeriodPicker() {
        long startMillis = periodStart.atStartOfDay(ZoneOffset.UTC)
                .toInstant().toEpochMilli();
        long endMillis = periodEnd.atStartOfDay(ZoneOffset.UTC)
                .toInstant().toEpochMilli();

        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder
                .dateRangePicker()
                .setTitleText(R.string.reports_period_title)
                .setSelection(new Pair<>(startMillis, endMillis))
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) {
                return;
            }
            LocalDate first = Instant.ofEpochMilli(selection.first)
                    .atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate second = Instant.ofEpochMilli(selection.second)
                    .atZone(ZoneOffset.UTC).toLocalDate();

            periodStart = second.isBefore(first) ? second : first;
            periodEnd = second.isBefore(first) ? first : second;
            updatePeriodLabel();
            loadReport();
        });

        picker.show(getSupportFragmentManager(), "report_period_picker");
    }

    private void showCategoryFilterDialog() {
        List<CategoryChoice> options = new ArrayList<>();
        for (CategoryChoice choice : categoryChoices) {
            String categoryType = choice.category.getType();
            if ("ALL".equals(typeFilter)
                    || (categoryType != null && categoryType.equalsIgnoreCase(typeFilter))) {
                options.add(choice);
            }
        }

        if (options.isEmpty()) {
            Toast.makeText(this, R.string.reports_categories_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[options.size()];
        boolean[] checked = new boolean[options.size()];
        for (int i = 0; i < options.size(); i++) {
            labels[i] = options.get(i).label;
            String id = options.get(i).category.getId();
            checked[i] = id != null && selectedCategoryIds.contains(id);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reports_categories_title)
                .setMultiChoiceItems(labels, checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(R.string.categories_cancel, null)
                .setNeutralButton(R.string.reports_categories_clear, (dialog, which) -> {
                    selectedCategoryIds.clear();
                    updateCategoryFilterLabel();
                    applyFiltersAndRender();
                })
                .setPositiveButton(R.string.reports_categories_apply, (dialog, which) -> {
                    selectedCategoryIds.clear();
                    for (int i = 0; i < options.size(); i++) {
                        String id = options.get(i).category.getId();
                        if (checked[i] && id != null) selectedCategoryIds.add(id);
                    }
                    updateCategoryFilterLabel();
                    applyFiltersAndRender();
                })
                .show();
    }

    private void updateCategoryFilterLabel() {
        binding.categoryFilterButton.setText(selectedCategoryIds.isEmpty()
                ? getString(R.string.reports_categories_all)
                : getString(R.string.reports_categories_selected,
                        selectedCategoryIds.size()));
    }

    private void applyFiltersAndRender() {
        List<TransactionListItemDto> filtered = new ArrayList<>();
        for (TransactionListItemDto transaction : allTransactions) {
            if (selectedCategoryIds.isEmpty()
                    || (transaction.categoryId != null
                        && selectedCategoryIds.contains(transaction.categoryId))) {
                filtered.add(transaction);
            }
        }
        filteredTransactions = filtered;

        computeSummaryAndBreakdowns();
        renderSummary();
        renderBreakdowns();
        updateDetailedExpenses();
        updateActionStates();
    }

    private void computeSummaryAndBreakdowns() {
        double income = 0;
        double expenses = 0;
        Map<String, AggregateBuilder> expenseMap = new LinkedHashMap<>();
        Map<String, AggregateBuilder> incomeMap = new LinkedHashMap<>();

        for (TransactionListItemDto transaction : filteredTransactions) {
            double amount = transaction.getAmountValue();
            if ("INCOME".equalsIgnoreCase(transaction.type)) {
                income += amount;
                addAggregate(incomeMap, transaction, amount);
            } else if ("EXPENSE".equalsIgnoreCase(transaction.type)) {
                expenses += amount;
                addAggregate(expenseMap, transaction, amount);
            }
        }

        summary = new ReportExporter.Summary(income, expenses, filteredTransactions.size());
        expenseAggregates = toAggregates(expenseMap);
        incomeAggregates = toAggregates(incomeMap);
    }

    private void addAggregate(
            @NonNull Map<String, AggregateBuilder> map,
            @NonNull TransactionListItemDto transaction,
            double amount) {
        String key = transaction.categoryId != null
                ? transaction.categoryId
                : displayName(transaction);
        AggregateBuilder builder = map.get(key);
        if (builder == null) {
            builder = new AggregateBuilder(
                    displayName(transaction), displayColor(transaction));
            map.put(key, builder);
        }
        builder.amount += amount;
    }

    @NonNull
    private List<ReportExporter.Aggregate> toAggregates(
            @NonNull Map<String, AggregateBuilder> map) {
        List<ReportExporter.Aggregate> aggregates = new ArrayList<>();
        for (AggregateBuilder builder : map.values()) {
            aggregates.add(new ReportExporter.Aggregate(
                    builder.name, builder.amount, builder.color));
        }
        aggregates.sort(Comparator.comparingDouble(
                (ReportExporter.Aggregate aggregate) -> aggregate.amount).reversed());
        return aggregates;
    }

    private void renderSummary() {
        binding.totalIncomeTextView.setText(PrivacyMode.format(this, summary.income));
        binding.totalExpensesTextView.setText(PrivacyMode.format(this, summary.expenses));
        binding.balanceTextView.setText(PrivacyMode.format(this, summary.balance));
        binding.balanceTextView.setTextColor(ContextCompat.getColor(
                this, summary.balance >= 0 ? R.color.money_positive : R.color.money_negative));
        binding.transactionCountTextView.setText(String.valueOf(summary.count));
    }

    private void renderBreakdowns() {
        boolean showExpenses = !"INCOME".equals(typeFilter);
        boolean showIncome = !"EXPENSE".equals(typeFilter);

        binding.expensesBreakdownCard.setVisibility(showExpenses ? View.VISIBLE : View.GONE);
        binding.incomeBreakdownCard.setVisibility(showIncome ? View.VISIBLE : View.GONE);

        renderCategoryRows(binding.expensesBreakdownContainer,
                binding.expensesBreakdownEmptyTextView, expenseAggregates,
                R.color.money_negative);
        renderCategoryRows(binding.incomeBreakdownContainer,
                binding.incomeBreakdownEmptyTextView, incomeAggregates,
                R.color.money_positive);
    }

    private void renderCategoryRows(
            @NonNull android.widget.LinearLayout container,
            @NonNull TextView emptyView,
            @NonNull List<ReportExporter.Aggregate> aggregates,
            int amountColor) {
        container.removeAllViews();
        emptyView.setVisibility(aggregates.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ReportExporter.Aggregate aggregate : aggregates) {
            View row = inflater.inflate(R.layout.item_report_category, container, false);
            View colorView = row.findViewById(R.id.reportCategoryColorView);
            TextView nameView = row.findViewById(R.id.reportCategoryNameTextView);
            TextView amountView = row.findViewById(R.id.reportCategoryAmountTextView);

            nameView.setText(aggregate.name);
            amountView.setText(PrivacyMode.format(this, aggregate.amount));
            amountView.setTextColor(ContextCompat.getColor(this, amountColor));

            try {
                colorView.setBackgroundTintList(ColorStateList.valueOf(
                        Color.parseColor(aggregate.color)));
            } catch (IllegalArgumentException ignored) {
                colorView.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.money_neutral)));
            }

            container.addView(row);
        }
    }

    private void updateDetailedExpenses() {
        detailedExpenses = new ArrayList<>();
        detailedExpensesTotal = 0;
        for (TransactionListItemDto transaction : filteredTransactions) {
            if ("EXPENSE".equalsIgnoreCase(transaction.type)) {
                detailedExpenses.add(transaction);
                detailedExpensesTotal += transaction.getAmountValue();
            }
        }
        detailedExpenses.sort(Comparator.comparingLong(
                TransactionListItemDto::getDateMillis));
    }

    private void updateActionStates() {
        if (binding == null) return;
        boolean loading = Boolean.TRUE.equals(viewModel.isLoading.getValue());
        binding.exportCsvButton.setEnabled(!loading);
        binding.exportPdfButton.setEnabled(!loading);
        binding.detailExpensesButton.setEnabled(!loading && !detailedExpenses.isEmpty());
        binding.categoryFilterButton.setEnabled(!loading && !categoryChoices.isEmpty());
    }

    private void openExpensesDialog() {
        if (detailedExpenses.isEmpty()) return;
        new ReportExpensesDialog(this, new ArrayList<>(detailedExpenses),
                detailedExpensesTotal).show();
    }

    private void onCsvDestination(@Nullable Uri uri) {
        if (uri == null) return;

        List<TransactionListItemDto> data = new ArrayList<>(filteredTransactions);
        ReportExporter.Summary summarySnapshot = summary;
        LocalDate from = periodStart;
        LocalDate to = periodEnd;

        new Thread(() -> {
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output == null) throw new IOException("Fluxo de saída indisponível");
                ReportExporter.writeCsv(output, data, summarySnapshot, from, to);
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.reports_export_success, Toast.LENGTH_SHORT).show());
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.reports_export_error, Toast.LENGTH_LONG).show());
            }
        }, "report-csv-export").start();
    }

    private void onPdfDestination(@Nullable Uri uri) {
        if (uri == null) return;

        List<TransactionListItemDto> data = new ArrayList<>(filteredTransactions);
        List<ReportExporter.Aggregate> expensesSnapshot = new ArrayList<>(expenseAggregates);
        List<ReportExporter.Aggregate> incomeSnapshot = new ArrayList<>(incomeAggregates);
        ReportExporter.Summary summarySnapshot = summary;
        LocalDate from = periodStart;
        LocalDate to = periodEnd;

        new Thread(() -> {
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output == null) throw new IOException("Fluxo de saída indisponível");
                ReportExporter.writePdf(output, data, expensesSnapshot, incomeSnapshot,
                        summarySnapshot, from, to);
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.reports_export_success, Toast.LENGTH_SHORT).show());
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.reports_export_error, Toast.LENGTH_LONG).show());
            }
        }, "report-pdf-export").start();
    }

    @NonNull
    private String exportFileName(@NonNull String extension) {
        return "relatorio_" + periodStart.format(FILE_DATE)
                + "_a_" + periodEnd.format(FILE_DATE) + extension;
    }

    @NonNull
    private String displayName(@NonNull TransactionListItemDto transaction) {
        CategoryDto category = transaction.categoryId == null
                ? null : categoryMap.get(transaction.categoryId);
        if (category == null) {
            return transaction.getCategoryName();
        }
        if (category.getParentId() != null) {
            CategoryDto parent = categoryMap.get(category.getParentId());
            if (parent != null && parent.getName() != null) {
                return parent.getName() + " / " + category.getName();
            }
        }
        return category.getName() == null ? "Sem categoria" : category.getName();
    }

    @NonNull
    private String displayColor(@NonNull TransactionListItemDto transaction) {
        CategoryDto category = transaction.categoryId == null
                ? null : categoryMap.get(transaction.categoryId);
        if (category != null) {
            if (category.getColor() != null) return category.getColor();
            if (category.getParentId() != null) {
                CategoryDto parent = categoryMap.get(category.getParentId());
                if (parent != null && parent.getColor() != null) return parent.getColor();
            }
        }
        if (transaction.category != null && transaction.category.color != null) {
            return transaction.category.color;
        }
        return "#94A3B8";
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) applyFiltersAndRender();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("periodStart", periodStart.toString());
        outState.putString("periodEnd", periodEnd.toString());
        outState.putString("typeFilter", typeFilter);
        outState.putStringArrayList("selectedCategoryIds",
                new ArrayList<>(selectedCategoryIds));
    }

    private static class CategoryChoice {
        final CategoryDto category;
        final String label;

        CategoryChoice(CategoryDto category, String label) {
            this.category = category;
            this.label = label;
        }
    }

    private static class AggregateBuilder {
        final String name;
        final String color;
        double amount;

        AggregateBuilder(String name, String color) {
            this.name = name;
            this.color = color;
        }
    }
}
