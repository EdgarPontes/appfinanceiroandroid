package br.com.tecpontes.appfinanceiro.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    @Inject
    public CreditCardRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public void getCards(@NonNull ResultCallback<List<CreditCardDto>> callback) {
        enqueue(apiService.getCreditCards(), callback, "Não foi possível carregar os cartões");
    }

    public void getInvoice(
            @NonNull String cardId, int year, int month,
            @NonNull ResultCallback<InvoiceDetailsDto> callback) {
        enqueue(apiService.getInvoiceDetails(cardId, year, month), callback,
                "Não foi possível carregar a fatura");
    }

    public void getAccounts(@NonNull ResultCallback<List<AccountDto>> callback) {
        enqueue(apiService.getAccounts(), callback, "Não foi possível carregar as contas");
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
                        callback.onError("Erro de conexão ao carregar as faturas: "
                                + error.getMessage());
                    }
                });
    }

    public void getCategories(@NonNull ResultCallback<List<CategoryDto>> callback) {
        enqueue(apiService.getCategories(null), callback, "Não foi possível carregar as categorias");
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
        Call<ResponseBody> call = transactionId == null || transactionId.trim().isEmpty()
                ? apiService.createTransactionFromList(transaction)
                : apiService.updateTransaction(transactionId, transaction);
        enqueueAction(call, callback, "Não foi possível salvar o lançamento");
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
                callback.onError("Erro de conexão: " + error.getMessage());
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
                callback.onError("Erro de conexão: " + error.getMessage());
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
