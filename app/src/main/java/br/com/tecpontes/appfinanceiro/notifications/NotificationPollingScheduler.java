package br.com.tecpontes.appfinanceiro.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

/** Agenda a verificação periódica de notificações não lidas. */
public final class NotificationPollingScheduler {

    private static final String UNIQUE_PERIODIC = "notification-polling-periodic";
    private static final String UNIQUE_ONCE = "notification-polling-once";

    private NotificationPollingScheduler() {}

    public static void schedule(@NonNull Context context) {
        NotificationChannels.ensureCreated(context);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                NotificationPollingWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    /** Executa uma verificação imediata (login, abertura do app, ativação). */
    public static void runNow(@NonNull Context context) {
        NotificationChannels.ensureCreated(context);

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(
                NotificationPollingWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONCE, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancel(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC);
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_ONCE);
    }
}
