package br.com.tecpontes.appfinanceiro.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.data.local.LocalOptionsCache;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.model.InvoiceDetailsDto;
import br.com.tecpontes.appfinanceiro.model.InvoicePaymentRequestDto;
import br.com.tecpontes.appfinanceiro.model.InvoicePendingDto;
import br.com.tecpontes.appfinanceiro.model.InvoicePendingRequestDto;
import br.com.tecpontes.appfinanceiro.model.InvoicePendingResponseDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationRequestDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.NetworkErrors;
import dagger.hilt.android.scopes.ViewModelScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Comunicação com as rotas de cartões, faturas e lançamentos da fatura. */
@ViewModelScoped
public class CreditCardRepository {

    private final ApiService apiService;
    private final LocalOptionsCache optionsCache;
    private final PendingTransactionQueue pendingQueue;

    @Inject
    public CreditCardRepository(
            ApiService apiService,
            LocalOptionsCache optionsCache,
            PendingTransactionQueue pendingQueue) {
        this.apiService = apiService;
        this.optionsCache = optionsCache;
        this.pendingQueue = pendingQueue;
    }

    public void getCards(@NonNull ResultCallback<List<CreditCardDto>> callback) {
        apiService.getCreditCards().enqueue(new Callback<List<CreditCardDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<CreditCardDto>> ignored,
                    @NonNull Response<List<CreditCardDto>> response) {
                if (response.isSuccessful()) {
                    List<CreditCardDto> cards = response.body();
                    optionsCache.saveCards(cards);
                    callback.onSuccess(cards);
                } else if (response.code() >= 500) {
                    fallbackCards(callback);
                } else {
                    callback.onError("Não foi possível carregar os cartões: "
                            + response.message());
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<CreditCardDto>> ignored,
                    @NonNull Throwable error) {
                fallbackCards(callback);
            }
        });
    }

    private void fallbackCards(@NonNull ResultCallback<List<CreditCardDto>> callback) {
        List<CreditCardDto> cached = optionsCache.getCards();
        if (!cached.isEmpty()) callback.onSuccess(cached);
        else callback.onError(
                "Conecte-se à internet para carregar seus cartões. Depois eles ficarão disponíveis offline.");
    }

    public void getInvoice(
            @NonNull String cardId, int year, int month,
            @NonNull ResultCallback<InvoiceDetailsDto> callback) {
        enqueue(apiService.getInvoiceDetails(cardId, year, month), callback,
                "Não foi possível carregar a fatura");
    }

    public void getAccounts(@NonNull ResultCallback<List<AccountDto>> callback) {
        apiService.getAccounts().enqueue(new Callback<List<AccountDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<AccountDto>> ignored,
                    @NonNull Response<List<AccountDto>> response) {
                if (response.isSuccessful()) {
                    List<AccountDto> accounts = response.body();
                    optionsCache.saveAccounts(accounts);
                    callback.onSuccess(accounts);
                } else if (response.code() >= 500) {
                    fallbackAccounts(callback);
                } else {
                    callback.onError("Não foi possível carregar as contas: "
                            + response.message());
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<AccountDto>> ignored,
                    @NonNull Throwable error) {
                fallbackAccounts(callback);
            }
        });
    }

    private void fallbackAccounts(@NonNull ResultCallback<List<AccountDto>> callback) {
        List<AccountDto> cached = optionsCache.getAccounts();
        if (!cached.isEmpty()) callback.onSuccess(cached);
        else callback.onError(
                "Conecte-se à internet para carregar suas contas. Depois elas ficarão disponíveis offline.");
    }

    /** Busca o valor pendente de várias faturas de uma só vez. */
    public void getPendingInvoices(
            @NonNull List<String> invoiceIds,
            @NonNull ResultCallback<Map<String, InvoicePendingDto>> callback) {
        if (invoiceIds.isEmpty()) {
            callback.onSuccess(new LinkedHashMap<>());
            return;
        }
        apiService.getInvoicePending(new InvoicePendingRequestDto(invoiceIds))
                .enqueue(new Callback<InvoicePendingResponseDto>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<InvoicePendingResponseDto> ignored,
                            @NonNull Response<InvoicePendingResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().getPending());
                        } else if (response.isSuccessful()) {
                            callback.onSuccess(new LinkedHashMap<>());
                        } else {
                            callback.onError("Não foi possível carregar as faturas: "
                                    + response.message());
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<InvoicePendingResponseDto> ignored,
                            @NonNull Throwable error) {
                        callback.onError(NetworkErrors.message(
                                error, "Não foi possível carregar as faturas. Tente novamente."));
                    }
                });
    }

    public void getCategories(@NonNull ResultCallback<List<CategoryDto>> callback) {
        apiService.getCategories(null).enqueue(new Callback<List<CategoryDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<CategoryDto>> ignored,
                    @NonNull Response<List<CategoryDto>> response) {
                if (response.isSuccessful()) {
                    List<CategoryDto> categories = response.body();
                    optionsCache.saveCategories(categories);
                    callback.onSuccess(categories);
                } else if (response.code() >= 500) {
                    fallbackCategories(callback);
                } else {
                    callback.onError("Não foi possível carregar as categorias: "
                            + response.message());
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<CategoryDto>> ignored,
                    @NonNull Throwable error) {
                fallbackCategories(callback);
            }
        });
    }

    private void fallbackCategories(@NonNull ResultCallback<List<CategoryDto>> callback) {
        List<CategoryDto> cached = optionsCache.getCategories();
        if (!cached.isEmpty()) callback.onSuccess(cached);
        else callback.onError(
                "Conecte-se à internet para carregar suas categorias. Depois elas ficarão disponíveis offline.");
    }

    public void closeInvoice(@NonNull String invoiceId, @NonNull ActionCallback callback) {
        enqueueAction(apiService.closeInvoice(invoiceId), callback, "Não foi possível fechar a fatura");
    }

    public void reopenInvoice(@NonNull String invoiceId, @NonNull ActionCallback callback) {
        enqueueAction(apiService.reopenInvoice(invoiceId), callback, "Não foi possível reabrir a fatura");
    }

    public void payInvoice(
            @NonNull String invoiceId,
            @NonNull InvoicePaymentRequestDto payment,
            @NonNull ActionCallback callback) {
        enqueueAction(apiService.payInvoice(invoiceId, payment), callback,
                "Não foi possível registrar o pagamento");
    }

    public void saveTransaction(
            @Nullable String transactionId,
            @NonNull TransactionWriteDto transaction,
            @NonNull ActionCallback callback) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            pendingQueue.enqueue(transaction, new PendingTransactionQueue.Callback() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onError(@NonNull String error) {
                    callback.onError(error);
                }
            });
            return;
        }
        enqueueAction(apiService.updateTransaction(transactionId, transaction), callback,
                "Não foi possível salvar o lançamento");
    }

    public void deleteTransaction(
            @NonNull String transactionId, @Nullable String scope,
            @NonNull ActionCallback callback) {
        enqueueAction(apiService.deleteTransaction(transactionId, scope), callback,
                "Não foi possível excluir o lançamento");
    }

    public void setReconciled(
            @NonNull String transactionId, boolean reconciled,
            @NonNull ActionCallback callback) {
        enqueueAction(apiService.setTransactionReconciled(
                transactionId, new ReconciliationRequestDto(reconciled)), callback,
                "Não foi possível atualizar a conciliação");
    }

    private <T> void enqueue(
            @NonNull Call<T> call,
            @NonNull ResultCallback<T> callback,
            @NonNull String failureMessage) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> ignored, @NonNull Response<T> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(failureMessage + ": " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<T> ignored, @NonNull Throwable error) {
                callback.onError(NetworkErrors.message(
                        error, failureMessage + ". Tente novamente."));
            }
        });
    }

    private void enqueueAction(
            @NonNull Call<ResponseBody> call,
            @NonNull ActionCallback callback,
            @NonNull String failureMessage) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(
                    @NonNull Call<ResponseBody> ignored,
                    @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) callback.onSuccess();
                else callback.onError(failureMessage + ": " + response.message());
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> ignored, @NonNull Throwable error) {
                callback.onError(NetworkErrors.message(
                        error, failureMessage + ". Tente novamente."));
            }
        });
    }

    public interface ResultCallback<T> {
        void onSuccess(@Nullable T result);
        void onError(@NonNull String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(@NonNull String error);
    }
}
