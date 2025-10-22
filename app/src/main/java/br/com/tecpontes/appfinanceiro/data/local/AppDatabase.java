package br.com.tecpontes.appfinanceiro.data.local;

import android.util.Log;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import br.com.tecpontes.appfinanceiro.data.local.dao.AccountDao;
import br.com.tecpontes.appfinanceiro.data.local.dao.TransactionDao;
import br.com.tecpontes.appfinanceiro.data.local.entity.Account;
import br.com.tecpontes.appfinanceiro.data.local.entity.Transaction;

/**
 * Database Room principal da aplicação
 */
@Database(
    entities = {
        Account.class,
        Transaction.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "app_financeiro_db";

    public abstract AccountDao accountDao();
    public abstract TransactionDao transactionDao();

    /**
     * Classe utilitária para conversões de tipo
     */
    public static class Converters {
        // Adicionar conversores personalizados se necessário
        // Exemplo: @TypeConverter public static Date fromTimestamp(Long value) { ... }
    }

    /**
     * Callback para eventos do banco de dados
     */
    public static RoomDatabase.Callback createCallback() {
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                super.onCreate(db);
                Log.d(TAG, "Database criado pela primeira vez");
            }

            @Override
            public void onOpen(androidx.sqlite.db.SupportSQLiteDatabase db) {
                super.onOpen(db);
                Log.d(TAG, "Database aberto");
            }
        };
    }
}
