package br.com.tecpontes.appfinanceiro.ui.transactions;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.databinding.DialogCreateTransactionBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * Dialog para criação de nova transação
 */
public class CreateTransactionDialog extends Dialog {

    private DialogCreateTransactionBinding binding;
    private final OnTransactionCreatedListener listener;

    // Categorias pré-definidas
    private final String[] expenseCategories = {
        "Alimentação", "Transporte", "Lazer", "Saúde", "Educação",
        "Casa", "Compras", "Outros"
    };

    private final String[] incomeCategories = {
        "Salário", "Freelance", "Investimentos", "Outros"
    };

    public CreateTransactionDialog(@NonNull Context context, @NonNull OnTransactionCreatedListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configura janela do dialog
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Infla layout
        binding = DialogCreateTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configura componentes
        setupComponents();

        // Configura listeners
        setupListeners();
    }

    private void setupComponents() {
        // Configura chips de categoria para despesas
        setupCategoryChips(expenseCategories, binding.expenseCategoriesChipGroup);

        // Configura chips de categoria para receitas
        setupCategoryChips(incomeCategories, binding.incomeCategoriesChipGroup);

        // Mostra apenas despesas por padrão
        binding.expenseCategoriesChipGroup.setVisibility(View.VISIBLE);
        binding.incomeCategoriesChipGroup.setVisibility(View.GONE);
    }

    private void setupCategoryChips(String[] categories, ChipGroup chipGroup) {
        for (String category : categories) {
            Chip chip = new Chip(getContext());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipStrokeColorResource(br.com.tecpontes.appfinanceiro.R.color.primary_purple);
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(getContext().getColor(br.com.tecpontes.appfinanceiro.R.color.primary_purple));

            chipGroup.addView(chip);
        }
    }

    private void setupListeners() {
        // Tipo de transação (receita/despesa)
        binding.typeChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.size() > 0) {
                int checkedId = checkedIds.get(0);
                if (checkedId == br.com.tecpontes.appfinanceiro.R.id.expenseChip) {
                    binding.expenseCategoriesChipGroup.setVisibility(View.VISIBLE);
                    binding.incomeCategoriesChipGroup.setVisibility(View.GONE);
                } else if (checkedId == br.com.tecpontes.appfinanceiro.R.id.incomeChip) {
                    binding.expenseCategoriesChipGroup.setVisibility(View.GONE);
                    binding.incomeCategoriesChipGroup.setVisibility(View.VISIBLE);
                }
            }
        });

        // Botão de salvar
        binding.saveButton.setOnClickListener(v -> {
            if (validateAndSave()) {
                dismiss();
            }
        });

        // Botão de cancelar
        binding.cancelButton.setOnClickListener(v -> {
            dismiss();
        });

        // Formatação do campo de valor
        binding.amountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remove formatação para edição
                String cleanString = s.toString().replaceAll("[R$,.\\s]", "");
                if (!cleanString.isEmpty()) {
                    try {
                        double amount = Double.parseDouble(cleanString) / 100.0;
                        binding.amountEditText.removeTextChangedListener(this);
                        binding.amountEditText.setText(String.format("%.2f", amount));
                        binding.amountEditText.setSelection(binding.amountEditText.getText().length());
                        binding.amountEditText.addTextChangedListener(this);
                    } catch (NumberFormatException e) {
                        // Ignora erro de formatação
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean validateAndSave() {
        // Valida valor
        String amountText = binding.amountEditText.getText().toString().trim();
        if (amountText.isEmpty()) {
            Toast.makeText(getContext(), "Digite o valor", Toast.LENGTH_SHORT).show();
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Valor inválido", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Valida categoria
        String category = getSelectedCategory();
        if (category == null) {
            Toast.makeText(getContext(), "Selecione uma categoria", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Obtém nota
        String note = binding.noteEditText.getText().toString().trim();

        // Determina tipo
        String type = binding.expenseCategoriesChipGroup.getVisibility() == View.VISIBLE ? "expense" : "income";

        // Chama listener
        if (listener != null) {
            listener.onTransactionCreated(amount, category, note, type);
        }

        return true;
    }

    private String getSelectedCategory() {
        ChipGroup visibleChipGroup = binding.expenseCategoriesChipGroup.getVisibility() == View.VISIBLE ?
            binding.expenseCategoriesChipGroup : binding.incomeCategoriesChipGroup;

        int checkedChipId = visibleChipGroup.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip checkedChip = findViewById(checkedChipId);
            return checkedChip.getText().toString();
        }

        return null;
    }

    /**
     * Interface para callback de criação de transação
     */
    public interface OnTransactionCreatedListener {
        void onTransactionCreated(double amount, String category, String note, String type);
    }
}
