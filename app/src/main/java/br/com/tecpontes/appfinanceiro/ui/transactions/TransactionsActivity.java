package br.com.tecpontes.appfinanceiro.ui.transactions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import br.com.tecpontes.appfinanceiro.databinding.ActivityTransactionsBinding;
import br.com.tecpontes.appfinanceiro.ui.dashboard.DashboardActivity;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.TransactionsViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import javax.inject.Inject;

/**
 * Activity para gerenciamento de transações
 */
@AndroidEntryPoint
public class TransactionsActivity extends AppCompatActivity {

    private static final String TAG = "TransactionsActivity";

    private ActivityTransactionsBinding binding;
    private TransactionsViewModel transactionsViewModel;

    @Inject
    TokenManager tokenManager;

    // Adapter para lista de transações
    private TransactionsAdapter transactionsAdapter;

    // Launcher para seleção de arquivo OFX
    private ActivityResultLauncher<String[]> ofxFilePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configura Edge-to-Edge
        EdgeToEdge.enable(this);

        // Infla layout com ViewBinding
        binding = ActivityTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configura window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa ViewModel
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);

        // Configura RecyclerView
        setupRecyclerView();

        // Configura SwipeRefreshLayout
        setupSwipeRefresh();

        // Configura file picker para OFX
        setupOfxFilePicker();

        // Configura observadores
        setupObservers();

        // Configura listeners
        setupListeners();

        // Carrega dados iniciais
        loadInitialData();

        Log.d(TAG, "TransactionsActivity criada");
    }

    /**
     * Configura RecyclerView para transações
     */
    private void setupRecyclerView() {
        transactionsAdapter = new TransactionsAdapter(new ArrayList<>(), this::onTransactionEdit, this::onTransactionDelete);
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.transactionsRecyclerView.setAdapter(transactionsAdapter);
    }

    /**
     * Configura SwipeRefreshLayout
     */
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            transactionsViewModel.syncTransactions();
        });
    }

    /**
     * Configura file picker para importação OFX
     */
    private void setupOfxFilePicker() {
        ofxFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::handleOfxFileSelection
        );
    }

    /**
     * Configura observadores do ViewModel
     */
    private void setupObservers() {
        // Observa estado de loading
        transactionsViewModel.loading.observe(this, loading -> {
            binding.swipeRefreshLayout.setRefreshing(loading);
            binding.loadingView.getRoot().setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        // Observa erros
        transactionsViewModel.error.observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        // Observa sucesso na sincronização
        transactionsViewModel.syncSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Transações sincronizadas com sucesso!", Toast.LENGTH_SHORT).show();
            }
        });

        // Observa sucesso na criação
        transactionsViewModel.createSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Transação criada com sucesso!", Toast.LENGTH_SHORT).show();
                // Recarrega dados
                loadInitialData();
            }
        });

        // Observa sucesso na importação
        transactionsViewModel.importSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Arquivo OFX importado com sucesso!", Toast.LENGTH_SHORT).show();
                // Recarrega dados
                loadInitialData();
            }
        });

        // Observa transações
        transactionsViewModel.transactionsByAccount.observe(this, transactions -> {
            if (transactions != null) {
                transactionsAdapter.updateTransactions(transactions);
                updateEmptyState(transactions.isEmpty());
            }
        });
    }

    /**
     * Configura listeners dos componentes
     */
    private void setupListeners() {
        // Botão de voltar
        binding.backButton.setOnClickListener(v -> {
            finish();
        });

        // Botão de adicionar transação
        binding.addTransactionFab.setOnClickListener(v -> {
            showCreateTransactionDialog();
        });

        // Botão de importar OFX
        binding.importOfxButton.setOnClickListener(v -> {
            openOfxFilePicker();
        });

        // Botão de sincronizar
        binding.syncButton.setOnClickListener(v -> {
            transactionsViewModel.syncTransactions();
        });
    }

    /**
     * Carrega dados iniciais
     */
    private void loadInitialData() {
        // Para demonstração, usa uma conta padrão
        // Em produção, isso viria da seleção de conta ou parâmetro
        String accountId = "default_account";
        transactionsViewModel.loadTransactionsByAccount(accountId);
    }

    /**
     * Manipula seleção de arquivo OFX
     */
    private void handleOfxFileSelection(Uri uri) {
        if (uri != null) {
            Log.d(TAG, "Arquivo OFX selecionado: " + uri.getPath());
            transactionsViewModel.importOfx(uri, uri.getPath());
        }
    }

    /**
     * Abre file picker para seleção de arquivo OFX
     */
    private void openOfxFilePicker() {
        ofxFilePickerLauncher.launch(new String[]{"application/octet-stream", "text/xml", "text/plain"});
    }

    /**
     * Mostra diálogo para criar nova transação
     */
    private void showCreateTransactionDialog() {
        CreateTransactionDialog dialog = new CreateTransactionDialog(
            this,
            (amount, category, note, type) -> {
                // Cria nova transação
                // Para demonstração, usa conta padrão
                String accountId = "default_account";
                long currentTime = System.currentTimeMillis();

                // TODO: Implementar criação completa de transação
                Toast.makeText(this, "Funcionalidade será implementada", Toast.LENGTH_SHORT).show();
            }
        );
        dialog.show();
    }

    /**
     * Callback para edição de transação
     */
    private void onTransactionEdit(br.com.tecpontes.appfinanceiro.data.local.entity.Transaction transaction) {
        Log.d(TAG, "Editando transação: " + transaction.getId());
        transactionsViewModel.selectTransaction(transaction);

        // TODO: Mostrar diálogo de edição
        Toast.makeText(this, "Editar: " + transaction.getCategory(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback para exclusão de transação
     */
    private void onTransactionDelete(String transactionId) {
        Log.d(TAG, "Excluindo transação: " + transactionId);

        // Mostra diálogo de confirmação
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirmar exclusão")
            .setMessage("Tem certeza que deseja excluir esta transação?")
            .setPositiveButton("Excluir", (dialog, which) -> {
                transactionsViewModel.deleteTransaction(transactionId);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    /**
     * Atualiza estado vazio da lista
     */
    private void updateEmptyState(boolean isEmpty) {
        binding.transactionsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.emptyStateTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "TransactionsActivity destruída");
    }
}
