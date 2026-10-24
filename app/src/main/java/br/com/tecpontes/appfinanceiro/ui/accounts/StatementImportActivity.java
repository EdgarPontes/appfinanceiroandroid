package br.com.tecpontes.appfinanceiro.ui.accounts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.data.repository.AccountsRepository;
import br.com.tecpontes.appfinanceiro.databinding.ActivityStatementImportBinding;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationConfirmRequestDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationConfirmResultDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationPreviewDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationPreviewRequestDto;
import br.com.tecpontes.appfinanceiro.model.StatementEntryDto;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.StatementFileParser;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.AccountsViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

/** Importa extratos CSV/OFX, mostra correspondências e confirma a conciliação. */
@AndroidEntryPoint
public class StatementImportActivity extends AppCompatActivity {
    public static final String EXTRA_ACCOUNT_ID = "statement_account_id";
    public static final String EXTRA_ACCOUNT_NAME = "statement_account_name";
    public static final String EXTRA_FORMAT = "statement_format";
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024;

    private ActivityStatementImportBinding binding;
    private AccountsViewModel viewModel;
    private ActivityResultLauncher<String[]> filePicker;
    private String accountId;
    private String format;
    private String selectedFileName;
    private List<CategoryDto> categories = new ArrayList<>();
    private ReconciliationPreviewDto preview;
    private final Set<String> selectedMatches = new HashSet<>();
    private final Set<Integer> selectedUnmatched = new HashSet<>();
    private final Map<Integer, Spinner> categorySpinners = new HashMap<>();

    @Inject TokenManager tokenManager;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityStatementImportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });
        binding.importToolbar.setNavigationOnClickListener(v -> finish());
        viewModel = new ViewModelProvider(this).get(AccountsViewModel.class);
        accountId = getIntent().getStringExtra(EXTRA_ACCOUNT_ID);
        format = getIntent().getStringExtra(EXTRA_FORMAT);
        if (format == null) format = "csv";
        binding.importAccountTextView.setText("Conta: " + safe(getIntent().getStringExtra(EXTRA_ACCOUNT_NAME))
                + " · Extrato " + format.toUpperCase(Locale.ROOT));
        setupPicker();
        setupObservers();
        binding.selectStatementButton.setOnClickListener(v -> openPicker());
        binding.confirmReconciliationButton.setOnClickListener(v -> confirmReconciliation());
        SessionExpiredNavigation.observe(this, tokenManager);
        viewModel.loadCategories();
    }

    private void setupPicker() {
        filePicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFileSelected);
    }

    private void setupObservers() {
        viewModel.categoriesData.observe(this, value -> categories = value == null ? new ArrayList<>() : value);
        viewModel.errorMessage.observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                binding.importErrorTextView.setVisibility(View.GONE);
            } else {
                showError(message);
            }
        });
    }

    private void openPicker() {
        if ("ofx".equalsIgnoreCase(format)) {
            filePicker.launch(new String[]{"application/octet-stream", "text/plain", "application/x-ofx"});
        } else {
            filePicker.launch(new String[]{"text/*", "application/vnd.ms-excel", "application/csv", "application/octet-stream"});
        }
    }

    private void onFileSelected(@Nullable Uri uri) {
        if (uri == null) return;
        showLoading(true);
        clearPreview();
        binding.importErrorTextView.setVisibility(View.GONE);
        selectedFileName = displayName(uri);
        binding.importFileNameTextView.setText(selectedFileName);
        binding.importFileNameTextView.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String contents = readContent(uri);
                List<StatementEntryDto> entries = "ofx".equalsIgnoreCase(format)
                        ? StatementFileParser.parseOfx(contents) : StatementFileParser.parseCsv(contents);
                if (entries.size() > 1000) throw new IllegalArgumentException("O extrato pode conter no máximo 1.000 transações.");
                runOnUiThread(() -> requestPreview(entries));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(error.getMessage() == null ? "Não foi possível ler o arquivo." : error.getMessage());
                });
            }
        }, "statement-file-reader").start();
    }

    private String readContent(Uri uri) throws Exception {
        try (InputStream input = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalArgumentException("Não foi possível abrir o arquivo.");
            byte[] buffer = new byte[8192];
            int count;
            int total = 0;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_FILE_SIZE) throw new IllegalArgumentException("O arquivo excede o limite de 5 MB.");
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        }
    }

    private void requestPreview(List<StatementEntryDto> entries) {
        if (accountId == null || accountId.trim().isEmpty()) {
            showLoading(false);
            showError("Conta não identificada para a importação.");
            return;
        }
        viewModel.preview(new ReconciliationPreviewRequestDto(accountId, entries),
                new AccountsRepository.ResultCallback<ReconciliationPreviewDto>() {
                    @Override public void onSuccess(@Nullable ReconciliationPreviewDto result) {
                        showLoading(false);
                        preview = result;
                        selectedMatches.clear();
                        selectedUnmatched.clear();
                        categorySpinners.clear();
                        if (preview != null) {
                            for (ReconciliationPreviewDto.Match match : preview.getMatches()) {
                                if ("HIGH".equalsIgnoreCase(match.confidence) && match.transactionId != null) {
                                    selectedMatches.add(match.transactionId);
                                }
                            }
                            for (Integer index : preview.getUnmatchedStatementIndices()) {
                                List<ReconciliationPreviewDto.Suggestion> suggestions = suggestions(index);
                                if (!suggestions.isEmpty()) selectedUnmatched.add(index);
                            }
                            renderPreview();
                        }
                    }
                    @Override public void onError(@NonNull String message) {
                        showLoading(false);
                        showError(message);
                    }
                });
    }

    private void renderPreview() {
        binding.importMatchesContainer.removeAllViews();
        binding.importUnmatchedContainer.removeAllViews();
        binding.summaryCard.setVisibility(View.GONE);
        binding.confirmReconciliationButton.setEnabled(false);
        if (preview == null) return;
        binding.importEmptyTextView.setVisibility(View.GONE);
        binding.summaryCard.setVisibility(View.VISIBLE);
        ReconciliationPreviewDto.Summary summary = preview.summary;
        String summaryText = preview.getEntries().size() + " linhas no extrato · "
                + (summary == null || summary.high == null ? 0 : summary.high) + " correspondências fortes · "
                + (summary == null || summary.medium == null ? 0 : summary.medium) + " médias · "
                + (summary == null || summary.low == null ? 0 : summary.low) + " baixas · "
                + preview.getUnmatchedStatementIndices().size() + " sem correspondência · "
                + preview.getAlreadyImportedIndices().size() + " já importadas";
        binding.importSummaryTextView.setText(summaryText);

        if (!preview.getMatches().isEmpty()) {
            binding.importMatchesContainer.addView(sectionTitle("Correspondências encontradas"));
            for (ReconciliationPreviewDto.Match match : preview.getMatches()) addMatchRow(match);
        }
        if (!preview.getUnmatchedStatementIndices().isEmpty()) {
            binding.importUnmatchedContainer.addView(sectionTitle("Lançamentos novos"));
            for (Integer index : preview.getUnmatchedStatementIndices()) addUnmatchedRow(index);
        }
        boolean noActionableRows = preview.getMatches().isEmpty() && preview.getUnmatchedStatementIndices().isEmpty();
        binding.importEmptyTextView.setVisibility(noActionableRows ? View.VISIBLE : View.GONE);
        binding.importEmptyTextView.setText(noActionableRows
                ? "Todas as transações do arquivo já foram importadas." : "");
        binding.confirmReconciliationButton.setEnabled(!noActionableRows);
    }

    private void addMatchRow(ReconciliationPreviewDto.Match match) {
        if (match.transactionId == null || match.statementIndex < 0 || match.statementIndex >= preview.getEntries().size()) return;
        StatementEntryDto entry = preview.getEntries().get(match.statementIndex);
        View row = getLayoutInflater().inflate(R.layout.item_statement_match, binding.importMatchesContainer, false);
        CheckBox checkBox = row.findViewById(R.id.matchCheckBox);
        TextView description = row.findViewById(R.id.matchDescriptionTextView);
        TextView amount = row.findViewById(R.id.matchAmountTextView);
        TextView details = row.findViewById(R.id.matchDetailsTextView);
        TextView badge = row.findViewById(R.id.matchConfidenceBadge);

        description.setText(safe(entry.description));
        amount.setText(formatAmount(entry.amount));
        amount.setTextColor(moneyColor(entry.amount));
        String systemDescription = match.transaction == null ? "" : safe(match.transaction.description).trim();
        String score = match.score == null ? "" : " · " + match.score + "%";
        details.setText((systemDescription.isEmpty() ? "" : systemDescription + " · ")
                + confidenceLabel(match.confidence) + score);
        badge.setText(confidenceLabel(match.confidence));
        applyConfidenceBadge(badge, match.confidence);

        checkBox.setChecked(selectedMatches.contains(match.transactionId));
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            if (checked) selectedMatches.add(match.transactionId);
            else selectedMatches.remove(match.transactionId);
        });
        row.setOnClickListener(v -> checkBox.toggle());
        binding.importMatchesContainer.addView(row);
    }

    private void addUnmatchedRow(int index) {
        if (index < 0 || index >= preview.getEntries().size()) return;
        StatementEntryDto entry = preview.getEntries().get(index);
        View row = getLayoutInflater().inflate(R.layout.item_statement_new, binding.importUnmatchedContainer, false);
        CheckBox checkBox = row.findViewById(R.id.newEntryCheckBox);
        TextView description = row.findViewById(R.id.newEntryDescriptionTextView);
        TextView amount = row.findViewById(R.id.newEntryAmountTextView);
        TextView details = row.findViewById(R.id.newEntryDetailsTextView);
        Spinner spinner = row.findViewById(R.id.newEntryCategorySpinner);

        description.setText(safe(entry.description));
        amount.setText(formatAmount(entry.amount));
        amount.setTextColor(moneyColor(entry.amount));
        details.setText(entryDetails(entry));
        checkBox.setChecked(selectedUnmatched.contains(index));
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            if (checked) selectedUnmatched.add(index);
            else selectedUnmatched.remove(index);
        });

        List<CategoryOption> options = categoryOptions(index);
        ArrayAdapter<CategoryOption> adapter = new ArrayAdapter<>(this,
                R.layout.item_spinner_form, options);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        spinner.setAdapter(adapter);
        categorySpinners.put(index, spinner);

        row.setOnClickListener(v -> checkBox.toggle());
        binding.importUnmatchedContainer.addView(row);
    }

    private List<CategoryOption> categoryOptions(int index) {
        LinkedHashMap<String, CategoryOption> options = new LinkedHashMap<>();
        for (ReconciliationPreviewDto.Suggestion suggestion : suggestions(index)) {
            if (suggestion.categoryId != null) options.put(suggestion.categoryId,
                    new CategoryOption(suggestion.categoryId, suggestion.categoryName));
        }
        for (CategoryDto category : categories) {
            if (category.getId() != null) options.putIfAbsent(category.getId(),
                    new CategoryOption(category.getId(), category.getName()));
        }
        return new ArrayList<>(options.values());
    }

    private List<ReconciliationPreviewDto.Suggestion> suggestions(int index) {
        if (preview == null || preview.suggestions == null) return new ArrayList<>();
        List<ReconciliationPreviewDto.Suggestion> result = preview.suggestions.get(String.valueOf(index));
        return result == null ? new ArrayList<>() : result;
    }

    private void confirmReconciliation() {
        if (preview == null || preview.accountId == null) return;
        List<ReconciliationConfirmRequestDto.ReconcileItem> toReconcile = new ArrayList<>();
        for (ReconciliationPreviewDto.Match match : preview.getMatches()) {
            if (match.transactionId != null && selectedMatches.contains(match.transactionId)) {
                toReconcile.add(new ReconciliationConfirmRequestDto.ReconcileItem(
                        match.statementIndex, match.transactionId));
            }
        }
        List<ReconciliationConfirmRequestDto.CreateItem> toCreate = new ArrayList<>();
        for (Integer index : selectedUnmatched) {
            Spinner spinner = categorySpinners.get(index);
            CategoryOption option = spinner == null ? null : (CategoryOption) spinner.getSelectedItem();
            if (option != null) toCreate.add(new ReconciliationConfirmRequestDto.CreateItem(index, option.id));
        }
        if (toReconcile.isEmpty() && toCreate.isEmpty()) {
            showError("Selecione pelo menos uma correspondência ou lançamento novo.");
            return;
        }
        binding.confirmReconciliationButton.setEnabled(false);
        showLoading(true);
        ReconciliationConfirmRequestDto request = new ReconciliationConfirmRequestDto(
                preview.accountId, preview.getEntries(), toReconcile, toCreate);
        viewModel.confirm(request, new AccountsRepository.ResultCallback<ReconciliationConfirmResultDto>() {
            @Override public void onSuccess(@Nullable ReconciliationConfirmResultDto result) {
                showLoading(false);
                int reconciled = result == null || result.reconciled == null ? 0 : result.reconciled;
                int created = result == null || result.created == null ? 0 : result.created;
                Toast.makeText(StatementImportActivity.this,
                        reconciled + " conciliadas, " + created + " criadas.", Toast.LENGTH_LONG).show();
                finish();
            }
            @Override public void onError(@NonNull String message) {
                showLoading(false);
                showError(message);
                binding.confirmReconciliationButton.setEnabled(true);
            }
        });
    }

    private void clearPreview() {
        preview = null;
        selectedMatches.clear();
        selectedUnmatched.clear();
        categorySpinners.clear();
        binding.importMatchesContainer.removeAllViews();
        binding.importUnmatchedContainer.removeAllViews();
        binding.summaryCard.setVisibility(View.GONE);
        binding.importEmptyTextView.setVisibility(View.GONE);
        binding.confirmReconciliationButton.setEnabled(false);
    }

    private TextView sectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(getColor(R.color.on_surface));
        title.setTextSize(17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, dp(20), 0, dp(8));
        return title;
    }

    private void applyConfidenceBadge(TextView badge, @Nullable String confidence) {
        if ("HIGH".equalsIgnoreCase(confidence)) {
            badge.setBackgroundResource(R.drawable.bg_status_badge_success);
            badge.setTextColor(getColor(R.color.on_success_container));
        } else if ("MEDIUM".equalsIgnoreCase(confidence)) {
            badge.setBackgroundResource(R.drawable.bg_status_badge_neutral);
            badge.setTextColor(getColor(R.color.on_neutral_container));
        } else {
            badge.setBackgroundResource(R.drawable.bg_status_badge_error);
            badge.setTextColor(getColor(R.color.on_error_container));
        }
    }

    private String entryDetails(StatementEntryDto entry) {
        StringBuilder details = new StringBuilder();
        if (entry.date != null && !entry.date.trim().isEmpty()) {
            details.append("Data ").append(entry.date.trim());
        }
        if (entry.type != null && !entry.type.trim().isEmpty()) {
            if (details.length() > 0) details.append(" · ");
            details.append(typeLabel(entry.type));
        }
        return details.toString();
    }

    private String typeLabel(String type) {
        if ("debit".equalsIgnoreCase(type)) return "Débito";
        if ("credit".equalsIgnoreCase(type)) return "Crédito";
        return type;
    }

    private int moneyColor(double value) {
        return getColor(value < 0 ? R.color.money_negative : R.color.money_positive);
    }

    private String displayName(Uri uri) {
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Exception ignored) { }
        return uri.getLastPathSegment() == null ? "Extrato" : uri.getLastPathSegment();
    }

    private String confidenceLabel(@Nullable String confidence) {
        if ("HIGH".equalsIgnoreCase(confidence)) return "Alta";
        if ("MEDIUM".equalsIgnoreCase(confidence)) return "Média";
        return "Baixa";
    }

    private String formatAmount(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(amount);
    }

    private void showLoading(boolean loading) {
        binding.importProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.selectStatementButton.setEnabled(!loading);
    }

    private void showError(String message) {
        binding.importErrorTextView.setText(message);
        binding.importErrorTextView.setVisibility(View.VISIBLE);
    }

    private String safe(@Nullable Object value) { return value == null ? "" : String.valueOf(value); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static class CategoryOption {
        final String id;
        final String name;
        CategoryOption(String id, String name) { this.id = id; this.name = name == null ? "Categoria" : name; }
        @Override public String toString() { return name; }
    }
}
