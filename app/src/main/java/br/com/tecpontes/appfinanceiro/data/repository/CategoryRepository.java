package br.com.tecpontes.appfinanceiro.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryRequestDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.ApiErrorUtils;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Operações de categorias (listagem, criação, edição e exclusão). */
@Singleton
public class CategoryRepository {

    private static final String TAG = "CategoryRepository";
    private final ApiService apiService;

    @Inject
    public CategoryRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Obtém categorias por tipo (INCOME ou EXPENSE).
     *
     * <p>A resposta da API já vem com as subcategorias aninhadas em {@code subCategories}.</p>
     */
    public void getCategories(@NonNull String type, @NonNull CategoriesCallback callback) {
        Log.d(TAG, "Buscando categorias do tipo: " + type);

        Call<List<CategoryDto>> call = apiService.getCategories(type);
        call.enqueue(new Callback<List<CategoryDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<CategoryDto>> call,
                                   @NonNull Response<List<CategoryDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Categorias carregadas: " + response.body().size());
                    callback.onSuccess(response.body());
                } else {
                    Log.e(TAG, "Erro na API: " + response.message());
                    callback.onError("Erro ao carregar categorias: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CategoryDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha na conexão", t);
                callback.onError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    public void getCategoriesByType(
            @NonNull String type,
            @NonNull ResultCallback<List<CategoryDto>> callback) {
        enqueue(apiService.getCategories(type), callback,
                "Não foi possível carregar as categorias");
    }

    public void createCategory(
            @NonNull CategoryRequestDto request,
            @NonNull ResultCallback<CategoryDto> callback) {
        enqueue(apiService.createCategory(request), callback,
                "Não foi possível criar a categoria");
    }

    public void updateCategory(
            @NonNull String id,
            @NonNull CategoryRequestDto request,
            @NonNull ResultCallback<CategoryDto> callback) {
        enqueue(apiService.updateCategory(id, request), callback,
                "Não foi possível atualizar a categoria");
    }

    public void deleteCategory(@NonNull String id, @NonNull ActionCallback callback) {
        enqueueAction(apiService.deleteCategory(id), callback,
                "Falha ao excluir categoria. Verifique se existem transações ou subcategorias associadas.");
    }

    public void createDefaultCategories(@NonNull ActionCallback callback) {
        enqueueAction(apiService.createDefaultCategories(), callback,
                "Não foi possível criar as categorias padrão");
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

    public interface CategoriesCallback {
        void onSuccess(List<CategoryDto> categories);
        void onError(String error);
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
