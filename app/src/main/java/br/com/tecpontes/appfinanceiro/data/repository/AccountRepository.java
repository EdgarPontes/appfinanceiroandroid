package br.com.tecpontes.appfinanceiro.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import br.com.tecpontes.appfinanceiro.data.local.dao.AccountDao;
import br.com.tecpontes.appfinanceiro.data.local.entity.Account;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository para operações com contas
 */
@Singleton
public class AccountRepository {

    private static final String TAG = "AccountRepository";

    private final ApiService apiService;
    private final AccountDao accountDao;
    private final ExecutorService executorService;

    @Inject
    public AccountRepository(ApiService apiService, AccountDao accountDao) {
        this.apiService = apiService;
        this.accountDao = accountDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Obtém todas as contas do banco local
     */
    public LiveData<List<Account>> getAllAccounts() {
        return accountDao.getAllAccounts();
    }

    /**
     * Obtém conta por ID
     */
    public LiveData<Account> getAccountById(String accountId) {
        return accountDao.getAccountById(accountId);
    }

    /**
     * Obtém saldo total de todas as contas
     */
    public LiveData<Double> getTotalBalance() {
        return accountDao.getTotalBalance();
    }

    /**
     * Sincroniza contas da API para o banco local
     */
    public void syncAccounts(@NonNull SyncCallback callback) {
        Log.d(TAG, "Sincronizando contas da API");

        Call<List<AccountDto>> call = apiService.getAccounts();

        call.enqueue(new Callback<List<AccountDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AccountDto>> call,
                                 @NonNull Response<List<AccountDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AccountDto> accountDtos = response.body();

                    // Converte DTOs para entidades e salva no banco
                    executorService.execute(() -> {
                        try {
                            for (AccountDto dto : accountDtos) {
                                Account account = new Account(
                                    dto.getId(),
                                    dto.getName(),
                                    dto.getBalance(),
                                    dto.getCurrency(),
                                    dto.getDescription()
                                );
                                accountDao.insert(account);
                            }

                            Log.d(TAG, "Contas sincronizadas com sucesso: " + accountDtos.size());
                            callback.onSuccess(accountDtos.size());
                        } catch (Exception e) {
                            Log.e(TAG, "Erro ao salvar contas no banco", e);
                            callback.onError("Erro ao salvar contas: " + e.getMessage());
                        }
                    });
                } else {
                    Log.d(TAG, "Erro na resposta da API: " + response.message());
                    callback.onError("Erro na API: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AccountDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha na requisição de contas", t);
                callback.onError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    /**
     * Cria nova conta
     */
    public void createAccount(@NonNull AccountDto accountDto, @NonNull CreateCallback callback) {
        Log.d(TAG, "Criando nova conta: " + accountDto.getName());

        Call<AccountDto> call = apiService.createAccount(accountDto);

        call.enqueue(new Callback<AccountDto>() {
            @Override
            public void onResponse(@NonNull Call<AccountDto> call,
                                 @NonNull Response<AccountDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccountDto createdAccount = response.body();

                    // Salva no banco local
                    executorService.execute(() -> {
                        try {
                            Account account = new Account(
                                createdAccount.getId(),
                                createdAccount.getName(),
                                createdAccount.getBalance(),
                                createdAccount.getCurrency(),
                                createdAccount.getDescription()
                            );
                            accountDao.insert(account);

                            Log.d(TAG, "Conta criada com sucesso: " + createdAccount.getId());
                            callback.onSuccess(createdAccount);
                        } catch (Exception e) {
                            Log.e(TAG, "Erro ao salvar conta no banco", e);
                            callback.onError("Erro ao salvar conta: " + e.getMessage());
                        }
                    });
                } else {
                    Log.d(TAG, "Erro ao criar conta: " + response.message());
                    callback.onError("Erro ao criar conta: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AccountDto> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha na criação de conta", t);
                callback.onError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    /**
     * Atualiza conta localmente
     */
    public void updateLocalAccount(@NonNull Account account) {
        executorService.execute(() -> {
            try {
                accountDao.update(account);
                Log.d(TAG, "Conta atualizada localmente: " + account.getId());
            } catch (Exception e) {
                Log.e(TAG, "Erro ao atualizar conta localmente", e);
            }
        });
    }

    /**
     * Remove conta
     */
    public void deleteAccount(@NonNull String accountId, @NonNull DeleteCallback callback) {
        Log.d(TAG, "Removendo conta: " + accountId);

        // Remove do banco local primeiro
        executorService.execute(() -> {
            try {
                accountDao.deleteById(accountId);
                Log.d(TAG, "Conta removida do banco local: " + accountId);
                callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao remover conta do banco", e);
                callback.onError("Erro ao remover conta: " + e.getMessage());
            }
        });
    }

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return "AccountRepository{" +
                "database=" + accountDao +
                '}';
    }

    /**
     * Interfaces para callbacks
     */
    public interface SyncCallback {
        void onSuccess(int count);
        void onError(String error);
    }

    public interface CreateCallback {
        void onSuccess(AccountDto account);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
