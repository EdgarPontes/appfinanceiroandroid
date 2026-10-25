package br.com.tecpontes.appfinanceiro.ui.notifications;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.DialogCreateReminderBinding;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Diálogo de criação de lembrete personalizado. */
public class CreateReminderDialog extends Dialog {

    public interface Listener {
        void onReminderCreated(@NonNull MessageConfigDto config);
    }

    private DialogCreateReminderBinding binding;
    private final Listener listener;
    private LocalDate selectedDate;
    private LocalTime selectedTime = LocalTime.of(9, 0);

    public CreateReminderDialog(@NonNull Context context, @NonNull Listener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        binding = DialogCreateReminderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecurrence();
        selectedDate = LocalDate.now();
        renderDate(selectedDate);
        renderTime();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupWindow();
    }

    private void setupRecurrence() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.notification_recurrence_labels,
                R.layout.item_spinner_form);
        adapter.setDropDownViewResource(R.layout.item_spinner_form_dropdown);
        binding.reminderRecurrenceSpinner.setAdapter(adapter);
        binding.reminderRecurrenceSpinner.setSelection(0);
    }

    private void setupListeners() {
        binding.cancelReminderButton.setOnClickListener(v -> dismiss());
        binding.reminderDateTextView.setOnClickListener(v -> showDatePicker());
        binding.reminderDateButton.setOnClickListener(v -> showDatePicker());
        binding.reminderTimeTextView.setOnClickListener(v -> showTimePicker());
        binding.reminderTimeButton.setOnClickListener(v -> showTimePicker());
        binding.saveReminderButton.setOnClickListener(v -> save());
    }

    private void showDatePicker() {
        LocalDate initial = selectedDate == null ? LocalDate.now() : selectedDate;
        DatePickerDialog picker = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    renderDate(selectedDate);
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth());
        picker.show();
    }

    private void showTimePicker() {
        TimePickerDialog picker = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    selectedTime = LocalTime.of(hourOfDay, minute);
                    renderTime();
                },
                selectedTime.getHour(),
                selectedTime.getMinute(),
                true);
        picker.show();
    }

    private void renderDate(LocalDate date) {
        binding.reminderDateTextView.setText(String.format(
                java.util.Locale.getDefault(), "%02d/%02d/%04d",
                date.getDayOfMonth(), date.getMonthValue(), date.getYear()));
    }

    private void renderTime() {
        binding.reminderTimeTextView.setText(String.format(
                java.util.Locale.ROOT, "%02d:%02d", selectedTime.getHour(), selectedTime.getMinute()));
    }

    private void save() {
        String title = binding.reminderTitleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            binding.reminderTitleEditText.setError(
                    getContext().getString(R.string.notification_required_title));
            return;
        }
        String text = binding.reminderTextEditText.getText().toString().trim();
        if (text.isEmpty()) {
            binding.reminderTextEditText.setError(
                    getContext().getString(R.string.notification_required_text));
            return;
        }

        MessageConfigDto config = new MessageConfigDto();
        config.title = title;
        config.defaultText = text;
        config.type = MessageConfigDto.TYPE_CUSTOM_REMINDER;
        config.priority = "medium";
        config.isActive = true;
        config.recurrenceType = recurrenceValue();
        config.reminderTime = String.format(
                java.util.Locale.ROOT, "%02d:%02d", selectedTime.getHour(), selectedTime.getMinute());
        config.timeZone = ZoneId.systemDefault().getId();

        config.specificDate = DateTimeFormatter.ISO_INSTANT.format(
                selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        listener.onReminderCreated(config);
        dismiss();
    }

    @NonNull
    private String recurrenceValue() {
        String[] values = getContext().getResources()
                .getStringArray(R.array.notification_recurrence_values);
        int position = binding.reminderRecurrenceSpinner.getSelectedItemPosition();
        if (position < 0 || position >= values.length) return "none";
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
