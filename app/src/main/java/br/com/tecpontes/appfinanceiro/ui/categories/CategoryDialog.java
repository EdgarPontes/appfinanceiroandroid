package br.com.tecpontes.appfinanceiro.ui.categories;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.DialogCategoryBinding;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryRequestDto;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Diálogo de criação e edição de categorias. */
public class CategoryDialog extends Dialog {

    public interface Listener {
        /**
         * Chamado ao salvar. O diálogo permanece aberto; a tela deve chamar
         * {@link #setSaving(boolean)} e, em caso de erro, {@link #showError(String)}.
         */
        void onSave(@NonNull CategoryRequestDto request, @NonNull CategoryDialog dialog);
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("^#([0-9A-Fa-f]{6})$");
    private static final String DEFAULT_COLOR = "#ef4444";
    private static final String[] PALETTE = {
            "#ef4444", "#f59e0b", "#10b981", "#3b82f6",
            "#8b5cf6", "#06b6d4", "#ec4899", "#6b7280",
            "#14b8a6", "#84cc16", "#f97316", "#6366f1"
    };

    private DialogCategoryBinding binding;
    private final CategoryDto editing;
    private final List<CategoryDto> allCategories;
    private final String initialType;
    private final Listener listener;

    private final List<View> swatches = new ArrayList<>();
    private String selectedColor = DEFAULT_COLOR;
    private boolean refreshingParents;
    private boolean saving;
    private boolean updatingHex;

    public CategoryDialog(
            @NonNull Context context,
            @Nullable CategoryDto editing,
            @NonNull List<CategoryDto> allCategories,
            @NonNull String initialType,
            @NonNull Listener listener) {
        super(context);
        this.editing = editing;
        this.allCategories = allCategories;
        this.initialType = initialType;
        this.listener = listener;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        binding = DialogCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.categoryDialogTitleTextView.setText(editing == null
                ? R.string.categories_dialog_new_title
                : R.string.categories_dialog_edit_title);

        selectedColor = editing != null && isValidHex(editing.getColor())
                ? normalizeHex(editing.getColor())
                : DEFAULT_COLOR;

        binding.categoryNameEditText.setText(editing == null || editing.getName() == null
                ? "" : editing.getName());

        setupColorPalette();
        setupColorHexField();
        updatePreview();

        setupTypeSpinner();
        refreshParentSpinner();
        setupParentListener();
        setupListeners();
        updateSaveButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupWindow();
    }

    private void setupTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.category_type_labels,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categoryTypeSpinner.setAdapter(adapter);

        String type = editing != null && editing.getType() != null
                ? editing.getType() : initialType;
        binding.categoryTypeSpinner.setSelection(
                "INCOME".equals(type) ? 1 : 0, false);
        binding.categoryTypeSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (refreshingParents) return;
                        refreshParentSpinner();
                        hideError();
                    }

                    @Override public void onNothingSelected(AdapterView<?> parent) {
                        // Mantém a seleção atual.
                    }
                });
    }

    private void refreshParentSpinner() {
        refreshingParents = true;

        String type = selectedType();
        List<CategoryDto> choices = new ArrayList<>();

        CategoryDto none = new CategoryDto();
        none.setName(getContext().getString(R.string.categories_parent_none));
        choices.add(none);

        for (CategoryDto category : allCategories) {
            if (category.getId() == null) continue;
            if (!type.equals(category.getType())) continue;
            if (!category.isParent()) continue;
            if (editing != null && category.getId().equals(editing.getId())) continue;
            choices.add(category);
        }

        ArrayAdapter<CategoryDto> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categoryParentSpinner.setAdapter(adapter);

        int selection = 0;
        String desiredParent = editing == null ? null : editing.getParentId();
        if (desiredParent != null) {
            for (int i = 0; i < choices.size(); i++) {
                CategoryDto choice = choices.get(i);
                if (choice.getId() != null && choice.getId().equals(desiredParent)) {
                    selection = i;
                    break;
                }
            }
        }
        binding.categoryParentSpinner.setSelection(selection, false);

        refreshingParents = false;
    }

    private void setupParentListener() {
        binding.categoryParentSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (refreshingParents) return;
                        Object item = parent.getItemAtPosition(position);
                        if (item instanceof CategoryDto) {
                            CategoryDto category = (CategoryDto) item;
                            if (category.getId() != null && isValidHex(category.getColor())) {
                                setColor(category.getColor(), true);
                            }
                        }
                        hideError();
                    }

                    @Override public void onNothingSelected(AdapterView<?> parent) {
                        // Mantém a seleção atual.
                    }
                });
    }

    private void setupColorPalette() {
        binding.categoryColorPaletteContainer.removeAllViews();
        swatches.clear();

        List<String> colors = new ArrayList<>();
        for (String color : PALETTE) colors.add(color);
        if (isValidHex(selectedColor)) {
            boolean present = false;
            for (String color : colors) {
                if (color.equalsIgnoreCase(selectedColor)) {
                    present = true;
                    break;
                }
            }
            if (!present) colors.add(0, selectedColor);
        }

        for (String color : colors) {
            View swatch = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(36), dp(36));
            params.setMargins(dp(2), dp(4), dp(2), dp(4));
            swatch.setLayoutParams(params);
            swatch.setTag(color);
            swatch.setContentDescription(color);
            swatch.setOnClickListener(v -> setColor((String) v.getTag(), true));

            swatches.add(swatch);
            binding.categoryColorPaletteContainer.addView(swatch);
        }
        refreshSwatches();
    }

    private void refreshSwatches() {
        int selectedStroke = ContextCompat.getColor(getContext(), R.color.primary_purple);
        for (View swatch : swatches) {
            String color = (String) swatch.getTag();
            boolean selected = color != null && color.equalsIgnoreCase(selectedColor);

            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(parseColor(color));
            background.setStroke(dp(selected ? 3 : 1), selected ? selectedStroke : 0x22000000);
            swatch.setBackground(background);
        }
    }

    private void setupColorHexField() {
        EditText hexField = binding.categoryColorHexEditText;
        hexField.setText(selectedColor);
        hexField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable editable) {
                if (updatingHex) return;
                String value = editable.toString().trim();
                if (value.isEmpty()) return;
                String normalized = normalizeHex(value);
                if (isValidHex(normalized)) {
                    setColor(normalized, false);
                }
            }
        });
        hexField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !isValidHex(normalizeHex(hexField.getText().toString()))) {
                hexField.setText(selectedColor);
            }
        });
    }

    private void setupListeners() {
        binding.cancelCategoryButton.setOnClickListener(v -> {
            if (!saving) dismiss();
        });
        binding.saveCategoryButton.setOnClickListener(v -> save());
    }

    private void setColor(@Nullable String color, boolean syncHex) {
        String normalized = normalizeHex(color);
        if (!isValidHex(normalized)) return;

        selectedColor = normalized;
        updatePreview();
        refreshSwatches();

        if (syncHex) {
            updatingHex = true;
            binding.categoryColorHexEditText.setText(normalized);
            binding.categoryColorHexEditText.setSelection(normalized.length());
            updatingHex = false;
        }
        hideError();
    }

    private void updatePreview() {
        GradientDrawable preview = new GradientDrawable();
        preview.setShape(GradientDrawable.RECTANGLE);
        preview.setCornerRadius(dp(8));
        preview.setColor(parseColor(selectedColor));
        binding.categoryColorPreviewView.setBackground(preview);
    }

    private void save() {
        if (saving) return;

        String name = binding.categoryNameEditText.getText().toString().trim();
        if (name.isEmpty()) {
            showError(getContext().getString(R.string.categories_error_name));
            return;
        }

        String color = normalizeHex(binding.categoryColorHexEditText.getText().toString());
        if (!isValidHex(color)) {
            showError(getContext().getString(R.string.categories_error_color));
            return;
        }
        selectedColor = color;
        updatePreview();
        refreshSwatches();

        CategoryDto parent = (CategoryDto) binding.categoryParentSpinner.getSelectedItem();
        String parentId = parent == null ? null : parent.getId();

        if (editing != null && editing.getParentId() != null && parentId == null) {
            showError(getContext().getString(R.string.categories_error_clear_parent));
            return;
        }

        CategoryRequestDto request = new CategoryRequestDto(
                name, selectedType(), selectedColor, parentId);
        hideError();
        listener.onSave(request, this);
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
        if (binding == null) return;

        boolean enabled = !saving;
        binding.categoryNameEditText.setEnabled(enabled);
        binding.categoryTypeSpinner.setEnabled(enabled);
        binding.categoryParentSpinner.setEnabled(enabled);
        binding.categoryColorHexEditText.setEnabled(enabled);
        updateSaveButton();
    }

    public void showError(@Nullable String message) {
        if (binding == null) return;
        if (message == null || message.trim().isEmpty()) {
            hideError();
            return;
        }
        binding.categoryDialogErrorTextView.setText(message);
        binding.categoryDialogErrorTextView.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        if (binding == null) return;
        binding.categoryDialogErrorTextView.setVisibility(View.GONE);
    }

    private void updateSaveButton() {
        if (binding == null) return;
        binding.saveCategoryButton.setEnabled(!saving);
        if (saving) {
            binding.saveCategoryButton.setText(R.string.categories_saving);
        } else {
            binding.saveCategoryButton.setText(editing == null
                    ? R.string.categories_create
                    : R.string.categories_update);
        }
    }

    private String selectedType() {
        int position = binding.categoryTypeSpinner.getSelectedItemPosition();
        String[] values = getContext().getResources()
                .getStringArray(R.array.category_type_values);
        if (position < 0 || position >= values.length) return "EXPENSE";
        return values[position];
    }

    @NonNull
    private String normalizeHex(@Nullable String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return "";
        return trimmed.startsWith("#") ? trimmed : "#" + trimmed;
    }

    private boolean isValidHex(@Nullable String value) {
        return value != null && HEX_PATTERN.matcher(value.trim()).matches();
    }

    private int parseColor(@Nullable String color) {
        try {
            return Color.parseColor(color == null ? "#6B7280" : color);
        } catch (IllegalArgumentException ignored) {
            return Color.parseColor("#6B7280");
        }
    }

    private void setupWindow() {
        Window window = getWindow();
        if (window == null) return;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
