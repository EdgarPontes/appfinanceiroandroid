package br.com.tecpontes.appfinanceiro.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Recarrega o agendamento de verificação periódica de notificações após o reinício do aparelho. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationPollingScheduler.schedule(context);
        }
    }
}
