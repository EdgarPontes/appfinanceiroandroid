package br.com.tecpontes.appfinanceiro.di;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import br.com.tecpontes.appfinanceiro.data.local.AppDatabase;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

/**
 * Módulo Hilt para configuração do banco de dados Room
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    private static final String TAG = "DatabaseModule";
    private static final String DATABASE_NAME = "app_financeiro_db";

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_transactions` ("
                            + "`id` TEXT NOT NULL, "
                            + "`ownerEmail` TEXT, "
                            + "`description` TEXT, "
                            + "`amount` TEXT, "
                            + "`date` TEXT, "
                            + "`type` TEXT, "
                            + "`categoryId` TEXT, "
                            + "`categoryName` TEXT, "
                            + "`accountId` TEXT, "
                            + "`accountName` TEXT, "
                            + "`cardId` TEXT, "
                            + "`cardName` TEXT, "
                            + "`cardDebitAccountId` TEXT, "
                            + "`note` TEXT, "
                            + "`isInstallment` INTEGER NOT NULL, "
                            + "`totalInstallments` INTEGER, "
                            + "`installmentValueType` TEXT, "
                            + "`totalValue` TEXT, "
                            + "`isFixedExpense` INTEGER NOT NULL, "
                            + "`createdAt` INTEGER NOT NULL, "
                            + "`attempts` INTEGER NOT NULL, "
                            + "`status` TEXT NOT NULL, "
                            + "`lastError` TEXT, "
                            + "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS "
                    + "`index_pending_transactions_ownerEmail` "
                    + "ON `pending_transactions` (`ownerEmail`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS "
                    + "`index_pending_transactions_status` "
                    + "ON `pending_transactions` (`status`)");
        }
    };

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        Log.d(TAG, "Criando instância do AppDatabase");

        return Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
            )
            .addCallback(AppDatabase.createCallback())
            .addMigrations(MIGRATION_1_2)
            .build();
    }

    @Provides
    @Singleton
    public br.com.tecpontes.appfinanceiro.data.local.dao.AccountDao provideAccountDao(AppDatabase database) {
        return database.accountDao();
    }

    @Provides
    @Singleton
    public br.com.tecpontes.appfinanceiro.data.local.dao.TransactionDao provideTransactionDao(AppDatabase database) {
        return database.transactionDao();
    }

    @Provides
    @Singleton
    public br.com.tecpontes.appfinanceiro.data.local.dao.PendingTransactionDao
    providePendingTransactionDao(AppDatabase database) {
        return database.pendingTransactionDao();
    }
}
