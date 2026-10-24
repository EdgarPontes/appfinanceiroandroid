package br.com.tecpontes.appfinanceiro.ui.transactions;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.DialogCreateTransactionBinding;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.model.TransactionTransferRequestDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import com.google.android.material.textfield.TextInputEditText;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Formulário de criação/edição de lançamento, correspondente às opções do modal web. */
public class CreateTransactionDialog extends Dialog {

    @Nullable private final OnTransactionCreatedListener listener;
    @Nullable private final OnTransactionEditedListener editListener;
    @Nullable private final TransactionListItemDto editingItem;
    private final boolean cloneMode;
    private DialogCreateTransactionBinding binding;
    private final Calendar selectedDate = Calendar.getInstance();
    private final List<AccountDto> accounts = new ArrayList<>();
    private final List<CreditCardDto> cards = new ArrayList<>();
    private final List<CategoryDto> categories = new ArrayList<>();
    private List<CategoryOption> visibleCategories = new ArrayList<>();
    private boolean saving;
    private boolean optionsLoading = true;
    private boolean formattingAmount;
    private boolean prefillApplied;
    @Nullable private String presetCardId;
    private boolean lockPaymentMethodToCard;

    public CreateTransactionDialog(
            @NonNull Context context,
            @NonNull OnTransactionCreatedListener listener) {
        super(context);
        this.listener = listener;
        this.editListener = null;
        this.editingItem = null;
        this.cloneMode = false;
    }

    public CreateTransactionDialog(
            @NonNull Context context,
            @NonNull TransactionListItemDto editingItem,
            @NonNull OnTransactionEditedListener editListener) {
        super(context);
        this.listener = null;
        this.editListener = editListener;
        this.editingItem = editingItem;
        this.cloneMode = false;
    }

    public CreateTransactionDialog(
            @NonNull Context context,
            @NonNull TransactionListItemDto sourceItem,
            boolean cloneMode,
            @NonNull OnTransactionCreatedListener listener) {
        super(context);
        this.listener = listener;
        this.editListener = null;
        this.editingItem = sourceItem;
        this.cloneMode = cloneMode;
    }

    /** Fixa o lançamento como despesa no cartão informado. */
    @NonNull
    public CreateTransactionDialog presetCreditCard(@Nullable String cardId) {
        this.presetCardId = cardId;
        this.lockPaymentMethodToCard = cardId != null;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        binding = DialogCreateTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupSpinners();
        setupListeners();
        updateDateButton();
        updateFormForType();
        renderLoadingState();
        if (isCloneMode()) {
            binding.formTitleTextView.setText("Clonar lançamento");
            binding.formSubtitleTextView.setText("Revise os dados e crie uma nova cópia");
            binding.transferTypeButton.setVisibility(View.GONE);
            binding.saveButton.setText("Criar");
        } else if (isEditMode()) {
            binding.formTitleTextView.setText("Editar lançamento");
            binding.formSubtitleTextView.setText("Atualize os dados do lançamento");
            binding.transferTypeButton.setVisibility(View.GONE);
        }
        applyPresetCard();
    }

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        if (window == null) return;
        int width = getContext().getResources().getDisplayMetrics().widthPixels - dp(32);
        int height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.88f);
        window.setLayout(width, height);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void setOptions(
            @Nullable List<AccountDto> loadedAccounts,
            @Nullable List<CategoryDto> loadedCategories,
            @Nullable List<CreditCardDto> loadedCards) {
        accounts.clear();
        cards.clear();
        categories.clear();
        if (loadedAccounts != null) {
            for (AccountDto account : loadedAccounts) {
                if (account == null || account.getId() == null) continue;
                if (hasSourceItem() || account.isActive()) accounts.add(account);
            }
        }
        if (loadedCategories != null) {
            for (CategoryDto category : loadedCategories) {
                if (category != null) categories.add(category);
            }
        }
        if (loadedCards != null) {
            for (CreditCardDto card : loadedCards) {
                if (card == null || card.getId() == null) continue;
                if (hasSourceItem() || card.isActive()) cards.add(card);
            }
        }
        setSpinnerItems(binding.accountSpinner, namesOfAccounts(), "Selecione uma conta");
        setSpinnerItems(binding.cardSpinner, namesOfCards(), "Selecione um cartão");
        setSpinnerItems(binding.fromAccountSpinner, namesOfAccounts(), "Selecione a conta de origem");
        setSpinnerItems(binding.toAccountSpinner, namesOfAccounts(), "Selecione a conta de destino");
        refreshCategoryOptions();
        renderLoadingState();
        prefillIfReady();
        applyPresetCard();
    }

    public void setOptionsLoading(boolean loading) {
        optionsLoading = loading;
        renderLoadingState();
        if (!loading) prefillIfReady();
    }

    private boolean hasSourceItem() {
        return editingItem != null;
    }

    private boolean isEditMode() {
        return editingItem != null && !cloneMode;
    }

    private boolean isCloneMode() {
        return editingItem != null && cloneMode;
    }

    private void prefillIfReady() {
        if (!hasSourceItem() || prefillApplied || binding == null || optionsLoading) return;
        prefillApplied = true;
        applyEditingItem(editingItem);
    }

    private void applyPresetCard() {
        if (!lockPaymentMethodToCard || binding == null) return;
        if (binding.typeChipGroup.getCheckedButtonId() == View.NO_ID) {
            binding.typeChipGroup.check(R.id.expenseTypeButton);
        }
        binding.transferTypeButton.setVisibility(View.GONE);
        binding.paymentMethodSpinner.setSelection(1, false);
        binding.paymentMethodSpinner.setEnabled(false);
        binding.paymentMethodSpinner.setAlpha(0.7f);
        updatePaymentMethodVisibility();
        if (presetCardId != null) selectCardById(presetCardId);
        binding.formTitleTextView.setText("Novo lançamento");
        binding.formSubtitleTextView.setText("Despesa ou crédito no cartão de crédito");
        renderLoadingState();
    }

    private void applyEditingItem(@NonNull TransactionListItemDto item) {
        String type = item.type == null ? "EXPENSE" : item.type.toUpperCase(Locale.US);
        if ("TRANSFER".equals(type)) {
            binding.typeChipGroup.check(R.id.transferTypeButton);
        } else if ("INCOME".equals(type) || "INITIAL_BALANCE".equals(type)) {
            binding.typeChipGroup.check(R.id.incomeTypeButton);
        } else {
            binding.typeChipGroup.check(R.id.expenseTypeButton);
        }

        binding.descriptionEditText.setText(item.description == null ? "" : item.description);
        binding.noteEditText.setText(item.note == null ? "" : item.note);

        if (item.getDateMillis() > 0) {
            selectedDate.setTimeInMillis(item.getDateMillis());
            selectedDate.set(Calendar.HOUR_OF_DAY, 12);
            selectedDate.set(Calendar.MINUTE, 0);
            selectedDate.set(Calendar.SECOND, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);
            updateDateButton();
        }

        boolean useCard = item.cardId != null;
        binding.paymentMethodSpinner.setSelection(useCard ? 1 : 0, false);
        updatePaymentMethodVisibility();
        if (useCard) {
            selectCardById(item.cardId);
        } else {
            selectAccountFromSpinner(binding.accountSpinner, item.accountId);
        }

        if (!isTransfer()) {
            selectCategoryById(item.categoryId);
        }

        boolean installment = Boolean.TRUE.equals(item.isInstallment)
                || item.installmentGroupId != null;
        boolean fixedExpense = Boolean.TRUE.equals(item.isFixedExpense)
                || item.fixedExpenseGroupId != null;
        if (installment) {
            binding.repetitionToggleGroup.check(R.id.repetitionInstallmentButton);
            binding.installmentCountEditText.setText(
                    item.totalInstallments == null ? "" : String.valueOf(item.totalInstallments));
            boolean totalValue = "total".equalsIgnoreCase(item.installmentValueType);
            binding.installmentValueTypeSpinner.setSelection(totalValue ? 1 : 0, false);
            String amountText = item.amount;
            if (totalValue) {
                amountText = item.totalValue;
                String normalizedAmount = normalizeAmount(item.amount);
                if (amountText == null && normalizedAmount != null && item.totalInstallments != null) {
                    try {
                        amountText = new BigDecimal(normalizedAmount)
                                .multiply(BigDecimal.valueOf(item.totalInstallments))
                                .setScale(2, RoundingMode.HALF_UP)
                                .toPlainString();
                    } catch (NumberFormatException ignored) {
                        amountText = normalizedAmount;
                    }
                }
            }
            setAmountValue(amountText);
        } else if (fixedExpense) {
            binding.repetitionToggleGroup.check(R.id.repetitionMonthlyButton);
            setAmountValue(item.amount);
        } else {
            binding.repetitionToggleGroup.check(R.id.repetitionOnceButton);
            setAmountValue(item.amount);
        }

        if (isTransfer()) {
            selectAccountFromSpinner(binding.fromAccountSpinner, item.accountId);
        }

        updateDateLabel();
        renderLoadingState();
    }

    private void selectAccountFromSpinner(Spinner spinner, @Nullable String accountId) {
        if (accountId == null) return;
        for (int i = 0; i < accounts.size(); i++) {
            if (accountId.equals(accounts.get(i).getId())) {
                spinner.setSelection(i + 1, false);
                return;
            }
        }
    }

    private void selectCardById(@Nullable String cardId) {
        if (cardId == null) return;
        for (int i = 0; i < cards.size(); i++) {
            if (cardId.equals(cards.get(i).getId())) {
                binding.cardSpinner.setSelection(i + 1, false);
                return;
            }
        }
    }

    private void selectCategoryById(@Nullable String categoryId) {
        if (categoryId == null) return;
        for (int i = 0; i < visibleCategories.size(); i++) {
            CategoryOption option = visibleCategories.get(i);
            if (categoryId.equals(option.id)) {
                binding.categorySpinner.setSelection(i, false);
                return;
            }
        }
    }

    private void setAmountValue(@Nullable String normalizedAmount) {
        String normalized = normalizeAmount(normalizedAmount);
        if (normalized == null) return;
        String digits = normalized.replaceAll("\\D", "");
        if (digits.isEmpty()) return;
        String formatted = formatCurrencyDigits(digits);
        formattingAmount = true;
        binding.amountEditText.setText(formatted);
        binding.amountEditText.setSelection(formatted.length());
        formattingAmount = false;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
        if (binding == null) return;
        binding.saveButton.setEnabled(!saving && !optionsLoading && canSubmitWithOptions());
        if (isCloneMode()) {
            binding.saveButton.setText(saving ? "Criando..." : "Criar");
        } else {
            binding.saveButton.setText(saving ? "Salvando..." : "Salvar");
        }
        binding.cancelButton.setEnabled(!saving);
        setCancelable(!saving);
        setCanceledOnTouchOutside(!saving);
    }

    public void showError(@NonNull String message) {
        binding.errorTextView.setText(message);
        binding.errorTextView.setVisibility(View.VISIBLE);
        setSaving(false);
    }

    private void setupSpinners() {
        setSpinnerItems(binding.paymentMethodSpinner,
                new String[]{"Conta bancária", "Cartão de crédito"}, null);
        setSpinnerItems(binding.installmentValueTypeSpinner,
                new String[]{"Por parcela", "Total"}, null);
        binding.typeChipGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) updateFormForType();
        });
        binding.repetitionToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) updateRepetitionVisibility();
        });
        binding.paymentMethodSpinner.setOnItemSelectedListener(new SimpleSelectionListener(() -> {
            updatePaymentMethodVisibility();
            clearError();
            renderLoadingState();
        }));
        updateRepetitionVisibility();
    }

    private void setupListeners() {
        setupAmountFormatter();
        binding.closeButton.setOnClickListener(v -> dismiss());
        View.OnClickListener dateClickListener = v -> new DatePickerDialog(
                getContext(),
                (picker, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth, 12, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);
                    updateDateButton();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        binding.dateEditText.setOnClickListener(dateClickListener);
        binding.dateLayout.setOnClickListener(dateClickListener);
        binding.cancelButton.setOnClickListener(v -> dismiss());
        binding.saveButton.setOnClickListener(v -> submit());
    }

    private void setupAmountFormatter() {
        binding.amountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (formattingAmount) return;
                String digits = s.toString().replaceAll("\\D", "");
                if (digits.isEmpty()) {
                    if (s.length() > 0) {
                        formattingAmount = true;
                        binding.amountEditText.setText("");
                        formattingAmount = false;
                    }
                    return;
                }
                if (digits.length() > 15) digits = digits.substring(0, 15);
                String formatted = formatCurrencyDigits(digits);
                if (formatted.equals(s.toString())) return;
                formattingAmount = true;
                binding.amountEditText.setText(formatted);
                binding.amountEditText.setSelection(formatted.length());
                formattingAmount = false;
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private static String formatCurrencyDigits(String digits) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(true);
        return "R$ " + format.format(new BigDecimal(digits).movePointLeft(2));
    }

    private void updateFormForType() {
        if (binding == null) return;
        boolean transfer = isTransfer();
        binding.paymentMethodContainer.setVisibility(transfer ? View.GONE : View.VISIBLE);
        binding.transferAccountsContainer.setVisibility(transfer ? View.VISIBLE : View.GONE);
        binding.categoryContainer.setVisibility(transfer ? View.GONE : View.VISIBLE);
        binding.repetitionContainer.setVisibility(transfer ? View.GONE : View.VISIBLE);
        binding.noteLayout.setVisibility(transfer ? View.GONE : View.VISIBLE);
        updateDateLabel();
        refreshCategoryOptions();
        clearError();
        renderLoadingState();
    }

    private void updateDateLabel() {
        if (binding == null) return;
        if (isTransfer()) {
            binding.dateLabelTextView.setText("Data");
        } else if ("INCOME".equals(getTransactionType())) {
            binding.dateLabelTextView.setText("Recebimento");
        } else {
            binding.dateLabelTextView.setText("Vencimento");
        }
    }

    private void updatePaymentMethodVisibility() {
        boolean useCard = binding.paymentMethodSpinner.getSelectedItemPosition() == 1;
        binding.accountSpinner.setVisibility(useCard ? View.GONE : View.VISIBLE);
        binding.cardSpinner.setVisibility(useCard ? View.VISIBLE : View.GONE);
    }

    private void updateRepetitionVisibility() {
        boolean installment = isInstallment();
        binding.installmentRow.setVisibility(installment ? View.VISIBLE : View.GONE);
        binding.installmentCountEditText.setVisibility(installment ? View.VISIBLE : View.GONE);
        binding.installmentValueTypeSpinner.setVisibility(installment ? View.VISIBLE : View.GONE);
    }

    private void refreshCategoryOptions() {
        if (binding == null || isTransfer()) return;
        String type = getTransactionType();
        visibleCategories = new ArrayList<>();
        visibleCategories.add(new CategoryOption(null, "Selecione uma categoria"));
        for (CategoryDto category : categories) {
            if (type.equalsIgnoreCase(category.getType())) {
                addCategoryAndChildren(category, "");
            }
        }
        ArrayAdapter<CategoryOption> adapter = new ArrayAdapter<>(getContext(),
                R.layout.item_spinner_form, visibleCategories);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        binding.categorySpinner.setAdapter(adapter);
    }

    private void addCategoryAndChildren(CategoryDto category, String prefix) {
        if (category.getId() != null) {
            String label = prefix.isEmpty() ? category.getName() : prefix + " › " + category.getName();
            visibleCategories.add(new CategoryOption(category.getId(), label == null ? "Categoria" : label));
        }
        for (CategoryDto child : category.getSubCategories()) {
            addCategoryAndChildren(child, category.getName() == null ? prefix : category.getName());
        }
    }

    private void submit() {
        if (saving || optionsLoading) return;
        clearError();
        String description = textOf(binding.descriptionEditText).trim();
        String normalizedAmount = normalizeAmount(textOf(binding.amountEditText));
        if (description.isEmpty()) {
            showError("Informe a descrição do lançamento.");
            binding.descriptionEditText.requestFocus();
            return;
        }
        if (normalizedAmount == null || parseAmount(normalizedAmount) <= 0) {
            showError("Informe um valor válido maior que zero.");
            binding.amountEditText.requestFocus();
            return;
        }

        if (isTransfer()) {
            submitTransfer(description, normalizedAmount);
            return;
        }

        CategoryOption category = selectedCategory();
        if (category == null || category.id == null) {
            showError("Selecione uma categoria.");
            return;
        }
        boolean useCard = binding.paymentMethodSpinner.getSelectedItemPosition() == 1;
        AccountDto account = selectedAccount(binding.accountSpinner);
        CreditCardDto card = selectedCard();
        if ((useCard && card == null) || (!useCard && account == null)) {
            showError(useCard ? "Selecione um cartão de crédito." : "Selecione uma conta bancária.");
            return;
        }

        TransactionWriteDto request = new TransactionWriteDto();
        request.description = description;
        request.amount = normalizedAmount;
        request.date = selectedDateIso();
        request.type = getTransactionType();
        request.categoryId = category.id;
        request.accountId = useCard ? null : account.getId();
        request.cardId = useCard ? card.getId() : null;
        String note = textOf(binding.noteEditText).trim();
        request.note = note.isEmpty() ? null : note;
        request.isInstallment = isInstallment();
        request.isFixedExpense = isFixedExpense();
        if (Boolean.TRUE.equals(request.isInstallment)) {
            String countText = textOf(binding.installmentCountEditText).trim();
            int count;
            try {
                count = Integer.parseInt(countText);
            } catch (NumberFormatException error) {
                showError("Informe o número de parcelas.");
                binding.installmentCountEditText.requestFocus();
                return;
            }
            if (count < 2 || count > 120) {
                showError("O número de parcelas deve estar entre 2 e 120.");
                binding.installmentCountEditText.requestFocus();
                return;
            }
            request.totalInstallments = count;
            boolean totalValue = binding.installmentValueTypeSpinner.getSelectedItemPosition() == 1;
            request.installmentValueType = totalValue ? "total" : "installment";
            if (totalValue) {
                request.totalValue = normalizedAmount;
                request.amount = String.format(Locale.US, "%.2f", parseAmount(normalizedAmount) / count);
            } else {
                request.totalValue = String.format(Locale.US, "%.2f", parseAmount(normalizedAmount) * count);
            }
        }
        if (isCloneMode()) {
            request.installmentGroupId = null;
            request.fixedExpenseGroupId = null;
            request.updateScope = null;
            if (listener != null) listener.onCreateTransaction(request);
        } else if (isEditMode()) {
            TransactionListItemDto item = editingItem;
            if (item != null) {
                request.installmentGroupId = item.installmentGroupId;
                request.fixedExpenseGroupId = item.fixedExpenseGroupId;
            }
            if (editListener != null) editListener.onUpdateTransaction(request);
        } else if (listener != null) {
            listener.onCreateTransaction(request);
        }
    }

    private void submitTransfer(String description, String amount) {
        AccountDto from = selectedAccount(binding.fromAccountSpinner);
        AccountDto to = selectedAccount(binding.toAccountSpinner);
        if (from == null || to == null) {
            showError("Selecione as contas de origem e destino.");
            return;
        }
        if (from.getId().equals(to.getId())) {
            showError("As contas de origem e destino devem ser diferentes.");
            return;
        }
        TransactionTransferRequestDto transfer = new TransactionTransferRequestDto();
        transfer.description = description;
        transfer.amount = amount;
        transfer.date = selectedDateIso();
        transfer.fromAccountId = from.getId();
        transfer.toAccountId = to.getId();
        if (hasSourceItem()) {
            showError("Transferências não estão disponíveis neste formulário.");
            return;
        }
        if (listener != null) listener.onCreateTransfer(transfer);
    }

    private void renderLoadingState() {
        if (binding == null) return;
        binding.loadingTextView.setVisibility(optionsLoading ? View.VISIBLE : View.GONE);
        boolean cardPayment = binding.paymentMethodSpinner.getSelectedItemPosition() == 1;
        boolean requiredOptionsMissing = categories.isEmpty()
                || (cardPayment ? cards.isEmpty() : accounts.isEmpty());
        if (!optionsLoading && requiredOptionsMissing && !isTransfer()) {
            String action = isEditMode() ? "editar" : "criar";
            String message;
            if (categories.isEmpty()) {
                message = "Cadastre ao menos uma categoria para " + action + " lançamentos.";
            } else if (cardPayment) {
                message = "Cadastre um cartão de crédito para " + action + " lançamentos.";
            } else {
                message = "Cadastre uma conta bancária para " + action + " lançamentos.";
            }
            binding.errorTextView.setText(message);
            binding.errorTextView.setVisibility(View.VISIBLE);
        } else if (!optionsLoading && accounts.size() < 2 && isTransfer()) {
            binding.errorTextView.setText("São necessárias duas contas para fazer uma transferência.");
            binding.errorTextView.setVisibility(View.VISIBLE);
        }
        binding.saveButton.setEnabled(!optionsLoading && !saving && canSubmitWithOptions());
    }

    private boolean canSubmitWithOptions() {
        if (isTransfer()) return accounts.size() >= 2;
        boolean paymentOptionAvailable = binding.paymentMethodSpinner.getSelectedItemPosition() == 1
                ? !cards.isEmpty() : !accounts.isEmpty();
        return !categories.isEmpty() && paymentOptionAvailable;
    }

    private String[] namesOfAccounts() {
        String[] result = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) result[i] = accounts.get(i).getName();
        return result;
    }

    private String[] namesOfCards() {
        String[] result = new String[cards.size()];
        for (int i = 0; i < cards.size(); i++) result[i] = cards.get(i).toString();
        return result;
    }

    private void setSpinnerItems(Spinner spinner, String[] values, @Nullable String emptyLabel) {
        List<String> entries = new ArrayList<>();
        if (emptyLabel != null) entries.add(emptyLabel);
        if (values != null) {
            for (String value : values) if (value != null) entries.add(value);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.item_spinner_form, entries);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        spinner.setAdapter(adapter);
    }

    @Nullable
    private AccountDto selectedAccount(Spinner spinner) {
        int position = spinner.getSelectedItemPosition();
        if (position <= 0 || position > accounts.size()) return null;
        return accounts.get(position - 1);
    }

    @Nullable
    private CreditCardDto selectedCard() {
        int position = binding.cardSpinner.getSelectedItemPosition();
        if (position <= 0 || position > cards.size()) return null;
        return cards.get(position - 1);
    }

    @Nullable
    private CategoryOption selectedCategory() {
        Object selected = binding.categorySpinner.getSelectedItem();
        return selected instanceof CategoryOption ? (CategoryOption) selected : null;
    }

    private String getTransactionType() {
        int checkedId = binding.typeChipGroup.getCheckedButtonId();
        if (checkedId == R.id.incomeTypeButton) return "INCOME";
        if (checkedId == R.id.transferTypeButton) return "TRANSFER";
        return "EXPENSE";
    }

    private boolean isTransfer() {
        return binding != null && binding.typeChipGroup.getCheckedButtonId() == R.id.transferTypeButton;
    }

    private boolean isInstallment() {
        return binding != null
                && binding.repetitionToggleGroup.getCheckedButtonId() == R.id.repetitionInstallmentButton;
    }

    private boolean isFixedExpense() {
        return binding != null
                && binding.repetitionToggleGroup.getCheckedButtonId() == R.id.repetitionMonthlyButton;
    }

    private String selectedDateIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
                .format(selectedDate.getTime());
    }

    private void updateDateButton() {
        binding.dateEditText.setText(new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"))
                .format(selectedDate.getTime()));
    }

    private void clearError() {
        if (binding != null) binding.errorTextView.setVisibility(View.GONE);
    }

    private static String textOf(@Nullable TextInputEditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString();
    }

    @Nullable
    private static String normalizeAmount(String raw) {
        String value = raw == null ? "" : raw.trim().replace("R$", "").replace(" ", "");
        if (value.isEmpty()) return null;
        if (value.contains(",")) value = value.replace(".", "").replace(',', '.');
        try {
            return String.format(Locale.US, "%.2f", Double.parseDouble(value));
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private static double parseAmount(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    public interface OnTransactionCreatedListener {
        void onCreateTransaction(@NonNull TransactionWriteDto transaction);
        void onCreateTransfer(@NonNull TransactionTransferRequestDto transfer);
    }

    public interface OnTransactionEditedListener {
        void onUpdateTransaction(@NonNull TransactionWriteDto transaction);
    }

    private static final class CategoryOption {
        @Nullable final String id;
        @NonNull final String label;
        CategoryOption(@Nullable String id, @NonNull String label) {
            this.id = id;
            this.label = label;
        }
        @NonNull @Override public String toString() { return label; }
    }

    private static final class SimpleSelectionListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable callback;
        SimpleSelectionListener(Runnable callback) { this.callback = callback; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            callback.run();
        }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }
}
