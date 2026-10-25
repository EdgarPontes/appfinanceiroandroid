package br.com.tecpontes.appfinanceiro.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;

/** Bounded network retry for FCM installation registration. */
public class PushRegistrationWorker extends Worker {
    public PushRegistrationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        PushRegistrationManager manager = EntryPointAccessors
                .fromApplication(getApplicationContext(), WorkerEntryPoint.class)
                .pushRegistrationManager();
        try {
            manager.syncBlocking();
            return Result.success();
        } catch (Exception error) {
            return getRunAttemptCount() >= 4 ? Result.failure() : Result.retry();
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent.class)
    public interface WorkerEntryPoint {
        PushRegistrationManager pushRegistrationManager();
    }
}
