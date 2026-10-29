package br.com.tecpontes.appfinanceiro.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.data.local.LocalOptionsCache;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryRequestDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.ApiErrorUtils;
import br.com.tecpontes.appfinanceiro.utils.NetworkErrors;
import java.util.ArrayList;
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
    private final LocalOptionsCache optionsCache;

    @Inject
    public CategoryRepository(ApiService apiService, LocalOptionsCache optionsCache) {
        this.apiService = apiService;
        this.optionsCache = optionsCache;
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
                    optionsCache.saveCategoryType(type, response.body());
                    callback.onSuccess(response.body());
                } else if (response.code() >= 500) {
                    List<CategoryDto> cached = cachedCategories(type);
                    if (!cached.isEmpty()) callback.onSuccess(cached);
                    else callback.onError(cachedEmptyMessage(type));
                } else {
                    Log.e(TAG, "Erro na API: " + response.message());
                    callback.onError(ApiErrorUtils.readError(
                            response, "Não foi possível carregar as categorias. Tente novamente."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CategoryDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha na conexão", t);
                List<CategoryDto> cached = cachedCategories(type);
                if (!cached.isEmpty()) callback.onSuccess(cached);
                else callback.onError(NetworkErrors.message(t, cachedEmptyMessage(type)));
            }
        });
    }

    public void getCategoriesByType(
            @NonNull String type,
            @NonNull ResultCallback<List<CategoryDto>> callback) {
        apiService.getCategories(type).enqueue(new Callback<List<CategoryDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<CategoryDto>> ignored,
                    @NonNull Response<List<CategoryDto>> response) {
                if (response.isSuccessful()) {
                    List<CategoryDto> categories = response.body();
                    optionsCache.saveCategoryType(type, categories);
                    callback.onSuccess(categories == null ? new ArrayList<>() : categories);
                } else if (response.code() >= 500) {
                    fallbackCategories(type, callback);
                } else {
                    callback.onError(ApiErrorUtils.readError(
                            response, "Não foi possível carregar as categorias. Tente novamente."));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<CategoryDto>> ignored, @NonNull Throwable error) {
                fallbackCategories(type, callback);
            }
        });
    }

    private void fallbackCategories(
            @NonNull String type, @NonNull ResultCallback<List<CategoryDto>> callback) {
        List<CategoryDto> cached = cachedCategories(type);
        if (!cached.isEmpty()) callback.onSuccess(cached);
        else callback.onError(cachedEmptyMessage(type));
    }

    @NonNull
    private List<CategoryDto> cachedCategories(@Nullable String type) {
        List<CategoryDto> cached = new ArrayList<>();
        for (CategoryDto category : optionsCache.getCategories()) {
            if (category == null) continue;
            if (type == null || type.equalsIgnoreCase(category.getType())) cached.add(category);
        }
        return cached;
    }

    @NonNull
    private static String cachedEmptyMessage(@Nullable String type) {
        return "Conecte-se à internet para carregar suas categorias"
                + ("INCOME".equalsIgnoreCase(type) ? " de receita" : " de despesa")
                + ". Depois elas ficarão disponíveis offline.";
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
                callback.onError(NetworkErrors.message(
                        error, failureMessage + ". Tente novamente."));
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
