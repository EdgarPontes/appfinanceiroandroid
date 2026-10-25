package br.com.tecpontes.appfinanceiro.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/** Canais de notificação usados pelas notificações financeiras locais. */
public final class NotificationChannels {

    public static final String CHANNEL_ID = "financial_notifications";
    public static final String CHANNEL_NAME = "Notificações financeiras";

    public static final String CHANNEL_ID_HIGH = "financial_notifications_high";
    public static final String CHANNEL_NAME_HIGH = "Alertas urgentes";

    private NotificationChannels() {}

    public static void ensureCreated(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channelDefault = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channelDefault.setDescription("Avisos de vencimento, pagamentos e lembretes personalizados");
        manager.createNotificationChannel(channelDefault);

        NotificationChannel channelHigh = new NotificationChannel(
                CHANNEL_ID_HIGH,
                CHANNEL_NAME_HIGH,
                NotificationManager.IMPORTANCE_HIGH
        );
        channelHigh.setDescription("Alertas importantes e contas com vencimento hoje ou em atraso");
        manager.createNotificationChannel(channelHigh);
    }
}
