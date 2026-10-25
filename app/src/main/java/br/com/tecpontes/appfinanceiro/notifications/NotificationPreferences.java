package br.com.tecpontes.appfinanceiro.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Preferências locais das notificações do aparelho (independentes do servidor). */
@Singleton
public class NotificationPreferences {

    private static final String PREF_NAME = "notification_prefs";
    private static final String KEY_ENABLED = "device_notifications_enabled";
    private static final String KEY_SEEN_IDS = "notified_log_ids";
    private static final int MAX_SEEN_IDS = 300;

    private final SharedPreferences preferences;

    @Inject
    public NotificationPreferences(@ApplicationContext Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isDeviceNotificationsEnabled() {
        return preferences.getBoolean(KEY_ENABLED, true);
    }

    public void setDeviceNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public boolean wasNotified(@NonNull String logId) {
        return preferences.getStringSet(KEY_SEEN_IDS, new HashSet<>()).contains(logId);
    }

    /** Registra o log como já exibido, mantendo o conjunto em tamanho controlado. */
    public void markNotified(@NonNull String logId) {
        Set<String> seen = new HashSet<>(
                preferences.getStringSet(KEY_SEEN_IDS, new HashSet<>()));
        if (seen.size() >= MAX_SEEN_IDS) {
            java.util.Iterator<String> iterator = seen.iterator();
            int toRemove = MAX_SEEN_IDS / 3;
            while (iterator.hasNext() && toRemove > 0) {
                iterator.next();
                iterator.remove();
                toRemove--;
            }
        }
        seen.add(logId);
        preferences.edit().putStringSet(KEY_SEEN_IDS, seen).apply();
    }

    public void clearSeen() {
        preferences.edit().remove(KEY_SEEN_IDS).apply();
    }
}
