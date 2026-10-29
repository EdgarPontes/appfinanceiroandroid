package br.com.tecpontes.appfinanceiro.data.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import br.com.tecpontes.appfinanceiro.data.local.dao.PendingTransactionDao;
import br.com.tecpontes.appfinanceiro.data.local.entity.PendingTransaction;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import br.com.tecpontes.appfinanceiro.sync.TransactionSyncScheduler;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Persiste lançamentos offline antes de enviá-los em segundo plano. */
@Singleton
public class PendingTransactionQueue {

    private final PendingTransactionDao dao;
    private final TokenManager tokenManager;
    private final TransactionSyncScheduler scheduler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Inject
    public PendingTransactionQueue(
            PendingTransactionDao dao,
            TokenManager tokenManager,
            TransactionSyncScheduler scheduler) {
        this.dao = dao;
        this.tokenManager = tokenManager;
        this.scheduler = scheduler;
    }

    @NonNull
    public LiveData<List<PendingTransaction>> observeAll() {
        return dao.observeAll();
    }

    public void enqueue(@NonNull TransactionWriteDto request, @NonNull Callback callback) {
        PendingTransaction pending = PendingTransaction.from(
                tokenManager.getUserIdentity(), request);
        executor.execute(() -> {
            try {
                dao.insert(pending);
                mainHandler.post(() -> {
                    scheduler.requestSync();
                    callback.onSuccess();
                });
            } catch (RuntimeException error) {
                mainHandler.post(() -> callback.onError(
                        "Não foi possível salvar o lançamento no aparelho: "
                                + error.getMessage()));
            }
        });
    }

    public void discard(@NonNull String id) {
        executor.execute(() -> dao.deleteById(id));
    }

    public void retryAll() {
        String owner = tokenManager.getUserIdentity();
        if (owner == null || owner.trim().isEmpty()) return;
        executor.execute(() -> {
            dao.retryAllErrors(owner);
            mainHandler.post(scheduler::requestSync);
        });
    }

    public void requestSync() {
        scheduler.requestSync();
    }

    public interface Callback {
        void onSuccess();
        void onError(@NonNull String error);
    }
}
