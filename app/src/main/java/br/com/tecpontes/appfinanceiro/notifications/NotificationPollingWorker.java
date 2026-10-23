package br.com.tecpontes.appfinanceiro.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import br.com.tecpontes.appfinanceiro.data.repository.NotificationsRepository;
import br.com.tecpontes.appfinanceiro.data.repository.SettingsRepository;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;
import br.com.tecpontes.appfinanceiro.model.MessageLogDto;
import br.com.tecpontes.appfinanceiro.model.UserSettingsDto;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Busca notificações não lidas no servidor e publica as novas no aparelho. */
public class NotificationPollingWorker extends Worker {

    private static final int MAX_NOTIFICATIONS_PER_RUN = 5;

    public NotificationPollingWorker(
            @NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        WorkerEntryPoint entryPoint =
                EntryPointAccessors.fromApplication(context, WorkerEntryPoint.class);

        NotificationPreferences preferences = entryPoint.notificationPreferences();
        TokenManager tokenManager = entryPoint.tokenManager();

        if (!preferences.isDeviceNotificationsEnabled() || !tokenManager.isLoggedIn()) {
            return Result.success();
        }
        if (!LocalNotificationPublisher.canPost(context)) {
            return Result.success();
        }

        NotificationsRepository notifications = entryPoint.notificationsRepository();
        SettingsRepository settingsRepository = entryPoint.settingsRepository();

        try {
            UserSettingsDto settings = settingsRepository.getSettingsBlocking();
            if (isCurrentlyInDnd(settings)) {
                return Result.success();
            }

            List<MessageLogDto> unread = notifications.getUnreadLogsBlocking(
                    NotificationsRepository.UNREAD_LIMIT);
            if (unread.isEmpty()) {
                return Result.success();
            }

            Map<String, MessageConfigDto> configs = loadConfigs(notifications);
            java.time.Instant cutoff = java.time.Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
            int published = 0;
            for (MessageLogDto log : unread) {
                if (log.id == null || preferences.wasNotified(log.id)) continue;

                if (published < MAX_NOTIFICATIONS_PER_RUN && isRecent(log.sentAt, cutoff)) {
                    MessageConfigDto config = log.configId == null ? null : configs.get(log.configId);
                    String title = config != null ? config.title : null;
                    String fallback = config != null ? config.defaultText : null;
                    LocalNotificationPublisher.publish(context, log, title, fallback);
                    published++;
                }
                // Marca todos como vistos para não repetir o mesmo lote a cada execução.
                preferences.markNotified(log.id);
            }
            return Result.success();
        } catch (Exception error) {
            return Result.retry();
        }
    }

    private static boolean isRecent(@Nullable String sentAt, @NonNull java.time.Instant cutoff) {
        if (sentAt == null || sentAt.trim().isEmpty()) return true;
        try {
            return java.time.Instant.parse(sentAt).isAfter(cutoff);
        } catch (Exception ignored) {
            return true;
        }
    }

    @NonNull
    private Map<String, MessageConfigDto> loadConfigs(NotificationsRepository repository) {
        Map<String, MessageConfigDto> map = new HashMap<>();
        try {
            for (MessageConfigDto config : repository.getConfigsBlocking()) {
                if (config.id != null) map.put(config.id, config);
            }
        } catch (Exception ignored) {
            // Sem os templates ainda é possível notificar usando o conteúdo do log.
        }
        return map;
    }

    /** Mesma regra usada pelo scheduler do servidor, incluindo faixas que cruzam meia-noite. */
    static boolean isCurrentlyInDnd(@Nullable UserSettingsDto settings) {
        if (settings == null || !settings.isNotDisturbActive()) return false;

        int current = currentMinutes();
        int start = parseMinutes(settings.notDisturbStart, 22 * 60);
        int end = parseMinutes(settings.notDisturbEnd, 8 * 60);

        if (start < end) {
            return current >= start && current <= end;
        }
        return current >= start || current <= end;
    }

    private static int currentMinutes() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        return now.get(java.util.Calendar.HOUR_OF_DAY) * 60
                + now.get(java.util.Calendar.MINUTE);
    }

    private static int parseMinutes(@Nullable String value, int fallback) {
        if (value == null) return fallback;
        String[] parts = value.split(":");
        if (parts.length != 2) return fallback;
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return fallback;
            return hour * 60 + minute;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent.class)
    public interface WorkerEntryPoint {
        NotificationsRepository notificationsRepository();
        SettingsRepository settingsRepository();
        TokenManager tokenManager();
        NotificationPreferences notificationPreferences();
    }
}
