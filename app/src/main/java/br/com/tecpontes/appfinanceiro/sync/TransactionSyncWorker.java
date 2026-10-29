package br.com.tecpontes.appfinanceiro.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;
import java.io.IOException;

/** Envia os lançamentos pendentes e reagenda em caso de falha temporária. */
public class TransactionSyncWorker extends Worker {

    public TransactionSyncWorker(
            @NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        TransactionSyncManager manager = EntryPointAccessors
                .fromApplication(getApplicationContext(), WorkerEntryPoint.class)
                .transactionSyncManager();
        try {
            manager.syncPendingBlocking();
            return Result.success();
        } catch (IOException error) {
            return Result.retry();
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent.class)
    public interface WorkerEntryPoint {
        TransactionSyncManager transactionSyncManager();
    }
}
