package br.com.tecpontes.appfinanceiro.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.repository.NotificationsRepository;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;
import br.com.tecpontes.appfinanceiro.notifications.NotificationPreferences;
import br.com.tecpontes.appfinanceiro.notifications.PushRegistrationManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Estado e ações da tela de notificações (templates e lembretes). */
@HiltViewModel
public class NotificationsViewModel extends ViewModel {

    private final NotificationsRepository repository;
    private final NotificationPreferences preferences;
    private final PushRegistrationManager pushRegistrationManager;

    private final MutableLiveData<List<MessageConfigDto>> configs =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> success = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deviceNotificationsEnabled =
            new MutableLiveData<>(true);

    public final LiveData<List<MessageConfigDto>> configsData = configs;
    public final LiveData<Boolean> isLoading = loading;
    public final LiveData<Boolean> isSaving = saving;
    public final LiveData<String> errorMessage = error;
    public final LiveData<String> successMessage = success;
    public final LiveData<Boolean> deviceNotificationsEnabledData = deviceNotificationsEnabled;

    @Inject
    public NotificationsViewModel(
            NotificationsRepository repository,
            NotificationPreferences preferences,
            PushRegistrationManager pushRegistrationManager) {
        this.repository = repository;
        this.preferences = preferences;
        this.pushRegistrationManager = pushRegistrationManager;
        deviceNotificationsEnabled.setValue(preferences.isDeviceNotificationsEnabled());
    }

    public void loadConfigs() {
        loading.setValue(true);
        error.setValue(null);
        repository.getConfigs(new NotificationsRepository.ResultCallback<List<MessageConfigDto>>() {
            @Override public void onSuccess(@Nullable List<MessageConfigDto> result) {
                loading.postValue(false);
                configs.postValue(result == null ? new ArrayList<>() : result);
            }

            @Override public void onError(@NonNull String message) {
                loading.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void toggleConfig(@NonNull MessageConfigDto config, boolean active) {
        if (config.id == null) return;
        MessageConfigDto body = new MessageConfigDto();
        body.isActive = active;
        saving.setValue(true);
        error.setValue(null);
        repository.patchConfig(config.id, body,
                new NotificationsRepository.ResultCallback<MessageConfigDto>() {
                    @Override public void onSuccess(@Nullable MessageConfigDto result) {
                        saving.postValue(false);
                        success.postValue("Configuração atualizada com sucesso.");
                        replaceConfig(result);
                    }

                    @Override public void onError(@NonNull String message) {
                        saving.postValue(false);
                        error.postValue(message);
                        loadConfigs();
                    }
                });
    }

    public void updateConfig(@NonNull MessageConfigDto original, @NonNull MessageConfigDto body) {
        if (original.id == null) return;
        saving.setValue(true);
        error.setValue(null);
        repository.updateConfig(original.id, body,
                new NotificationsRepository.ResultCallback<MessageConfigDto>() {
                    @Override public void onSuccess(@Nullable MessageConfigDto result) {
                        saving.postValue(false);
                        success.postValue("Template atualizado com sucesso.");
                        replaceConfig(result);
                    }

                    @Override public void onError(@NonNull String message) {
                        saving.postValue(false);
                        error.postValue(message);
                    }
                });
    }

    public void createReminder(@NonNull MessageConfigDto body) {
        saving.setValue(true);
        error.setValue(null);
        repository.createConfig(body,
                new NotificationsRepository.ResultCallback<MessageConfigDto>() {
                    @Override public void onSuccess(@Nullable MessageConfigDto result) {
                        saving.postValue(false);
                        success.postValue("Lembrete criado com sucesso.");
                        loadConfigs();
                    }

                    @Override public void onError(@NonNull String message) {
                        saving.postValue(false);
                        error.postValue(message);
                    }
                });
    }

    public void deleteReminder(@NonNull MessageConfigDto config) {
        if (config.id == null) return;
        saving.setValue(true);
        error.setValue(null);
        repository.deleteConfig(config.id, new NotificationsRepository.ActionCallback() {
            @Override public void onSuccess() {
                saving.postValue(false);
                success.postValue("Lembrete excluído com sucesso.");
                loadConfigs();
            }

            @Override public void onError(@NonNull String message) {
                saving.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void createDefaultConfigs() {
        saving.setValue(true);
        error.setValue(null);
        repository.createDefaults(new NotificationsRepository.ActionCallback() {
            @Override public void onSuccess() {
                saving.postValue(false);
                success.postValue("Configurações padrão criadas com sucesso.");
                loadConfigs();
            }

            @Override public void onError(@NonNull String message) {
                saving.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void setDeviceNotificationsEnabled(boolean enabled) {
        pushRegistrationManager.setNotificationsEnabled(enabled);
        deviceNotificationsEnabled.setValue(enabled);
    }

    private void replaceConfig(@Nullable MessageConfigDto updated) {
        if (updated == null) return;
        List<MessageConfigDto> current = configs.getValue();
        if (current == null) return;
        List<MessageConfigDto> copy = new ArrayList<>(current);
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i).id != null && copy.get(i).id.equals(updated.id)) {
                copy.set(i, updated);
                configs.setValue(copy);
                return;
            }
        }
        copy.add(updated);
        configs.setValue(copy);
    }
}
