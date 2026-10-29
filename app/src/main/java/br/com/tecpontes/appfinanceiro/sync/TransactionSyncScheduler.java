package br.com.tecpontes.appfinanceiro.sync;

import android.content.Context;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Agenda o envio da fila local quando houver rede. */
@Singleton
public class TransactionSyncScheduler {

    private static final String UNIQUE_WORK = "pending-transaction-sync";

    private final Context context;

    @Inject
    public TransactionSyncScheduler(@ApplicationContext Context context) {
        this.context = context.getApplicationContext();
    }

    public void requestSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(TransactionSyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request);
    }
}
