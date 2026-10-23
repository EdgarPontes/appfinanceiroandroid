package br.com.tecpontes.appfinanceiro.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.repository.SettingsRepository;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryGoalDto;
import br.com.tecpontes.appfinanceiro.model.CategoryGoalRequestDto;
import br.com.tecpontes.appfinanceiro.model.ImportCategoryGoalsRequestDto;
import br.com.tecpontes.appfinanceiro.model.UserSettingsDto;
import br.com.tecpontes.appfinanceiro.model.UserSettingsUpdateRequestDto;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Estado e ações da tela de configurações. */
@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private final SettingsRepository repository;

    private final MutableLiveData<UserSettingsDto> settings = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryDto>> categories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CategoryGoalDto>> categoryGoals =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> success = new MutableLiveData<>();

    public final LiveData<UserSettingsDto> settingsData = settings;
    public final LiveData<List<CategoryDto>> categoriesData = categories;
    public final LiveData<List<CategoryGoalDto>> categoryGoalsData = categoryGoals;
    public final LiveData<Boolean> isLoading = loading;
    public final LiveData<Boolean> isSaving = saving;
    public final LiveData<String> errorMessage = error;
    public final LiveData<String> successMessage = success;

    private int pendingLoads;

    @Inject
    public SettingsViewModel(SettingsRepository repository) {
        this.repository = repository;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(null);

        int month = java.time.LocalDate.now().getMonthValue();
        int year = java.time.LocalDate.now().getYear();

        pendingLoads = 3;

        repository.getSettings(new SettingsRepository.ResultCallback<UserSettingsDto>() {
            @Override public void onSuccess(@Nullable UserSettingsDto result) {
                settings.postValue(result);
                finishLoad();
            }

            @Override public void onError(@NonNull String message) {
                error.postValue(message);
                finishLoad();
            }
        });

        repository.getCategories(new SettingsRepository.ResultCallback<List<CategoryDto>>() {
            @Override public void onSuccess(@Nullable List<CategoryDto> result) {
                categories.postValue(result == null ? new ArrayList<>() : result);
                finishLoad();
            }

            @Override public void onError(@NonNull String message) {
                error.postValue(message);
                finishLoad();
            }
        });

        loadGoals(month, year);
    }

    public void reloadGoals() {
        int month = java.time.LocalDate.now().getMonthValue();
        int year = java.time.LocalDate.now().getYear();
        loadGoals(month, year);
    }

    private void loadGoals(int month, int year) {
        repository.getCategoryGoals(month, year,
                new SettingsRepository.ResultCallback<List<CategoryGoalDto>>() {
                    @Override public void onSuccess(@Nullable List<CategoryGoalDto> result) {
                        categoryGoals.postValue(result == null ? new ArrayList<>() : result);
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

    public void saveSettings(
            @NonNull UserSettingsUpdateRequestDto request,
            @NonNull String successMessage) {
        saving.setValue(true);
        error.setValue(null);
        repository.updateSettings(request,
                new SettingsRepository.ResultCallback<UserSettingsDto>() {
                    @Override public void onSuccess(@Nullable UserSettingsDto result) {
                        saving.postValue(false);
                        if (result != null) settings.postValue(result);
                        success.postValue(successMessage);
                    }

                    @Override public void onError(@NonNull String message) {
                        saving.postValue(false);
                        error.postValue(message);
                    }
                });
    }

    public void saveGoals(@NonNull List<CategoryGoalRequestDto> goals) {
        if (goals.isEmpty()) {
            success.setValue("Nada para salvar. Informe pelo menos uma meta maior ou igual a zero.");
            return;
        }
        saving.setValue(true);
        error.setValue(null);
        repository.saveCategoryGoals(goals, new SettingsRepository.ActionCallback() {
            @Override public void onSuccess() {
                saving.postValue(false);
                success.postValue("As metas por categoria foram atualizadas.");
                reloadGoals();
            }

            @Override public void onError(@NonNull String message) {
                saving.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void importGoals(@NonNull ImportCategoryGoalsRequestDto request) {
        saving.setValue(true);
        error.setValue(null);
        repository.importCategoryGoals(request, new SettingsRepository.ActionCallback() {
            @Override public void onSuccess() {
                saving.postValue(false);
                success.postValue("Metas importadas com sucesso.");
                reloadGoals();
            }

            @Override public void onError(@NonNull String message) {
                saving.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void clearMessages() {
        error.setValue(null);
    }
}
