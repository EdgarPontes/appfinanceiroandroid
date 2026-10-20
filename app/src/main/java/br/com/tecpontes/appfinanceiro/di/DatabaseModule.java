package br.com.tecpontes.appfinanceiro.di;

import android.content.Context;
import android.util.Log;
import androidx.room.Room;
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
            .fallbackToDestructiveMigration() // TODO: Implementar migração adequada em produção
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
}
