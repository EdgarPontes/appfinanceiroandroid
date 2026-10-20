package br.com.tecpontes.appfinanceiro.viewmodel;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.local.entity.Transaction;
import br.com.tecpontes.appfinanceiro.data.repository.TransactionRepository;
import br.com.tecpontes.appfinanceiro.model.TransactionDto;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;

/**
 * ViewModel para tela de transações
 */
@HiltViewModel
public class TransactionsViewModel extends ViewModel {

    private static final String TAG = "TransactionsViewModel";

    private final TransactionRepository transactionRepository;

    // LiveData para estados da UI
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public final LiveData<Boolean> loading = _loading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _syncSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> syncSuccess = _syncSuccess;

    private final MutableLiveData<Boolean> _createSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> createSuccess = _createSuccess;

    private final MutableLiveData<Boolean> _importSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> importSuccess = _importSuccess;

    // LiveData para dados
    private final MutableLiveData<List<Transaction>> _transactions = new MutableLiveData<>();
    public final LiveData<List<Transaction>> transactions = _transactions;

    private final MutableLiveData<List<Transaction>> _transactionsByAccount = new MutableLiveData<>();
    public final LiveData<List<Transaction>> transactionsByAccount = _transactionsByAccount;

    private final MutableLiveData<Transaction> _selectedTransaction = new MutableLiveData<>();
    public final LiveData<Transaction> selectedTransaction = _selectedTransaction;

    // Estado atual do filtro
    private String currentAccountId = null;

    @Inject
    public TransactionsViewModel(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
        Log.d(TAG, "TransactionsViewModel inicializado");

        // Observa todas as transações
        observeAllTransactions();
    }

    /**
     * Observa todas as transações
     */
    private void observeAllTransactions() {
        transactionRepository.getAllTransactions().observeForever(transactions -> {
            _transactions.setValue(transactions);
            Log.d(TAG, "Todas as transações atualizadas: " + (transactions != null ? transactions.size() : 0));
        });
    }

    /**
     * Carrega transações por conta
     */
    public void loadTransactionsByAccount(@NonNull String accountId) {
        Log.d(TAG, "Carregando transações para conta: " + accountId);
        currentAccountId = accountId;

        transactionRepository.getTransactionsByAccount(accountId).observeForever(transactions -> {
            _transactionsByAccount.setValue(transactions);
            Log.d(TAG, "Transações da conta atualizadas: " + (transactions != null ? transactions.size() : 0));
        });
    }

    /**
     * Sincroniza transações da API
     */
    public void syncTransactions() {
        if (currentAccountId == null) {
            Log.w(TAG, "Tentativa de sincronização sem conta selecionada");
            _error.setValue("Selecione uma conta primeiro");
            return;
        }

        Log.d(TAG, "Sincronizando transações para conta: " + currentAccountId);

        _loading.setValue(true);
        _error.setValue(null);

        transactionRepository.syncTransactions(currentAccountId, new TransactionRepository.SyncCallback() {
            @Override
            public void onSuccess(int count) {
                _loading.postValue(false);
                _syncSuccess.postValue(true);
                Log.d(TAG, "Transações sincronizadas com sucesso: " + count);
            }

            @Override
            public void onError(String error) {
                _loading.postValue(false);
                _error.postValue(error);
                Log.d(TAG, "Erro na sincronização: " + error);
            }
        });
    }

    /**
     * Cria nova transação
     */
    public void createTransaction(@NonNull TransactionDto transactionDto) {
        Log.d(TAG, "Criando nova transação: " + transactionDto.getAmount() + " - " + transactionDto.getCategory());

        _loading.setValue(true);
        _error.setValue(null);

        transactionRepository.createTransaction(transactionDto, new TransactionRepository.CreateCallback() {
            @Override
            public void onSuccess(TransactionDto transaction) {
                _loading.postValue(false);
                _createSuccess.postValue(true);
                Log.d(TAG, "Transação criada com sucesso: " + transaction.getId());
            }

            @Override
            public void onError(String error) {
                _loading.postValue(false);
                _error.postValue(error);
                Log.d(TAG, "Erro ao criar transação: " + error);
            }
        });
    }

    /**
     * Importa arquivo OFX
     */
    public void importOfx(@NonNull Uri fileUri, @NonNull String filePath) {
        Log.d(TAG, "Importando arquivo OFX: " + filePath);

        _loading.setValue(true);
        _error.setValue(null);

        transactionRepository.importOfx(fileUri, filePath, new TransactionRepository.ImportCallback() {
            @Override
            public void onSuccess(int count) {
                _loading.postValue(false);
                _importSuccess.postValue(true);
                Log.d(TAG, "OFX importado com sucesso: " + count + " transações");
            }

            @Override
            public void onError(String error) {
                _loading.postValue(false);
                _error.postValue(error);
                Log.d(TAG, "Erro na importação OFX: " + error);
            }
        });
    }

    /**
     * Remove transação
     */
    public void deleteTransaction(@NonNull String transactionId) {
        Log.d(TAG, "Removendo transação: " + transactionId);

        _loading.setValue(true);
        _error.setValue(null);

        transactionRepository.deleteTransaction(transactionId, new TransactionRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                _loading.postValue(false);
                Log.d(TAG, "Transação removida com sucesso");
                // Recarrega dados após remoção
                if (currentAccountId != null) {
                    loadTransactionsByAccount(currentAccountId);
                }
            }

            @Override
            public void onError(String error) {
                _loading.postValue(false);
                _error.postValue(error);
                Log.d(TAG, "Erro ao remover transação: " + error);
            }
        });
    }

    /**
     * Seleciona transação para edição
     */
    public void selectTransaction(@NonNull Transaction transaction) {
        Log.d(TAG, "Transação selecionada: " + transaction.getId());
        _selectedTransaction.setValue(transaction);
    }

    /**
     * Limpa seleção de transação
     */
    public void clearSelection() {
        Log.d(TAG, "Seleção de transação limpa");
        _selectedTransaction.setValue(null);
    }

    /**
     * Atualiza transação localmente
     */
    public void updateLocalTransaction(@NonNull Transaction transaction) {
        Log.d(TAG, "Atualizando transação localmente: " + transaction.getId());
        transactionRepository.updateLocalTransaction(transaction);
    }

    /**
     * Limpa estados de erro e sucesso
     */
    public void clearStates() {
        _error.setValue(null);
        _syncSuccess.setValue(false);
        _createSuccess.setValue(false);
        _importSuccess.setValue(false);
    }

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return "TransactionsViewModel{" +
                "loading=" + _loading.getValue() +
                ", hasError=" + (_error.getValue() != null) +
                ", currentAccountId=" + currentAccountId +
                ", allTransactionsCount=" + (_transactions.getValue() != null ? _transactions.getValue().size() : 0) +
                ", accountTransactionsCount=" + (_transactionsByAccount.getValue() != null ? _transactionsByAccount.getValue().size() : 0) +
                ", selectedTransaction=" + (_selectedTransaction.getValue() != null ? _selectedTransaction.getValue().getId() : "none") +
                '}';
    }

    /**
     * Obtém ID da conta atualmente selecionada
     */
    public String getCurrentAccountId() {
        return currentAccountId;
    }
}
