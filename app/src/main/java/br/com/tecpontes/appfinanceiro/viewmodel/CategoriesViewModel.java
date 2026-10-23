package br.com.tecpontes.appfinanceiro.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.repository.CategoryRepository;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryRequestDto;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Estado e ações da tela de categorias. */
@HiltViewModel
public class CategoriesViewModel extends ViewModel {

    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INCOME = "INCOME";

    private final CategoryRepository repository;

    private final MutableLiveData<List<CategoryDto>> expenseCategories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CategoryDto>> incomeCategories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> success = new MutableLiveData<>();

    public final LiveData<List<CategoryDto>> expenseCategoriesData = expenseCategories;
    public final LiveData<List<CategoryDto>> incomeCategoriesData = incomeCategories;
    public final LiveData<Boolean> isLoading = loading;
    public final LiveData<Boolean> isSaving = saving;
    public final LiveData<String> errorMessage = error;
    public final LiveData<String> successMessage = success;

    private int pendingLoads;

    @Inject
    public CategoriesViewModel(CategoryRepository repository) {
        this.repository = repository;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(null);
        pendingLoads = 2;
        loadType(TYPE_EXPENSE, expenseCategories);
        loadType(TYPE_INCOME, incomeCategories);
    }

    private void loadType(
            @NonNull String type,
            @NonNull MutableLiveData<List<CategoryDto>> target) {
        repository.getCategoriesByType(type,
                new CategoryRepository.ResultCallback<List<CategoryDto>>() {
                    @Override public void onSuccess(@Nullable List<CategoryDto> result) {
                        target.postValue(result == null ? new ArrayList<>() : result);
                        finishLoad();
                    }

                    @Override public void onError(@NonNull String message) {
                        error.postValue(message);
                        finishLoad();
                    }
                });
    }

    private void finishLoad() {
        pendingLoads--;
        if (pendingLoads <= 0) {
            pendingLoads = 0;
            loading.postValue(false);
        }
    }

    /**
     * Cria ou atualiza uma categoria. O callback permite que o diálogo permaneça aberto até
     * a resposta do servidor.
     */
    public void save(
            @Nullable CategoryDto editing,
            @NonNull CategoryRequestDto request,
            @NonNull ActionCallback callback) {
        saving.setValue(true);
        error.setValue(null);

        CategoryRepository.ResultCallback<CategoryDto> resultCallback =
                new CategoryRepository.ResultCallback<CategoryDto>() {
                    @Override public void onSuccess(@Nullable CategoryDto result) {
                        saving.postValue(false);
                        success.postValue(editing == null
                                ? "Categoria criada com sucesso!"
                                : "Categoria atualizada com sucesso!");
                        load();
                        callback.onSuccess();
                    }

                    @Override public void onError(@NonNull String message) {
                        saving.postValue(false);
                        error.postValue(message);
                        callback.onError(message);
                    }
                };

        if (editing != null && editing.getId() != null) {
            repository.updateCategory(editing.getId(), request, resultCallback);
        } else {
            repository.createCategory(request, resultCallback);
        }
    }

    public void delete(@NonNull CategoryDto category) {
        if (category.getId() == null) return;
        saving.setValue(true);
        error.setValue(null);
        repository.deleteCategory(category.getId(), new CategoryRepository.ActionCallback() {
            @Override public void onSuccess() {
                saving.postValue(false);
                success.postValue("Categoria excluída com sucesso!");
                load();
            }

            @Override public void onError(@NonNull String message) {
                saving.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void createDefaults() {
        saving.setValue(true);
        error.setValue(null);
        repository.createDefaultCategories(new CategoryRepository.ActionCallback() {
            @Override public void onSuccess() {
                saving.postValue(false);
                success.postValue("Categorias padrão criadas com sucesso!");
                load();
            }

            @Override public void onError(@NonNull String message) {
                saving.postValue(false);
                error.postValue(message);
            }
        });
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }
}
