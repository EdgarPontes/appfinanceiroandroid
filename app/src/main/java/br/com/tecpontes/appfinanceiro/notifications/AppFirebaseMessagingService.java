package br.com.tecpontes.appfinanceiro.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.BuildConfig;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;

@AndroidEntryPoint
public class AppFirebaseMessagingService extends FirebaseMessagingService {
    private static final String EXPECTED_DEEP_LINK = "app://notifications/history";

    @Inject PushRegistrationManager registrationManager;
    @Inject NotificationPreferences notificationPreferences;
    @Inject TokenManager tokenManager;

    @Override
    public void onNewToken(@NonNull String token) {
        // The worker fetches the latest SDK token itself; avoid persisting/logging the raw token.
        registrationManager.requestSync();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (!tokenManager.isLoggedIn() || !notificationPreferences.isDeviceNotificationsEnabled()) return;

        // Firebase Console's "test message" is a notification payload, not the production
        // data-only contract. Allow it only in debug builds so developers can verify the
        // device/permission/channel without weakening production binding validation.
        if ((data == null || data.isEmpty() || !"1".equals(data.get("schemaVersion")))
                && BuildConfig.DEBUG && remoteMessage.getNotification() != null) {
            publishDebugConsoleTest(remoteMessage);
            return;
        }
        if (data == null || data.isEmpty() || !"1".equals(data.get("schemaVersion"))) return;

        String bindingId = data.get("bindingId");
        String currentBindingId = registrationManager.currentBindingId();
        if (bindingId == null || currentBindingId == null || !bindingId.equals(currentBindingId)) return;

        String logId = data.get("logId");
        if (!isUuid(logId)) return;
        String deepLink = data.get("deepLink");
        if (!EXPECTED_DEEP_LINK.equals(deepLink)) return;

        String type = data.get("type");
        if (!isSupportedType(type)) return;
        String priority = data.get("priority");
        if (!isSupportedPriority(priority)) return;

        String title = bounded(data.get("title"), 100);
        String body = bounded(data.get("body"), 700);
        if (title.isEmpty() && body.isEmpty()) return;
        if (!LocalNotificationPublisher.canPost(this)) return;

        FcmMessageDeduplicator deduplicator = FcmMessageDeduplicator.get(this);
        if (deduplicator.wasDelivered(logId)) return;
        LocalNotificationPublisher.publishFcm(this, logId, title, body, priority);
        deduplicator.markDelivered(logId);
    }

    private void publishDebugConsoleTest(@NonNull RemoteMessage message) {
        if (!LocalNotificationPublisher.canPost(this)) return;
        RemoteMessage.Notification notification = message.getNotification();
        if (notification == null) return;
        String messageId = message.getMessageId();
        String source = messageId == null || messageId.isEmpty() ? UUID.randomUUID().toString() : messageId;
        String testLogId = UUID.nameUUIDFromBytes(
                ("fcm-console-test:" + source).getBytes(StandardCharsets.UTF_8)).toString();
        FcmMessageDeduplicator deduplicator = FcmMessageDeduplicator.get(this);
        if (deduplicator.wasDelivered(testLogId)) return;
        LocalNotificationPublisher.publishFcm(
                this,
                testLogId,
                bounded(notification.getTitle(), 100),
                bounded(notification.getBody(), 700),
                "medium");
        deduplicator.markDelivered(testLogId);
    }

    private static boolean isUuid(@Nullable String value) {
        if (value == null) return false;
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isSupportedType(@Nullable String type) {
        return "custom_reminder".equals(type)
                || "financial_due_date".equals(type)
                || "payment_confirmation".equals(type)
                || "delay_notice".equals(type)
                || "cancellation_notice".equals(type);
    }

    private static boolean isSupportedPriority(@Nullable String priority) {
        return "low".equals(priority) || "medium".equals(priority)
                || "high".equals(priority) || "critical".equals(priority);
    }

    @NonNull
    private static String bounded(@Nullable String value, int maxLength) {
        if (value == null) return "";
        String safe = value.trim();
        return safe.length() > maxLength ? safe.substring(0, maxLength) : safe;
    }
}
