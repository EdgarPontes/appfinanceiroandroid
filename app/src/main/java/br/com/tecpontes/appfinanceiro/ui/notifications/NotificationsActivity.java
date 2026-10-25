package br.com.tecpontes.appfinanceiro.ui.notifications;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.ActivityNotificationsBinding;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.NotificationsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/** Configuração das notificações financeiras e dos lembretes personalizados. */
@AndroidEntryPoint
public class NotificationsActivity extends AppCompatActivity {

    private static final Set<String> DEFAULT_TITLES = new HashSet<>(Arrays.asList(
            "Lembrete de Vencimento (5 dias)",
            "Lembrete de Vencimento (1 dia)",
            "Aviso de Atraso (1 dia)",
            "Confirmação de Pagamento"
    ));

    private ActivityNotificationsBinding binding;
    private NotificationsViewModel viewModel;

    @Inject TokenManager tokenManager;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableDeviceNotifications();
                } else {
                    updateDeviceCard();
                    Toast.makeText(this,
                            "Permissão de notificações não concedida.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        BottomNavigationHelper.setup(this, R.id.navigation_menu);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        setupListeners();
        setupObservers();
        SessionExpiredNavigation.observe(this, tokenManager);
        viewModel.loadConfigs();
        updateDeviceCard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null
                && Boolean.TRUE.equals(viewModel.deviceNotificationsEnabledData.getValue())
                && !br.com.tecpontes.appfinanceiro.notifications.LocalNotificationPublisher.canPost(this)) {
            viewModel.setDeviceNotificationsEnabled(false);
        }
        updateDeviceCard();
    }

    private void setupListeners() {
        binding.createReminderButton.setOnClickListener(v ->
                new CreateReminderDialog(this, config -> viewModel.createReminder(config)).show());

        binding.createDefaultsButton.setOnClickListener(v -> viewModel.createDefaultConfigs());
        binding.historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationHistoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        binding.deviceToggleButton.setOnClickListener(v -> onDeviceToggleClicked());
    }

    private void setupObservers() {
        viewModel.configsData.observe(this, this::renderConfigs);

        viewModel.isLoading.observe(this, loading -> {
            binding.notificationsProgressBar.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            updateActionButtons();
        });

        viewModel.isSaving.observe(this, saving -> updateActionButtons());

        viewModel.errorMessage.observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                binding.notificationsErrorTextView.setVisibility(View.GONE);
            } else {
                binding.notificationsErrorTextView.setText(message);
                binding.notificationsErrorTextView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.successMessage.observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.deviceNotificationsEnabledData.observe(this, enabled -> updateDeviceCard());
    }

    private void renderConfigs(@Nullable List<MessageConfigDto> configs) {
        if (configs == null) configs = new ArrayList<>();

        binding.financialConfigsContainer.removeAllViews();
        binding.remindersContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        List<MessageConfigDto> financial = new ArrayList<>();
        List<MessageConfigDto> reminders = new ArrayList<>();
        boolean hasDefaultTitles = false;

        for (MessageConfigDto config : configs) {
            if (config.isCustomReminder()) {
                reminders.add(config);
            } else {
                financial.add(config);
                if (config.title != null && DEFAULT_TITLES.contains(config.title)) {
                    hasDefaultTitles = true;
                }
            }
        }

        for (MessageConfigDto config : financial) {
            View row = inflater.inflate(R.layout.item_notification_config,
                    binding.financialConfigsContainer, false);
            bindConfigRow(row, config, false);
            binding.financialConfigsContainer.addView(row);
        }

        for (MessageConfigDto config : reminders) {
            View row = inflater.inflate(R.layout.item_notification_config,
                    binding.remindersContainer, false);
            bindConfigRow(row, config, true);
            binding.remindersContainer.addView(row);
        }

        binding.financialEmptyTextView.setVisibility(financial.isEmpty() ? View.VISIBLE : View.GONE);
        binding.remindersEmptyTextView.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
        binding.createDefaultsButton.setVisibility(hasDefaultTitles ? View.GONE : View.VISIBLE);
        updateActionButtons();
    }

    private void updateActionButtons() {
        boolean idle = !Boolean.TRUE.equals(viewModel.isLoading.getValue())
                && !Boolean.TRUE.equals(viewModel.isSaving.getValue());
        binding.createReminderButton.setEnabled(idle);
        binding.createDefaultsButton.setEnabled(idle);
        binding.deviceToggleButton.setEnabled(idle);
    }

    private void bindConfigRow(
            @NonNull View row, @NonNull MessageConfigDto config, boolean isReminder) {
        TextView title = row.findViewById(R.id.configTitleTextView);
        TextView text = row.findViewById(R.id.configTextTextView);
        MaterialSwitch toggle = row.findViewById(R.id.configSwitch);
        ImageButton edit = row.findViewById(R.id.editConfigButton);
        ImageButton delete = row.findViewById(R.id.deleteConfigButton);

        title.setText(config.title == null ? "" : config.title);
        text.setText(config.defaultText == null ? "" : config.defaultText);

        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(config.isActive());
        toggle.setOnCheckedChangeListener((button, checked) ->
                viewModel.toggleConfig(config, checked));

        edit.setOnClickListener(v ->
                new EditMessageConfigDialog(this, config,
                        body -> viewModel.updateConfig(config, body)).show());

        if (isReminder) {
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(v -> confirmDelete(config));
        } else {
            delete.setVisibility(View.GONE);
            delete.setOnClickListener(null);
        }
    }

    private void confirmDelete(@NonNull MessageConfigDto config) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Excluir lembrete")
                .setMessage("Deseja excluir o lembrete \"" + config.title + "\"?")
                .setNegativeButton(R.string.history_cancel, null)
                .setPositiveButton(R.string.history_delete_confirm,
                        (dialog, which) -> viewModel.deleteReminder(config))
                .show();
    }

    private void onDeviceToggleClicked() {
        if (isDeviceActive()) {
            disableDeviceNotifications();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            openNotificationSettings();
            return;
        }

        enableDeviceNotifications();
    }

    private boolean isDeviceActive() {
        boolean preferred = Boolean.TRUE.equals(
                viewModel.deviceNotificationsEnabledData.getValue());
        boolean permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        return preferred && permissionGranted
                && NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private void enableDeviceNotifications() {
        viewModel.setDeviceNotificationsEnabled(true);
        updateDeviceCard();
        Toast.makeText(this, "Notificações do aparelho ativadas.", Toast.LENGTH_SHORT).show();
    }

    private void disableDeviceNotifications() {
        viewModel.setDeviceNotificationsEnabled(false);
        updateDeviceCard();
        Toast.makeText(this, "Notificações do aparelho desativadas.", Toast.LENGTH_SHORT).show();
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (Exception ignored) {
            Toast.makeText(this,
                    "Abra as configurações do sistema para permitir notificações.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateDeviceCard() {
        if (binding == null) return;

        boolean permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        boolean systemBlocked = permissionGranted
                && !NotificationManagerCompat.from(this).areNotificationsEnabled();

        if (systemBlocked) {
            binding.deviceStatusTextView.setText(R.string.notifications_device_blocked);
            binding.deviceStatusTextView.setTextColor(
                    ContextCompat.getColor(this, R.color.money_negative));
            binding.deviceToggleButton.setText(R.string.notifications_device_enable);
        } else if (isDeviceActive()) {
            binding.deviceStatusTextView.setText(R.string.notifications_device_enabled);
            binding.deviceStatusTextView.setTextColor(
                    ContextCompat.getColor(this, R.color.money_positive));
            binding.deviceToggleButton.setText(R.string.notifications_device_disable);
        } else {
            binding.deviceStatusTextView.setText(R.string.notifications_device_disabled);
            binding.deviceStatusTextView.setTextColor(
                    ContextCompat.getColor(this, R.color.brand_primary));
            binding.deviceToggleButton.setText(R.string.notifications_device_enable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
