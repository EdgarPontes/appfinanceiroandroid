package br.com.tecpontes.appfinanceiro.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/** Canal único usado pelas notificações financeiras locais. */
public final class NotificationChannels {

    public static final String CHANNEL_ID = "financial_notifications";
    public static final String CHANNEL_NAME = "Notificações financeiras";

    private NotificationChannels() {}

    public static void ensureCreated(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Avisos de vencimento, pagamentos e lembretes personalizados");
        manager.createNotificationChannel(channel);
    }
}
