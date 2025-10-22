package br.com.tecpontes.appfinanceiro.data.repository;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import br.com.tecpontes.appfinanceiro.data.local.dao.TransactionDao;
import br.com.tecpontes.appfinanceiro.data.local.entity.Transaction;
import br.com.tecpontes.appfinanceiro.model.TransactionDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository para operações com transações
 */
@Singleton
public class TransactionRepository {

    private static final String TAG = "TransactionRepository";

    private final ApiService apiService;
    private final TransactionDao transactionDao;
    private final ExecutorService executorService;

    @Inject
    public TransactionRepository(ApiService apiService, TransactionDao transactionDao) {
        this.apiService = apiService;
        this.transactionDao = transactionDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Obtém todas as transações
     */
    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    /**
     * Obtém transações por conta
     */
    public LiveData<List<Transaction>> getTransactionsByAccount(String accountId) {
        return transactionDao.getTransactionsByAccount(accountId);
    }

    /**
     * Obtém transação por ID
     */
    public LiveData<Transaction> getTransactionById(String transactionId) {
        return transactionDao.getTransactionById(transactionId);
    }

    /**
     * Obtém transações recentes (últimas 5)
     */
    public LiveData<List<Transaction>> getRecentTransactions() {
        return transactionDao.getRecentTransactions();
    }

    /**
     * Sincroniza transações da API para o banco local
     */
    public void syncTransactions(@NonNull String accountId, @NonNull SyncCallback callback) {
        Log.d(TAG, "Sincronizando transações para conta: " + accountId);

        Call<List<TransactionDto>> call = apiService.getTransactions(accountId, null, null);

        call.enqueue(new Callback<List<TransactionDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<TransactionDto>> call,
                                 @NonNull Response<List<TransactionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TransactionDto> transactionDtos = response.body();

                    // Converte DTOs para entidades e salva no banco
                    executorService.execute(() -> {
                        try {
                            for (TransactionDto dto : transactionDtos) {
                                Transaction transaction = new Transaction(
                                    dto.getId(),
                                    dto.getAccountId(),
                                    dto.getAmount(),
                                    dto.getDate(),
                                    dto.getCategory(),
                                    dto.getNote(),
                                    dto.getType()
                                );
                                transactionDao.insert(transaction);
                            }

                            Log.d(TAG, "Transações sincronizadas: " + transactionDtos.size());
                            callback.onSuccess(transactionDtos.size());
                        } catch (Exception e) {
                            Log.e(TAG, "Erro ao salvar transações", e);
                            callback.onError("Erro ao salvar: " + e.getMessage());
                        }
                    });
                } else {
                    Log.d(TAG, "Erro na API: " + response.message());
                    callback.onError("Erro na API: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TransactionDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha na sincronização", t);
                callback.onError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    /**
     * Cria nova transação
     */
    public void createTransaction(@NonNull TransactionDto transactionDto, @NonNull CreateCallback callback) {
        Log.d(TAG, "Criando transação: " + transactionDto.getAmount() + " - " + transactionDto.getCategory());

        Call<TransactionDto> call = apiService.createTransaction(transactionDto);

        call.enqueue(new Callback<TransactionDto>() {
            @Override
            public void onResponse(@NonNull Call<TransactionDto> call,
                                 @NonNull Response<TransactionDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TransactionDto createdTransaction = response.body();

                    // Salva no banco local
                    executorService.execute(() -> {
                        try {
                            Transaction transaction = new Transaction(
                                createdTransaction.getId(),
                                createdTransaction.getAccountId(),
                                createdTransaction.getAmount(),
                                createdTransaction.getDate(),
                                createdTransaction.getCategory(),
                                createdTransaction.getNote(),
                                createdTransaction.getType()
                            );
                            transactionDao.insert(transaction);

                            Log.d(TAG, "Transação criada: " + createdTransaction.getId());
                            callback.onSuccess(createdTransaction);
                        } catch (Exception e) {
                            Log.e(TAG, "Erro ao salvar transação", e);
                            callback.onError("Erro ao salvar: " + e.getMessage());
                        }
                    });
                } else {
                    Log.d(TAG, "Erro ao criar: " + response.message());
                    callback.onError("Erro ao criar: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TransactionDto> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha na criação", t);
                callback.onError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    /**
     * Importa arquivo OFX
     */
    public void importOfx(@NonNull Uri fileUri, @NonNull String filePath, @NonNull ImportCallback callback) {
        Log.d(TAG, "Importando arquivo OFX: " + filePath);

        try {
            File file = new File(fileUri.getPath());
            RequestBody requestFile = RequestBody.create(MediaType.parse("application/octet-stream"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            Call<List<TransactionDto>> call = apiService.importOfx(body);

            call.enqueue(new Callback<List<TransactionDto>>() {
                @Override
                public void onResponse(@NonNull Call<List<TransactionDto>> call,
                                     @NonNull Response<List<TransactionDto>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<TransactionDto> importedTransactions = response.body();

                        // Salva transações importadas no banco
                        executorService.execute(() -> {
                            try {
                                for (TransactionDto dto : importedTransactions) {
                                    Transaction transaction = new Transaction(
                                        dto.getId(),
                                        dto.getAccountId(),
                                        dto.getAmount(),
                                        dto.getDate(),
                                        dto.getCategory(),
                                        dto.getNote(),
                                        dto.getType()
                                    );
                                    transactionDao.insert(transaction);
                                }

                                Log.d(TAG, "OFX importado com sucesso: " + importedTransactions.size() + " transações");
                                callback.onSuccess(importedTransactions.size());
                            } catch (Exception e) {
                                Log.e(TAG, "Erro ao salvar transações do OFX", e);
                                callback.onError("Erro ao salvar: " + e.getMessage());
                            }
                        });
                    } else {
                        Log.d(TAG, "Erro na importação OFX: " + response.message());
                        callback.onError("Erro na importação: " + response.message());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<TransactionDto>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Falha na importação OFX", t);
                    callback.onError("Erro de conexão: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erro ao preparar arquivo OFX", e);
            callback.onError("Erro ao preparar arquivo: " + e.getMessage());
        }
    }

    /**
     * Atualiza transação localmente
     */
    public void updateLocalTransaction(@NonNull Transaction transaction) {
        executorService.execute(() -> {
            try {
                transactionDao.update(transaction);
                Log.d(TAG, "Transação atualizada localmente: " + transaction.getId());
            } catch (Exception e) {
                Log.e(TAG, "Erro ao atualizar transação", e);
            }
        });
    }

    /**
     * Remove transação
     */
    public void deleteTransaction(@NonNull String transactionId, @NonNull DeleteCallback callback) {
        Log.d(TAG, "Removendo transação: " + transactionId);

        executorService.execute(() -> {
            try {
                transactionDao.deleteById(transactionId);
                Log.d(TAG, "Transação removida: " + transactionId);
                callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao remover transação", e);
                callback.onError("Erro ao remover: " + e.getMessage());
            }
        });
    }

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return "TransactionRepository{" +
                "database=" + transactionDao +
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
        void onSuccess(TransactionDto transaction);
        void onError(String error);
    }

    public interface ImportCallback {
        void onSuccess(int count);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
