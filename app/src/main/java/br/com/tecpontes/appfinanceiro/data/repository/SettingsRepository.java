package br.com.tecpontes.appfinanceiro.data.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryGoalDto;
import br.com.tecpontes.appfinanceiro.model.CategoryGoalRequestDto;
import br.com.tecpontes.appfinanceiro.model.ImportCategoryGoalsRequestDto;
import br.com.tecpontes.appfinanceiro.model.UserSettingsDto;
import br.com.tecpontes.appfinanceiro.model.UserSettingsUpdateRequestDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.ApiErrorUtils;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Configurações do usuário e metas por categoria. */
@Singleton
public class SettingsRepository {

    private final ApiService apiService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Inject
    public SettingsRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public void getSettings(@NonNull ResultCallback<UserSettingsDto> callback) {
        enqueue(apiService.getUserSettings(), callback,
                "Não foi possível carregar as configurações");
    }

    public void updateSettings(
            @NonNull UserSettingsUpdateRequestDto request,
            @NonNull ResultCallback<UserSettingsDto> callback) {
        enqueue(apiService.updateUserSettings(request), callback,
                "Não foi possível salvar as configurações");
    }

    public void getCategories(@NonNull ResultCallback<List<CategoryDto>> callback) {
        enqueue(apiService.getCategories(null), callback,
                "Não foi possível carregar as categorias");
    }

    public void getCategoryGoals(
            int referenceMonth,
            int referenceYear,
            @NonNull ResultCallback<List<CategoryGoalDto>> callback) {
        enqueue(apiService.getCategoryGoals(referenceMonth, referenceYear), callback,
                "Não foi possível carregar as metas por categoria");
    }

    public void saveCategoryGoals(
            @NonNull List<CategoryGoalRequestDto> goals,
            @NonNull ActionCallback callback) {
        new Thread(() -> {
            try {
                for (CategoryGoalRequestDto goal : goals) {
                    Response<CategoryGoalDto> response = apiService.upsertCategoryGoal(goal).execute();
                    if (!response.isSuccessful()) {
                        throw new IOException(ApiErrorUtils.readError(response,
                                "Não foi possível salvar as metas por categoria"));
                    }
                    ApiErrorUtils.closeBody(response.errorBody());
                }
                mainHandler.post(callback::onSuccess);
            } catch (Exception error) {
                String message = error.getMessage() == null
                        ? "Não foi possível salvar as metas por categoria"
                        : error.getMessage();
                mainHandler.post(() -> callback.onError(message));
            }
        }, "settings-category-goals").start();
    }

    public void importCategoryGoals(
            @NonNull ImportCategoryGoalsRequestDto request,
            @NonNull ActionCallback callback) {
        enqueueAction(apiService.importCategoryGoals(request), callback,
                "Não foi possível importar as metas");
    }

    /** Versão síncrona usada pelo Worker de notificações (executada em thread de background). */
    @NonNull
    public UserSettingsDto getSettingsBlocking() throws IOException {
        Response<UserSettingsDto> response = apiService.getUserSettings().execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException(ApiErrorUtils.readError(response,
                    "Não foi possível carregar as configurações"));
        }
        return response.body();
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

    private void enqueueAction(
            @NonNull Call<ResponseBody> call,
            @NonNull ActionCallback callback,
            @NonNull String failureMessage) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(
                    @NonNull Call<ResponseBody> ignored,
                    @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    ApiErrorUtils.closeBody(response.body());
                    callback.onSuccess();
                } else {
                    callback.onError(ApiErrorUtils.readError(response, failureMessage));
                }
            }

            @Override public void onFailure(
                    @NonNull Call<ResponseBody> ignored, @NonNull Throwable error) {
                callback.onError("Erro de conexão: " + error.getMessage());
            }
        });
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
