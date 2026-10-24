package br.com.tecpontes.appfinanceiro.ui.notifications;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.DialogEditMessageConfigBinding;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;

/** Diálogo de edição de título, texto e prioridade de um template de notificação. */
public class EditMessageConfigDialog extends Dialog {

    public interface Listener {
        void onSave(@NonNull MessageConfigDto body);
    }

    private DialogEditMessageConfigBinding binding;
    private final MessageConfigDto template;
    private final Listener listener;

    public EditMessageConfigDialog(
            @NonNull Context context,
            @NonNull MessageConfigDto template,
            @NonNull Listener listener) {
        super(context);
        this.template = template;
        this.listener = listener;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        binding = DialogEditMessageConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.configTitleEditText.setText(template.title == null ? "" : template.title);
        binding.configTextEditText.setText(
                template.defaultText == null ? "" : template.defaultText);

        setupPriority();
        setupPlaceholders();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupWindow();
    }

    private void setupPriority() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.notification_priority_labels,
                R.layout.item_spinner_form);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        binding.configPrioritySpinner.setAdapter(adapter);

        String[] values = getContext().getResources()
                .getStringArray(R.array.notification_priority_values);
        int selected = 1; // medium
        if (template.priority != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(template.priority)) {
                    selected = i;
                    break;
                }
            }
        }
        binding.configPrioritySpinner.setSelection(selected);
    }

    private void setupPlaceholders() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.notification_placeholders,
                R.layout.item_spinner_form);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        binding.placeholderSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.cancelConfigButton.setOnClickListener(v -> dismiss());
        binding.insertPlaceholderButton.setOnClickListener(v -> insertPlaceholder());
        binding.saveConfigButton.setOnClickListener(v -> save());
    }

    private void insertPlaceholder() {
        String[] values = getContext().getResources()
                .getStringArray(R.array.notification_placeholder_values);
        int position = binding.placeholderSpinner.getSelectedItemPosition();
        if (position < 0 || position >= values.length) return;

        int start = Math.max(binding.configTextEditText.getSelectionStart(), 0);
        binding.configTextEditText.getText().insert(start, values[position]);
    }

    private void save() {
        String title = binding.configTitleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            binding.configTitleEditText.setError(
                    getContext().getString(R.string.notification_required_title));
            return;
        }
        String text = binding.configTextEditText.getText().toString().trim();
        if (text.isEmpty()) {
            binding.configTextEditText.setError(
                    getContext().getString(R.string.notification_required_text));
            return;
        }

        MessageConfigDto body = new MessageConfigDto();
        body.title = title;
        body.defaultText = text;
        body.priority = priorityValue();

        listener.onSave(body);
        dismiss();
    }

    @NonNull
    private String priorityValue() {
        String[] values = getContext().getResources()
                .getStringArray(R.array.notification_priority_values);
        int position = binding.configPrioritySpinner.getSelectedItemPosition();
        if (position < 0 || position >= values.length) return "medium";
        return values[position];
    }

    private void setupWindow() {
        Window window = getWindow();
        if (window == null) return;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}
