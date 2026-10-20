package br.com.tecpontes.appfinanceiro.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import br.com.tecpontes.appfinanceiro.data.local.entity.Transaction;
import java.util.List;

/**
 * DAO para operações com transações
 */
@Dao
public interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByAccount(String accountId);

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    LiveData<Transaction> getTransactionById(String transactionId);

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByAccountAndDateRange(String accountId, long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByType(String type);

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByCategory(String category);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income' AND accountId = :accountId AND date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalIncome(String accountId, long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense' AND accountId = :accountId AND date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalExpenses(String accountId, long startDate, long endDate);

    @Query("SELECT COUNT(*) FROM transactions")
    LiveData<Integer> getTransactionsCount();

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    LiveData<Integer> getTransactionsCountByAccount(String accountId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Transaction transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Transaction> transactions);

    @Update
    void update(Transaction transaction);

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    void deleteById(String transactionId);

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    void deleteByAccountId(String accountId);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT * FROM transactions WHERE lastSync < :timestamp")
    List<Transaction> getTransactionsNeedingSync(long timestamp);

    @Query("UPDATE transactions SET lastSync = :timestamp WHERE id = :transactionId")
    void updateSyncTimestamp(String transactionId, long timestamp);

    // Queries para dashboard
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT 5")
    LiveData<List<Transaction>> getRecentTransactions();

    @Query("SELECT category, SUM(amount) as total, type FROM transactions WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate GROUP BY category, type ORDER BY total DESC")
    LiveData<List<CategorySummary>> getCategorySummary(String accountId, long startDate, long endDate);

    /**
     * Classe auxiliar para resumo por categoria
     */
    class CategorySummary {
        public String category;
        public double total;
        public String type;
    }
}
