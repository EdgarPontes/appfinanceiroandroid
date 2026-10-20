package br.com.tecpontes.appfinanceiro.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import br.com.tecpontes.appfinanceiro.data.local.entity.Account;
import java.util.List;

/**
 * DAO para operações com contas
 */
@Dao
public interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY name")
    LiveData<List<Account>> getAllAccounts();

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    LiveData<Account> getAccountById(String accountId);

    @Query("SELECT * FROM accounts ORDER BY balance DESC")
    LiveData<List<Account>> getAccountsByBalance();

    @Query("SELECT SUM(balance) FROM accounts")
    LiveData<Double> getTotalBalance();

    @Query("SELECT COUNT(*) FROM accounts")
    LiveData<Integer> getAccountsCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Account account);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Account> accounts);

    @Update
    void update(Account account);

    @Query("DELETE FROM accounts WHERE id = :accountId")
    void deleteById(String accountId);

    @Query("DELETE FROM accounts")
    void deleteAll();

    @Query("SELECT * FROM accounts WHERE lastSync < :timestamp")
    List<Account> getAccountsNeedingSync(long timestamp);

    @Query("UPDATE accounts SET lastSync = :timestamp WHERE id = :accountId")
    void updateSyncTimestamp(String accountId, long timestamp);
}
