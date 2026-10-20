package br.com.tecpontes.appfinanceiro.viewmodel;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.local.entity.Account;
import br.com.tecpontes.appfinanceiro.data.local.entity.Transaction;
import br.com.tecpontes.appfinanceiro.data.repository.AccountRepository;
import br.com.tecpontes.appfinanceiro.data.repository.DashboardRepository;
import br.com.tecpontes.appfinanceiro.data.repository.TransactionRepository;
import br.com.tecpontes.appfinanceiro.model.DashboardDto;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;

/**
 * ViewModel para tela do dashboard
 */
@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private static final String TAG = "DashboardViewModel";

    private final DashboardRepository dashboardRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // LiveData para estados da UI
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public final LiveData<Boolean> loading = _loading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _refreshSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> refreshSuccess = _refreshSuccess;

    // LiveData para dados do dashboard
    private final MutableLiveData<DashboardDto> _dashboardData = new MutableLiveData<>();
    public final LiveData<DashboardDto> dashboardData = _dashboardData;

    private final MutableLiveData<List<Account>> _accounts = new MutableLiveData<>();
    public final LiveData<List<Account>> accounts = _accounts;

    private final MutableLiveData<List<Transaction>> _recentTransactions = new MutableLiveData<>();
    public final LiveData<List<Transaction>> recentTransactions = _recentTransactions;

    private final MutableLiveData<Double> _totalBalance = new MutableLiveData<>();
    public final LiveData<Double> totalBalance = _totalBalance;

    @Inject
    public DashboardViewModel(
            DashboardRepository dashboardRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository
    ) {
        this.dashboardRepository = dashboardRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;

        Log.d(TAG, "DashboardViewModel inicializado");

        // Observa dados locais
        observeLocalData();
    }

    /**
     * Observa dados locais do banco
     */
    private void observeLocalData() {
        // Observa contas
        accountRepository.getAllAccounts().observeForever(accounts -> {
            _accounts.setValue(accounts);
            Log.d(TAG, "Contas atualizadas: " + (accounts != null ? accounts.size() : 0));
        });

        // Observa transações recentes
        transactionRepository.getRecentTransactions().observeForever(transactions -> {
            _recentTransactions.setValue(transactions);
            Log.d(TAG, "Transações recentes atualizadas: " + (transactions != null ? transactions.size() : 0));
        });

        // Observa saldo total
        accountRepository.getTotalBalance().observeForever(balance -> {
            _totalBalance.setValue(balance != null ? balance : 0.0);
            Log.d(TAG, "Saldo total atualizado: " + balance);
        });
    }

    /**
     * Carrega dados do dashboard
     */
    public void loadDashboardData() {
        Log.d(TAG, "Carregando dados do dashboard");

        _loading.setValue(true);
        _error.setValue(null);

        dashboardRepository.getDashboardData(new DashboardRepository.DashboardCallback() {
            @Override
            public void onSuccess(DashboardDto dashboardData) {
                _loading.postValue(false);
                _dashboardData.postValue(dashboardData);
                Log.d(TAG, "Dados do dashboard carregados com sucesso");
            }

            @Override
            public void onError(String error) {
                _loading.postValue(false);
                _error.postValue(error);
                Log.d(TAG, "Erro ao carregar dashboard: " + error);

                // Tenta carregar dados locais como fallback
                loadLocalDashboardData();
            }
        });
    }

    /**
     * Carrega dados locais como fallback
     */
    private void loadLocalDashboardData() {
        Log.d(TAG, "Carregando dados locais como fallback");

        dashboardRepository.getLocalDashboardData(new DashboardRepository.LocalDashboardCallback() {
            @Override
            public void onSuccess(double totalBalance, int accountsCount) {
                Log.d(TAG, "Dados locais carregados - Saldo: " + totalBalance + ", Contas: " + accountsCount);
                // Dados locais já estão sendo observados automaticamente
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Erro ao carregar dados locais: " + error);
                _error.postValue("Erro ao carregar dados locais: " + error);
            }
        });
    }

    /**
     * Sincroniza contas da API
     */
    public void syncAccounts() {
        Log.d(TAG, "Sincronizando contas");

        _loading.setValue(true);
        _error.setValue(null);

        accountRepository.syncAccounts(new AccountRepository.SyncCallback() {
            @Override
            public void onSuccess(int count) {
                _loading.postValue(false);
                _refreshSuccess.postValue(true);
                Log.d(TAG, "Contas sincronizadas com sucesso: " + count);
            }

            @Override
            public void onError(String error) {
                _loading.postValue(false);
                _error.postValue(error);
                Log.d(TAG, "Erro na sincronização de contas: " + error);
            }
        });
    }

    /**
     * Sincroniza transações da API
     */
    public void syncTransactions(@NonNull String accountId) {
        Log.d(TAG, "Sincronizando transações para conta: " + accountId);

        _loading.setValue(true);
        _error.setValue(null);

        transactionRepository.syncTransactions(accountId, new TransactionRepository.SyncCallback() {
            @Override
            public void onSuccess(int count) {
                _loading.postValue(false);
                _refreshSuccess.postValue(true);
                Log.d(TAG, "Transações sincronizadas com sucesso: " + count);
            }

            @Override
            public void onError(String error) {
                _loading.postValue(false);
                _error.postValue(error);
                Log.d(TAG, "Erro na sincronização de transações: " + error);
            }
        });
    }

    /**
     * Atualiza dados locais
     */
    public void refreshData() {
        Log.d(TAG, "Atualizando dados do dashboard");
        loadDashboardData();
    }

    /**
     * Limpa estados de erro e sucesso
     */
    public void clearStates() {
        _error.setValue(null);
        _refreshSuccess.setValue(false);
    }

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return "DashboardViewModel{" +
                "loading=" + _loading.getValue() +
                ", hasError=" + (_error.getValue() != null) +
                ", accountsCount=" + (_accounts.getValue() != null ? _accounts.getValue().size() : 0) +
                ", transactionsCount=" + (_recentTransactions.getValue() != null ? _recentTransactions.getValue().size() : 0) +
                ", totalBalance=" + _totalBalance.getValue() +
                '}';
    }
}
