package br.com.tecpontes.appfinanceiro.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.repository.CreditCardRepository;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.model.InvoiceDetailsDto;
import br.com.tecpontes.appfinanceiro.model.InvoicePaymentRequestDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Estado e operações da tela de cartões. */
@HiltViewModel
public class CreditCardsViewModel extends ViewModel {

    private final CreditCardRepository repository;
    private final MutableLiveData<List<CreditCardDto>> cards = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<InvoiceDetailsDto> invoice = new MutableLiveData<>();
    private final MutableLiveData<List<AccountDto>> accounts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CategoryDto>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public final LiveData<List<CreditCardDto>> cardsData = cards;
    public final LiveData<InvoiceDetailsDto> invoiceData = invoice;
    public final LiveData<List<AccountDto>> accountsData = accounts;
    public final LiveData<List<CategoryDto>> categoriesData = categories;
    public final LiveData<Boolean> isRefreshing = refreshing;
    public final LiveData<String> errorMessage = error;

    @Inject
    public CreditCardsViewModel(CreditCardRepository repository) {
        this.repository = repository;
    }

    public void loadCards() {
        refreshing.setValue(true);
        error.setValue(null);
        repository.getCards(new CreditCardRepository.ResultCallback<List<CreditCardDto>>() {
            @Override public void onSuccess(@Nullable List<CreditCardDto> result) {
                cards.postValue(result == null ? new ArrayList<>() : result);
                refreshing.postValue(false);
            }
            @Override public void onError(@NonNull String message) {
                error.postValue(message);
                refreshing.postValue(false);
            }
        });
    }

    public void loadInvoice(@NonNull String cardId, int year, int month) {
        refreshing.setValue(true);
        error.setValue(null);
        invoice.setValue(null);
        repository.getInvoice(cardId, year, month,
                new CreditCardRepository.ResultCallback<InvoiceDetailsDto>() {
                    @Override public void onSuccess(@Nullable InvoiceDetailsDto result) {
                        invoice.postValue(result);
                        refreshing.postValue(false);
                    }
                    @Override public void onError(@NonNull String message) {
                        invoice.postValue(null);
                        error.postValue(message);
                        refreshing.postValue(false);
                    }
                });
    }

    public void loadPaymentOptions() {
        repository.getAccounts(new CreditCardRepository.ResultCallback<List<AccountDto>>() {
            @Override public void onSuccess(@Nullable List<AccountDto> result) {
                accounts.postValue(result == null ? new ArrayList<>() : result);
            }
            @Override public void onError(@NonNull String message) { error.postValue(message); }
        });
        repository.getCategories(new CreditCardRepository.ResultCallback<List<CategoryDto>>() {
            @Override public void onSuccess(@Nullable List<CategoryDto> result) {
                categories.postValue(result == null ? new ArrayList<>() : result);
            }
            @Override public void onError(@NonNull String message) { error.postValue(message); }
        });
    }

    public void closeInvoice(@NonNull String id, @NonNull CreditCardRepository.ActionCallback callback) {
        repository.closeInvoice(id, callback);
    }

    public void reopenInvoice(@NonNull String id, @NonNull CreditCardRepository.ActionCallback callback) {
        repository.reopenInvoice(id, callback);
    }

    public void payInvoice(@NonNull String id, @NonNull InvoicePaymentRequestDto request,
                           @NonNull CreditCardRepository.ActionCallback callback) {
        repository.payInvoice(id, request, callback);
    }

    public void saveTransaction(@Nullable String id, @NonNull TransactionWriteDto transaction,
                                @NonNull CreditCardRepository.ActionCallback callback) {
        repository.saveTransaction(id, transaction, callback);
    }

    public void deleteTransaction(@NonNull String id, @Nullable String scope,
                                  @NonNull CreditCardRepository.ActionCallback callback) {
        repository.deleteTransaction(id, scope, callback);
    }

    public void setReconciled(@NonNull String id, boolean reconciled,
                              @NonNull CreditCardRepository.ActionCallback callback) {
        repository.setReconciled(id, reconciled, callback);
    }
}
