package br.com.tecpontes.appfinanceiro.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.ApiErrorUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Dados usados pela tela de relatórios. */
@Singleton
public class ReportRepository {

    public static final String TYPE_ALL = "ALL";

    private final ApiService apiService;

    @Inject
    public ReportRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public void getCategories(@NonNull ResultCallback<List<CategoryDto>> callback) {
        enqueue(apiService.getCategories(null), callback,
                "Não foi possível carregar as categorias");
    }

    public void getTransactions(
            @NonNull LocalDate from,
            @NonNull LocalDate to,
            @NonNull String type,
            @NonNull ResultCallback<List<TransactionListItemDto>> callback) {
        String startDate = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toString();
        String endDate = to.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toString();
        String typeParam = TYPE_ALL.equals(type) ? null : type;

        enqueue(apiService.getTransactions(null, startDate, endDate, typeParam, null, null, null),
                callback, "Não foi possível carregar as transações do relatório");
    }

    private <T> void enqueue(
            @NonNull Call<T> call,
            @NonNull ResultCallback<T> callback,
            @NonNull String failureMessage) {
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(@NonNull Call<T> ignored, @NonNull Response<T> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(ApiErrorUtils.readError(response, failureMessage));
                }
            }

            @Override public void onFailure(@NonNull Call<T> ignored, @NonNull Throwable error) {
                callback.onError("Erro de conexão: " + error.getMessage());
            }
        });
    }

    public interface ResultCallback<T> {
        void onSuccess(@Nullable T result);
        void onError(@NonNull String message);
    }

    /** Mantido para simetria com os demais repositórios, embora não seja usado aqui. */
    public interface ActionCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }
}
