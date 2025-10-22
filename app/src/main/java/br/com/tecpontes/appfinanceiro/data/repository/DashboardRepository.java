package br.com.tecpontes.appfinanceiro.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import br.com.tecpontes.appfinanceiro.data.local.dao.AccountDao;
import br.com.tecpontes.appfinanceiro.data.local.dao.TransactionDao;
import br.com.tecpontes.appfinanceiro.model.DashboardDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository para dados do dashboard
 */
@Singleton
public class DashboardRepository {

    private static final String TAG = "DashboardRepository";

    private final ApiService apiService;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;

    @Inject
    public DashboardRepository(ApiService apiService, AccountDao accountDao, TransactionDao transactionDao) {
        this.apiService = apiService;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
    }

    /**
     * Obtém dados do dashboard da API
     */
    public void getDashboardData(@NonNull DashboardCallback callback) {
        Log.d(TAG, "Carregando dados do dashboard da API");

        Call<DashboardDto> call = apiService.getDashboard();

        call.enqueue(new Callback<DashboardDto>() {
            @Override
            public void onResponse(@NonNull Call<DashboardDto> call,
                                 @NonNull Response<DashboardDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DashboardDto dashboardData = response.body();
                    Log.d(TAG, "Dados do dashboard carregados: " + dashboardData);
                    callback.onSuccess(dashboardData);
                } else {
                    Log.d(TAG, "Erro na resposta do dashboard: " + response.message());
                    callback.onError("Erro na API: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<DashboardDto> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha ao carregar dashboard", t);
                callback.onError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    /**
     * Obtém dados locais do dashboard (fallback)
     */
    public void getLocalDashboardData(@NonNull LocalDashboardCallback callback) {
        Log.d(TAG, "Carregando dados locais do dashboard");

        // Dados locais como fallback quando API não disponível
        new Thread(() -> {
            try {
                // Obtém dados locais
                Double totalBalance = accountDao.getTotalBalance().getValue();
                Integer accountsCount = accountDao.getAccountsCount().getValue();

                // Valores padrão se não houver dados
                if (totalBalance == null) totalBalance = 0.0;
                if (accountsCount == null) accountsCount = 0;

                Log.d(TAG, "Dados locais carregados - Saldo: " + totalBalance + ", Contas: " + accountsCount);
                callback.onSuccess(totalBalance, accountsCount);

            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar dados locais", e);
                callback.onError("Erro local: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return "DashboardRepository{" +
                "accountDao=" + accountDao +
                ", transactionDao=" + transactionDao +
                '}';
    }

    /**
     * Interfaces para callbacks
     */
    public interface DashboardCallback {
        void onSuccess(DashboardDto dashboardData);
        void onError(String error);
    }

    public interface LocalDashboardCallback {
        void onSuccess(double totalBalance, int accountsCount);
        void onError(String error);
    }
}
