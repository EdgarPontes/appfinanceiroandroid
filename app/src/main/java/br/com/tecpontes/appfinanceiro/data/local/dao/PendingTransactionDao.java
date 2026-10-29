package br.com.tecpontes.appfinanceiro.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import br.com.tecpontes.appfinanceiro.data.local.entity.PendingTransaction;
import java.util.List;

/** Fila persistente de lançamentos criados offline. */
@Dao
public interface PendingTransactionDao {

    @Insert
    void insert(PendingTransaction transaction);

    @Query("SELECT * FROM pending_transactions ORDER BY createdAt ASC")
    LiveData<List<PendingTransaction>> observeAll();

    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' "
            + "AND ownerEmail = :ownerEmail ORDER BY createdAt ASC")
    List<PendingTransaction> getReadyToSync(String ownerEmail);

    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' "
            + "AND ownerEmail = :ownerEmail ORDER BY createdAt ASC LIMIT 1")
    PendingTransaction getNextReady(String ownerEmail);

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    void deleteById(String id);

    @Query("UPDATE pending_transactions SET attempts = attempts + 1, "
            + "lastError = :error WHERE id = :id")
    void markAttempt(String id, String error);

    @Query("UPDATE pending_transactions SET status = 'ERROR', attempts = attempts + 1, "
            + "lastError = :error WHERE id = :id")
    void markError(String id, String error);

    @Query("UPDATE pending_transactions SET status = 'PENDING', lastError = NULL "
            + "WHERE id = :id")
    void markRetry(String id);

    @Query("UPDATE pending_transactions SET status = 'PENDING', lastError = NULL, attempts = 0 "
            + "WHERE status = 'ERROR' AND ownerEmail = :ownerEmail")
    void retryAllErrors(String ownerEmail);

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'PENDING' "
            + "AND ownerEmail = :ownerEmail")
    int countReady(String ownerEmail);
}
