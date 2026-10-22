package br.com.tecpontes.appfinanceiro.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository para operações com categorias
 */
@Singleton
public class CategoryRepository {

    private static final String TAG = "CategoryRepository";
    private final ApiService apiService;

    @Inject
    public CategoryRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Obtém categorias por tipo (INCOME ou EXPENSE)
     */
    public void getCategories(@NonNull String type, @NonNull CategoriesCallback callback) {
        Log.d(TAG, "Buscando categorias do tipo: " + type);

        Call<List<CategoryDto>> call = apiService.getCategories(type);
        call.enqueue(new Callback<List<CategoryDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<CategoryDto>> call, @NonNull Response<List<CategoryDto>> response) {
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

    public interface CategoriesCallback {
        void onSuccess(List<CategoryDto> categories);
        void onError(String error);
    }
}
