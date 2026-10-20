package br.com.tecpontes.appfinanceiro.di;

import br.com.tecpontes.appfinanceiro.data.local.dao.AccountDao;
import br.com.tecpontes.appfinanceiro.data.local.dao.TransactionDao;
import br.com.tecpontes.appfinanceiro.data.repository.AccountRepository;
import br.com.tecpontes.appfinanceiro.data.repository.AuthRepository;
import br.com.tecpontes.appfinanceiro.data.repository.DashboardRepository;
import br.com.tecpontes.appfinanceiro.data.repository.TransactionRepository;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

/**
 * Módulo Hilt para injeção de repositories
 */
@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    @Singleton
    public AuthRepository provideAuthRepository(ApiService apiService, TokenManager tokenManager) {
        return new AuthRepository(apiService, tokenManager);
    }

    @Provides
    @Singleton
    public AccountRepository provideAccountRepository(ApiService apiService, AccountDao accountDao) {
        return new AccountRepository(apiService, accountDao);
    }

    @Provides
    @Singleton
    public TransactionRepository provideTransactionRepository(ApiService apiService, TransactionDao transactionDao) {
        return new TransactionRepository(apiService, transactionDao);
    }

    @Provides
    @Singleton
    public DashboardRepository provideDashboardRepository(ApiService apiService, AccountDao accountDao, TransactionDao transactionDao) {
        return new DashboardRepository(apiService, accountDao, transactionDao);
    }
}
