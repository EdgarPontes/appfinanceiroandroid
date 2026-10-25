package br.com.tecpontes.appfinanceiro.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Preferências locais das notificações do aparelho (independentes do servidor). */
@Singleton
public class NotificationPreferences {

    private static final String PREF_NAME = "notification_prefs";
    private static final String KEY_ENABLED = "device_notifications_enabled";

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

}
