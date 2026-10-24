package br.com.tecpontes.appfinanceiro.ui.settings;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.ActivitySettingsBinding;
import br.com.tecpontes.appfinanceiro.databinding.DialogImportGoalsBinding;
import br.com.tecpontes.appfinanceiro.databinding.ItemCategoryGoalBinding;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryGoalDto;
import br.com.tecpontes.appfinanceiro.model.CategoryGoalRequestDto;
import br.com.tecpontes.appfinanceiro.model.ImportCategoryGoalsRequestDto;
import br.com.tecpontes.appfinanceiro.model.UserSettingsDto;
import br.com.tecpontes.appfinanceiro.model.UserSettingsUpdateRequestDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.SettingsViewModel;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;

/** Preferências financeiras, DND, retenção e metas por categoria. */
@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {

    private static final Pattern TIME_PATTERN =
            Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");
    private static final String CURRENCY = "BRL";

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;
    private final Map<String, EditText> goalFields = new HashMap<>();
    private final Map<String, String> originalGoals = new HashMap<>();

    @Inject TokenManager tokenManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        BottomNavigationHelper.setup(this, R.id.navigation_menu);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupListeners();
        setupObservers();
        SessionExpiredNavigation.observe(this, tokenManager);
        viewModel.load();
    }

    private void setupListeners() {
        binding.saveGoalsButton.setOnClickListener(v -> saveSettings(
                "Metas financeiras salvas com sucesso."));
        binding.saveDndButton.setOnClickListener(v -> saveSettings(
                "Configurações de Não Perturbe salvas com sucesso."));
        binding.saveRetentionButton.setOnClickListener(v -> saveSettings(
                "Período de retenção salvo com sucesso."));
        binding.saveCategoryGoalsButton.setOnClickListener(v -> saveCategoryGoals());
        binding.importGoalsButton.setOnClickListener(v -> showImportGoalsDialog());

        binding.dndStartEditText.setOnClickListener(v -> showTimePicker(binding.dndStartEditText));
        binding.dndEndEditText.setOnClickListener(v -> showTimePicker(binding.dndEndEditText));

        binding.dndSwitch.setOnCheckedChangeListener((button, checked) ->
                binding.dndTimesRow.setVisibility(checked ? View.VISIBLE : View.GONE));
    }

    private void setupObservers() {
        viewModel.settingsData.observe(this, this::renderSettings);
        viewModel.categoriesData.observe(this,
                categories -> renderCategoryGoals(categories, viewModel.categoryGoalsData.getValue()));
        viewModel.categoryGoalsData.observe(this, goals ->
                renderCategoryGoals(viewModel.categoriesData.getValue(), goals));

        viewModel.isLoading.observe(this, loading -> {
            binding.settingsProgressBar.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            updateActionButtons();
        });

        viewModel.isSaving.observe(this, saving -> updateActionButtons());

        viewModel.errorMessage.observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                binding.settingsErrorTextView.setVisibility(View.GONE);
            } else {
                binding.settingsErrorTextView.setText(message);
                binding.settingsErrorTextView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.successMessage.observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSettings(@Nullable UserSettingsDto settings) {
        binding.savingsGoalEditText.setText(settings == null || settings.savingsGoalPercentage == null
                ? "20" : settings.savingsGoalPercentage);
        binding.monthlyBudgetEditText.setText(settings == null || settings.monthlyBudgetGoal == null
                ? "" : settings.monthlyBudgetGoal);
        binding.expenseAlertEditText.setText(settings == null || settings.expenseAlertThreshold == null
                ? "80" : settings.expenseAlertThreshold);
        binding.currencyEditText.setText(settings == null || settings.currency == null
                ? CURRENCY : settings.currency);
        binding.retentionEditText.setText(String.valueOf(
                settings == null ? 90 : settings.retentionDays(90)));

        boolean dndActive = settings != null && settings.isNotDisturbActive();
        binding.dndSwitch.setOnCheckedChangeListener(null);
        binding.dndSwitch.setChecked(dndActive);
        binding.dndSwitch.setOnCheckedChangeListener((button, checked) ->
                binding.dndTimesRow.setVisibility(checked ? View.VISIBLE : View.GONE));
        binding.dndTimesRow.setVisibility(dndActive ? View.VISIBLE : View.GONE);

        binding.dndStartEditText.setText(settings == null || settings.notDisturbStart == null
                ? "22:00" : settings.notDisturbStart);
        binding.dndEndEditText.setText(settings == null || settings.notDisturbEnd == null
                ? "08:00" : settings.notDisturbEnd);
    }

    private void renderCategoryGoals(
            @Nullable List<CategoryDto> categories, @Nullable List<CategoryGoalDto> goals) {
        if (categories == null) categories = new ArrayList<>();
        if (goals == null) goals = new ArrayList<>();

        Map<String, String> goalByCategory = new HashMap<>();
        for (CategoryGoalDto goal : goals) {
            if (goal.categoryId != null && goal.goalAmount != null) {
                goalByCategory.put(goal.categoryId, goal.goalAmount);
            }
        }

        binding.categoryGoalsContainer.removeAllViews();
        goalFields.clear();
        originalGoals.clear();

        boolean empty = categories.isEmpty();
        binding.categoryGoalsEmptyTextView.setVisibility(empty ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (CategoryDto category : categories) {
            if (category.getId() == null) continue;

            ItemCategoryGoalBinding itemBinding = ItemCategoryGoalBinding.inflate(
                    inflater, binding.categoryGoalsContainer, false);

            itemBinding.goalCategoryName.setText(category.getName() == null ? "" : category.getName());
            if (category.getParentId() != null) {
                itemBinding.getRoot().setPadding(dp(20), itemBinding.getRoot().getPaddingTop(),
                        itemBinding.getRoot().getPaddingRight(), itemBinding.getRoot().getPaddingBottom());
            }

            try {
                String categoryColor = category.getColor();
                int tint = categoryColor == null
                        ? getColor(R.color.money_neutral) : Color.parseColor(categoryColor);
                itemBinding.goalColorView.setBackgroundTintList(ColorStateList.valueOf(tint));
            } catch (Exception ignored) {
                itemBinding.goalColorView.setBackgroundTintList(
                        ColorStateList.valueOf(getColor(R.color.money_neutral)));
            }

            String value = goalByCategory.get(category.getId());
            itemBinding.goalAmountEditText.setText(value == null ? "" : value);
            if (value != null && !value.trim().isEmpty()) {
                originalGoals.put(category.getId(), value);
            }
            goalFields.put(category.getId(), itemBinding.goalAmountEditText);

            binding.categoryGoalsContainer.addView(itemBinding.getRoot());
        }

        updateActionButtons();
    }

    private void updateActionButtons() {
        boolean idle = !Boolean.TRUE.equals(viewModel.isLoading.getValue())
                && !Boolean.TRUE.equals(viewModel.isSaving.getValue());
        binding.saveGoalsButton.setEnabled(idle);
        binding.saveDndButton.setEnabled(idle);
        binding.saveRetentionButton.setEnabled(idle);
        binding.importGoalsButton.setEnabled(idle);
        binding.saveCategoryGoalsButton.setEnabled(idle && !goalFields.isEmpty());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showTimePicker(@NonNull EditText target) {
        String current = target.getText().toString().trim();
        int hour = 22;
        int minute = 0;
        if (TIME_PATTERN.matcher(current).matches()) {
            try {
                String[] parts = current.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                // Mantém o padrão.
            }
        }

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Selecione o horário")
                .build();
        picker.addOnPositiveButtonClickListener(view -> target.setText(
                String.format(Locale.ROOT, "%02d:%02d", picker.getHour(), picker.getMinute())));
        picker.show(getSupportFragmentManager(), "settings_time_picker");
    }

    private void saveSettings(@NonNull String successMessage) {
        Double savings = parseDecimal(binding.savingsGoalEditText.getText().toString());
        if (savings == null || savings < 0 || savings > 100) {
            binding.savingsGoalEditText.setError("Informe um valor entre 0 e 100.");
            return;
        }

        String budgetRaw = binding.monthlyBudgetEditText.getText().toString().trim();
        Double budget = budgetRaw.isEmpty() ? null : parseDecimal(budgetRaw);
        if (!budgetRaw.isEmpty() && (budget == null || budget < 0)) {
            binding.monthlyBudgetEditText.setError("Informe um valor maior ou igual a zero.");
            return;
        }

        Double alert = parseDecimal(binding.expenseAlertEditText.getText().toString());
        if (alert == null || alert < 0 || alert > 100) {
            binding.expenseAlertEditText.setError("Informe um valor entre 0 e 100.");
            return;
        }

        Integer retention = parseInteger(binding.retentionEditText.getText().toString());
        if (retention == null || retention < 7 || retention > 365) {
            binding.retentionEditText.setError("Informe um valor entre 7 e 365 dias.");
            return;
        }

        String start = binding.dndStartEditText.getText().toString().trim();
        String end = binding.dndEndEditText.getText().toString().trim();
        if (!TIME_PATTERN.matcher(start).matches()) {
            binding.dndStartEditText.setError("Use o formato HH:mm.");
            return;
        }
        if (!TIME_PATTERN.matcher(end).matches()) {
            binding.dndEndEditText.setError("Use o formato HH:mm.");
            return;
        }

        UserSettingsUpdateRequestDto request = new UserSettingsUpdateRequestDto();
        request.savingsGoalPercentage = formatAmount(savings);
        request.monthlyBudgetGoal = budget == null ? null : formatAmount(budget);
        request.expenseAlertThreshold = formatAmount(alert);
        request.currency = CURRENCY;
        request.isNotDisturbActive = binding.dndSwitch.isChecked();
        request.notDisturbStart = start;
        request.notDisturbEnd = end;
        request.messageRetentionDays = retention;

        viewModel.saveSettings(request, successMessage);
    }

    private void saveCategoryGoals() {
        LocalDate now = LocalDate.now();
        List<CategoryGoalRequestDto> goals = new ArrayList<>();

        for (Map.Entry<String, EditText> entry : goalFields.entrySet()) {
            String raw = entry.getValue().getText().toString().trim();
            if (raw.isEmpty()) {
                // Campo limpo: zera uma meta que já existia (mesmo comportamento do app web).
                if (originalGoals.containsKey(entry.getKey())) {
                    goals.add(new CategoryGoalRequestDto(entry.getKey(),
                            now.getMonthValue(), now.getYear(), "0.00"));
                }
                continue;
            }

            Double value = parseDecimal(raw);
            if (value == null || value < 0) {
                entry.getValue().setError("Valor inválido.");
                return;
            }
            goals.add(new CategoryGoalRequestDto(
                    entry.getKey(), now.getMonthValue(), now.getYear(), formatAmount(value)));
        }

        if (goals.isEmpty()) {
            Toast.makeText(this, "Nada para salvar. Informe pelo menos uma meta maior ou igual a zero.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        viewModel.saveGoals(goals);
    }

    private void showImportGoalsDialog() {
        DialogImportGoalsBinding dialogBinding =
                DialogImportGoalsBinding.inflate(getLayoutInflater());

        String[] months = getResources().getStringArray(R.array.month_values);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.importMonthSpinner.setAdapter(adapter);

        LocalDate now = LocalDate.now();
        dialogBinding.importMonthSpinner.setSelection(now.getMonthValue() - 1);
        dialogBinding.importYearEditText.setText(String.valueOf(now.getYear()));

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCanceledOnTouchOutside(true);

        dialogBinding.cancelImportButton.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.confirmImportButton.setOnClickListener(v -> {
            Integer year = parseInteger(dialogBinding.importYearEditText.getText().toString());
            if (year == null || year < 2000 || year > 2100) {
                dialogBinding.importYearEditText.setError("Informe um ano entre 2000 e 2100.");
                return;
            }
            int month = dialogBinding.importMonthSpinner.getSelectedItemPosition() + 1;
            viewModel.importGoals(new ImportCategoryGoalsRequestDto(
                    month, year, now.getMonthValue(), now.getYear()));
            dialog.dismiss();
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    private Double parseDecimal(@Nullable String value) {
        if (value == null) return null;
        String normalized = value.trim().replace(',', '.');
        if (normalized.isEmpty()) return null;
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private Integer parseInteger(@Nullable String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String formatAmount(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
