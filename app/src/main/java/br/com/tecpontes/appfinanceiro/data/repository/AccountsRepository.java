package br.com.tecpontes.appfinanceiro.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.AccountBalanceHistoryDto;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.AccountRequestDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardRequestDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationConfirmRequestDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationConfirmResultDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationPreviewDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationPreviewRequestDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationSummaryDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import dagger.hilt.android.scopes.ViewModelScoped;
import java.util.List;
import javax.inject.Inject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Operações remotas da página Contas. */
@ViewModelScoped
public class AccountsRepository {

    private final ApiService apiService;

    @Inject
    public AccountsRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public void getAccounts(boolean includeInactive, @NonNull ResultCallback<List<AccountDto>> callback) {
        enqueue(apiService.getAccountsWithInactive(Boolean.toString(includeInactive)), callback,
                "Não foi possível carregar as contas");
    }

    public void getCards(boolean includeInactive, @NonNull ResultCallback<List<CreditCardDto>> callback) {
        enqueue(apiService.getCreditCardsWithInactive(Boolean.toString(includeInactive)), callback,
                "Não foi possível carregar os cartões");
    }

    public void getReconciliationSummary(@NonNull ResultCallback<ReconciliationSummaryDto> callback) {
        enqueue(apiService.getReconciliationSummary(), callback,
                "Não foi possível carregar o resumo de conciliação");
    }

    public void getBalanceHistory(@NonNull String accountId,
                                  @NonNull ResultCallback<AccountBalanceHistoryDto> callback) {
        enqueue(apiService.getAccountBalanceHistory(accountId), callback,
                "Não foi possível carregar o histórico de saldo");
    }

    public void getCategories(@NonNull ResultCallback<List<CategoryDto>> callback) {
        enqueue(apiService.getCategories(null), callback, "Não foi possível carregar as categorias");
    }

    public void saveAccount(@Nullable String id, @NonNull AccountRequestDto request,
                            @NonNull ActionCallback callback) {
        Call<ResponseBody> call = id == null || id.trim().isEmpty()
                ? apiService.createManagedAccount(request)
                : apiService.updateManagedAccount(id, request);
        enqueueAction(call, callback, "Não foi possível salvar a conta");
    }

    public void setAccountActive(@NonNull String id, boolean active, @NonNull ActionCallback callback) {
        AccountRequestDto request = new AccountRequestDto();
        request.isActive = active;
        enqueueAction(apiService.updateManagedAccount(id, request), callback,
                active ? "Não foi possível reativar a conta" : "Não foi possível inativar a conta");
    }

    public void deleteAccount(@NonNull String id, @NonNull ActionCallback callback) {
        enqueueAction(apiService.deleteManagedAccount(id), callback, "Não foi possível excluir a conta");
    }

    public void saveCard(@Nullable String id, @NonNull CreditCardRequestDto request,
                         @NonNull ActionCallback callback) {
        Call<ResponseBody> call = id == null || id.trim().isEmpty()
                ? apiService.createCreditCard(request)
                : apiService.updateCreditCard(id, request);
        enqueueAction(call, callback, "Não foi possível salvar o cartão");
    }

    public void setCardActive(@NonNull String id, boolean active, @NonNull ActionCallback callback) {
        CreditCardRequestDto request = new CreditCardRequestDto();
        request.isActive = active;
        enqueueAction(apiService.updateCreditCard(id, request), callback,
                active ? "Não foi possível reativar o cartão" : "Não foi possível inativar o cartão");
    }

    public void deleteCard(@NonNull String id, @NonNull ActionCallback callback) {
        enqueueAction(apiService.deleteCreditCard(id), callback, "Não foi possível excluir o cartão");
    }

    public void recalculateBalances(@NonNull ActionCallback callback) {
        enqueueAction(apiService.recalculateBalances(), callback,
                "Não foi possível recalcular os saldos e as faturas");
    }

    public void previewReconciliation(@NonNull ReconciliationPreviewRequestDto request,
                                      @NonNull ResultCallback<ReconciliationPreviewDto> callback) {
        enqueue(apiService.previewReconciliation(request), callback,
                "Não foi possível gerar a prévia de conciliação");
    }

    public void confirmReconciliation(@NonNull ReconciliationConfirmRequestDto request,
                                      @NonNull ResultCallback<ReconciliationConfirmResultDto> callback) {
        enqueue(apiService.confirmReconciliation(request), callback,
                "Não foi possível confirmar a conciliação");
    }

    private <T> void enqueue(@NonNull Call<T> call, @NonNull ResultCallback<T> callback,
                             @NonNull String failureMessage) {
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(@NonNull Call<T> ignored, @NonNull Response<T> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(readError(response, failureMessage));
            }
            @Override public void onFailure(@NonNull Call<T> ignored, @NonNull Throwable error) {
                callback.onError("Erro de conexão: " + error.getMessage());
            }
        });
    }

    private void enqueueAction(@NonNull Call<ResponseBody> call, @NonNull ActionCallback callback,
                               @NonNull String failureMessage) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(@NonNull Call<ResponseBody> ignored,
                                            @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) callback.onSuccess();
                else callback.onError(readError(response, failureMessage));
            }
            @Override public void onFailure(@NonNull Call<ResponseBody> ignored, @NonNull Throwable error) {
                callback.onError("Erro de conexão: " + error.getMessage());
            }
        });
    }

    private String readError(Response<?> response, String fallback) {
        if (response.errorBody() != null) {
            try {
                String body = response.errorBody().string();
                try {
                    org.json.JSONObject json = new org.json.JSONObject(body);
                    String message = json.optString("message");
                    if (!message.isEmpty()) return message;
                } catch (org.json.JSONException ignored) { }
                if (!body.trim().isEmpty()) return body;
            } catch (Exception ignored) { }
        }
        return fallback + ": " + response.message();
    }

    public interface ResultCallback<T> {
        void onSuccess(@Nullable T result);
        void onError(@NonNull String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }
}
