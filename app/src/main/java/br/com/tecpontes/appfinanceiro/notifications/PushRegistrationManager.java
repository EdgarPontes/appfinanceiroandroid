package br.com.tecpontes.appfinanceiro.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import br.com.tecpontes.appfinanceiro.BuildConfig;
import br.com.tecpontes.appfinanceiro.model.PushTokenRegistrationRequest;
import br.com.tecpontes.appfinanceiro.model.PushTokenRegistrationResponse;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class PushRegistrationManager {
    private static final String UNIQUE_SYNC_WORK = "fcm-token-registration";

    private final Context context;
    private final ApiService apiService;
    private final TokenManager tokenManager;
    private final PushRegistrationStore store;
    private final NotificationPreferences notificationPreferences;

    @Inject
    public PushRegistrationManager(
            @ApplicationContext Context context,
            ApiService apiService,
            TokenManager tokenManager,
            PushRegistrationStore store,
            NotificationPreferences notificationPreferences) {
        this.context = context.getApplicationContext();
        this.apiService = apiService;
        this.tokenManager = tokenManager;
        this.store = store;
        this.notificationPreferences = notificationPreferences;
    }

    public void requestSync() {
        if (!tokenManager.isLoggedIn()
                || !notificationPreferences.isDeviceNotificationsEnabled()
                || FirebaseApp.getApps(context).isEmpty()) return;

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PushRegistrationWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_SYNC_WORK, ExistingWorkPolicy.KEEP, request);
    }

    public void setNotificationsEnabled(boolean enabled) {
        notificationPreferences.setDeviceNotificationsEnabled(enabled);
        if (enabled) requestSync();
        else unregisterCurrentSession();
    }

    /** Called by the unique network-constrained Worker. */
    public void syncBlocking() throws IOException {
        if (!tokenManager.isLoggedIn()
                || !notificationPreferences.isDeviceNotificationsEnabled()
                || FirebaseApp.getApps(context).isEmpty()) return;

        final String token;
        try {
            token = Tasks.await(FirebaseMessaging.getInstance().getToken(), 30, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new IOException("Não foi possível obter o token FCM", error);
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IOException("Token FCM vazio");
        }

        PushTokenRegistrationRequest request = new PushTokenRegistrationRequest();
        request.token = token;
        request.deviceId = store.getDeviceId();
        request.appVersion = BuildConfig.VERSION_NAME;
        Response<PushTokenRegistrationResponse> response =
                apiService.registerDevicePushToken(request).execute();
        if (response.isSuccessful() && response.body() != null
                && response.body().id != null && response.body().bindingId != null) {
            store.saveRegistration(response.body().id, response.body().bindingId);
            synchronizeReminderTimeZone();
            return;
        }
        if (response.code() == 401) return; // A later authenticated startup retries.
        throw new IOException("Falha ao registrar token FCM (HTTP " + response.code() + ")");
    }

    private void synchronizeReminderTimeZone() throws IOException {
        String currentZone = ZoneId.systemDefault().getId();
        String previousZone = store.getDeviceTimeZone();
        if (currentZone.equals(previousZone)) return;

        Response<java.util.List<MessageConfigDto>> response = apiService.getMessageConfigs().execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Não foi possível sincronizar o fuso dos lembretes");
        }
        for (MessageConfigDto config : response.body()) {
            if (config == null || !MessageConfigDto.TYPE_CUSTOM_REMINDER.equals(config.type)
                    || config.id == null) continue;
            if (previousZone != null && config.timeZone != null
                    && !previousZone.equals(config.timeZone)) continue;
            if (currentZone.equals(config.timeZone)) continue;

            MessageConfigDto patch = new MessageConfigDto();
            patch.timeZone = currentZone;
            if (config.specificDate != null && config.timeZone != null) {
                try {
                    LocalDate reminderDate = Instant.parse(config.specificDate)
                            .atZone(ZoneId.of(config.timeZone)).toLocalDate();
                    patch.specificDate = reminderDate.atStartOfDay(ZoneId.of(currentZone))
                            .toInstant().toString();
                } catch (Exception ignored) {
                    // Keep the server's existing date if an older record is not ISO-8601.
                }
            }
            Response<MessageConfigDto> updated = apiService.patchMessageConfig(config.id, patch).execute();
            if (!updated.isSuccessful()) {
                throw new IOException("Falha ao atualizar o fuso de um lembrete");
            }
        }
        store.setDeviceTimeZone(currentZone);
    }

    /** Fire-and-forget deletion captures the JWT before local logout clears it. */
    public void unregisterCurrentSession() {
        String registrationId = store.getRegistrationId();
        String jwt = tokenManager.getToken();
        if (registrationId == null || registrationId.isEmpty() || jwt == null || jwt.isEmpty()) return;

        apiService.deleteDevicePushToken("Bearer " + jwt, registrationId)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(
                            @NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful() || response.code() == 404) {
                            store.clearRegistration();
                            if (notificationPreferences.isDeviceNotificationsEnabled()
                                    && tokenManager.isLoggedIn()) requestSync();
                        }
                    }

                    @Override public void onFailure(
                            @NonNull Call<Void> call, @NonNull Throwable error) {
                        // Keep the local association ID. The next authenticated registration
                        // will atomically re-associate this installation and rotate bindingId.
                    }
                });
    }

    @Nullable
    public String currentBindingId() {
        return store.getBindingId();
    }
}
