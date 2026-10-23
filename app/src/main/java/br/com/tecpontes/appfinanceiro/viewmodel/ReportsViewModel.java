package br.com.tecpontes.appfinanceiro.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.repository.ReportRepository;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Estado e carregamento da tela de relatórios. */
@HiltViewModel
public class ReportsViewModel extends ViewModel {

    private final ReportRepository repository;

    private final MutableLiveData<List<CategoryDto>> categories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<TransactionListItemDto>> transactions =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public final LiveData<List<CategoryDto>> categoriesData = categories;
    public final LiveData<List<TransactionListItemDto>> transactionsData = transactions;
    public final LiveData<Boolean> isLoading = loading;
    public final LiveData<String> errorMessage = error;

    private boolean categoriesLoaded;
    private int requestId;
    private int pendingRequests;

    @Inject
    public ReportsViewModel(ReportRepository repository) {
        this.repository = repository;
    }

    public void load(
            @NonNull LocalDate from,
            @NonNull LocalDate to,
            @NonNull String type) {
        int currentRequest = ++requestId;
        loading.setValue(true);
        error.setValue(null);
        pendingRequests = categoriesLoaded ? 1 : 2;

        if (!categoriesLoaded) {
            repository.getCategories(new ReportRepository.ResultCallback<List<CategoryDto>>() {
                @Override public void onSuccess(@Nullable List<CategoryDto> result) {
                    if (currentRequest != requestId) return;
                    categoriesLoaded = true;
                    categories.postValue(result == null ? new ArrayList<>() : result);
                    finishRequest(currentRequest);
                }

                @Override public void onError(@NonNull String message) {
                    if (currentRequest != requestId) return;
                    error.postValue(message);
                    finishRequest(currentRequest);
                }
            });
        }

        repository.getTransactions(from, to, type,
                new ReportRepository.ResultCallback<List<TransactionListItemDto>>() {
                    @Override public void onSuccess(@Nullable List<TransactionListItemDto> result) {
                        if (currentRequest != requestId) return;
                        transactions.postValue(result == null
                                ? new ArrayList<>() : result);
                        finishRequest(currentRequest);
                    }

                    @Override public void onError(@NonNull String message) {
                        if (currentRequest != requestId) return;
                        error.postValue(message);
                        finishRequest(currentRequest);
                    }
                });
    }

    private void finishRequest(int currentRequest) {
        if (currentRequest != requestId) return;
        pendingRequests--;
        if (pendingRequests <= 0) {
            pendingRequests = 0;
            loading.postValue(false);
        }
    }
}
