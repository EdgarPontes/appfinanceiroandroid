package br.com.tecpontes.appfinanceiro.notifications;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.model.MessageLogDto;
import br.com.tecpontes.appfinanceiro.ui.notifications.NotificationHistoryActivity;
import br.com.tecpontes.appfinanceiro.utils.DateFormats;

/** Monta e publica as notificações locais do aparelho. */
public final class LocalNotificationPublisher {

    public static final String EXTRA_LOG_ID = "notification_log_id";

    private LocalNotificationPublisher() {}

    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void publish(
            @NonNull Context context,
            @NonNull MessageLogDto log,
            @Nullable String title,
            @Nullable String fallbackText) {
        if (log.id == null) return;
        if (!canPost(context)) return;

        NotificationChannels.ensureCreated(context);

        String content = log.content;
        if (content == null || content.trim().isEmpty()) content = fallbackText;
        if (content == null || content.trim().isEmpty()) content = "Nova notificação financeira";

        Intent intent = new Intent(context, NotificationHistoryActivity.class);
        intent.putExtra(EXTRA_LOG_ID, log.id);
        intent.setData(Uri.parse("appfinanceiro://notification/" + log.id));

        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        boolean important = isImportant(log);
        String channelId = important ? NotificationChannels.CHANNEL_ID_HIGH : NotificationChannels.CHANNEL_ID;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, channelId)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle(title == null || title.trim().isEmpty() ? "Notificação" : title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(important ? NotificationCompat.PRIORITY_HIGH
                        : NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup("financial_notifications");

        if (DateFormats.formatTime(log.sentAt) != null) {
            builder.setWhen(System.currentTimeMillis());
        }

        try {
            // A stable tag makes provider retries update one notification, not create duplicates.
            NotificationManagerCompat.from(context).notify(log.id, 0, builder.build());
        } catch (SecurityException ignored) {
            // Permissão revogada entre a verificação e a publicação.
        }
    }

    public static void publishFcm(
            @NonNull Context context,
            @NonNull String logId,
            @Nullable String title,
            @Nullable String body,
            @Nullable String priority) {
        MessageLogDto log = new MessageLogDto();
        log.id = logId;
        log.content = body;
        log.priority = priority;
        log.sentAt = java.time.Instant.now().toString();
        publish(context, log, title, body);
    }

    private static boolean isImportant(@NonNull MessageLogDto log) {
        return "critical".equalsIgnoreCase(log.priority) || "high".equalsIgnoreCase(log.priority);
    }
}
