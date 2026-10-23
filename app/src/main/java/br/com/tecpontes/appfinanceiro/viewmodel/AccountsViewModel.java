package br.com.tecpontes.appfinanceiro.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.repository.AccountsRepository;
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
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Estado da lista de contas/cartões e ponte para as ações da tela. */
@HiltViewModel
public class AccountsViewModel extends ViewModel {
    private final AccountsRepository repository;
    private final MutableLiveData<List<AccountDto>> accounts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CreditCardDto>> cards = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ReconciliationSummaryDto> reconciliation = new MutableLiveData<>();
    private final MutableLiveData<AccountBalanceHistoryDto> balanceHistory = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private int pendingLoads;

    public final LiveData<List<AccountDto>> accountsData = accounts;
    public final LiveData<List<CreditCardDto>> cardsData = cards;
    public final LiveData<ReconciliationSummaryDto> reconciliationData = reconciliation;
    public final LiveData<AccountBalanceHistoryDto> balanceHistoryData = balanceHistory;
    public final LiveData<List<CategoryDto>> categoriesData = categories;
    public final LiveData<Boolean> isLoading = loading;
    public final LiveData<String> errorMessage = error;

    @Inject public AccountsViewModel(AccountsRepository repository) { this.repository = repository; }

    public void load(boolean includeInactive) {
        pendingLoads = 3;
        loading.setValue(true);
        error.setValue(null);
        repository.getAccounts(includeInactive, new AccountsRepository.ResultCallback<List<AccountDto>>() {
            @Override public void onSuccess(@Nullable List<AccountDto> value) {
                accounts.postValue(value == null ? new ArrayList<>() : value);
                loadFinished();
            }
            @Override public void onError(@NonNull String message) { error.postValue(message); loadFinished(); }
        });
        repository.getCards(includeInactive, new AccountsRepository.ResultCallback<List<CreditCardDto>>() {
            @Override public void onSuccess(@Nullable List<CreditCardDto> value) {
                cards.postValue(value == null ? new ArrayList<>() : value);
                loadFinished();
            }
            @Override public void onError(@NonNull String message) { error.postValue(message); loadFinished(); }
        });
        repository.getReconciliationSummary(new AccountsRepository.ResultCallback<ReconciliationSummaryDto>() {
            @Override public void onSuccess(@Nullable ReconciliationSummaryDto value) {
                reconciliation.postValue(value);
                loadFinished();
            }
            @Override public void onError(@NonNull String message) { loadFinished(); }
        });
    }

    private void loadFinished() {
        pendingLoads = Math.max(0, pendingLoads - 1);
        loading.postValue(pendingLoads > 0);
    }

    public void loadHistory(@NonNull String accountId) {
        balanceHistory.setValue(null);
        repository.getBalanceHistory(accountId, new AccountsRepository.ResultCallback<AccountBalanceHistoryDto>() {
            @Override public void onSuccess(@Nullable AccountBalanceHistoryDto value) { balanceHistory.postValue(value); }
            @Override public void onError(@NonNull String message) { error.postValue(message); }
        });
    }

    public void loadCategories() {
        repository.getCategories(new AccountsRepository.ResultCallback<List<CategoryDto>>() {
            @Override public void onSuccess(@Nullable List<CategoryDto> value) {
                categories.postValue(value == null ? new ArrayList<>() : value);
            }
            @Override public void onError(@NonNull String message) { error.postValue(message); }
        });
    }

    public void saveAccount(@Nullable String id, @NonNull AccountRequestDto request,
                            @NonNull AccountsRepository.ActionCallback callback) {
        repository.saveAccount(id, request, callback);
    }
    public void setAccountActive(@NonNull String id, boolean active,
                                 @NonNull AccountsRepository.ActionCallback callback) {
        repository.setAccountActive(id, active, callback);
    }
    public void deleteAccount(@NonNull String id, @NonNull AccountsRepository.ActionCallback callback) {
        repository.deleteAccount(id, callback);
    }
    public void saveCard(@Nullable String id, @NonNull CreditCardRequestDto request,
                         @NonNull AccountsRepository.ActionCallback callback) {
        repository.saveCard(id, request, callback);
    }
    public void setCardActive(@NonNull String id, boolean active,
                              @NonNull AccountsRepository.ActionCallback callback) {
        repository.setCardActive(id, active, callback);
    }
    public void deleteCard(@NonNull String id, @NonNull AccountsRepository.ActionCallback callback) {
        repository.deleteCard(id, callback);
    }
    public void recalculate(@NonNull AccountsRepository.ActionCallback callback) {
        repository.recalculateBalances(callback);
    }
    public void preview(@NonNull ReconciliationPreviewRequestDto request,
                        @NonNull AccountsRepository.ResultCallback<ReconciliationPreviewDto> callback) {
        repository.previewReconciliation(request, callback);
    }
    public void confirm(@NonNull ReconciliationConfirmRequestDto request,
                        @NonNull AccountsRepository.ResultCallback<ReconciliationConfirmResultDto> callback) {
        repository.confirmReconciliation(request, callback);
    }
}
