package br.com.tecpontes.appfinanceiro.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import br.com.tecpontes.appfinanceiro.databinding.ActivityDashboardBinding;
import br.com.tecpontes.appfinanceiro.ui.auth.LoginActivity;
import br.com.tecpontes.appfinanceiro.ui.transactions.TransactionsActivity;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.DashboardViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import javax.inject.Inject;

/**
 * Activity para tela do dashboard
 */
@AndroidEntryPoint
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private ActivityDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;

    @Inject
    TokenManager tokenManager;

    // Adapter para transações recentes
    private RecentTransactionsAdapter transactionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configura Edge-to-Edge
        EdgeToEdge.enable(this);

        // Infla layout com ViewBinding
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configura window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa ViewModel
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Configura RecyclerView
        setupRecyclerView();

        // Configura SwipeRefreshLayout
        setupSwipeRefresh();

        // Configura observadores
        setupObservers();

        // Configura listeners
        setupListeners();

        // Carrega dados iniciais
        loadInitialData();

        Log.d(TAG, "DashboardActivity criada");
    }

    /**
     * Configura RecyclerView para transações recentes
     */
    private void setupRecyclerView() {
        transactionsAdapter = new RecentTransactionsAdapter(new ArrayList<>());
        binding.recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recentTransactionsRecyclerView.setAdapter(transactionsAdapter);
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
            dashboardViewModel.refreshData();
        });
    }

    /**
     * Configura observadores do ViewModel
     */
    private void setupObservers() {
        // Observa estado de loading
        dashboardViewModel.loading.observe(this, loading -> {
            binding.swipeRefreshLayout.setRefreshing(loading);
            binding.loadingView.getRoot().setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        // Observa erros
        dashboardViewModel.error.observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        // Observa sucesso na atualização
        dashboardViewModel.refreshSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show();
            }
        });

        // Observa saldo total
        dashboardViewModel.totalBalance.observe(this, balance -> {
            if (balance != null) {
                updateBalanceDisplay(balance);
            }
        });

        // Observa transações recentes
        dashboardViewModel.recentTransactions.observe(this, transactions -> {
            if (transactions != null) {
                transactionsAdapter.updateTransactions(transactions);
                updateTransactionsVisibility(transactions.isEmpty());
            }
        });

        // Observa contas
        dashboardViewModel.accounts.observe(this, accounts -> {
            if (accounts != null) {
                updateAccountsInfo(accounts.size());
            }
        });
    }

    /**
     * Configura listeners dos componentes
     */
    private void setupListeners() {
        // Botão de ver todas as transações
        binding.viewAllTransactionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionsActivity.class);
            startActivity(intent);
        });

        // Botão de sincronizar
        binding.syncButton.setOnClickListener(v -> {
            dashboardViewModel.syncAccounts();
        });

        // Card de adicionar transação
        binding.addTransactionCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Carrega dados iniciais
     */
    private void loadInitialData() {
        dashboardViewModel.loadDashboardData();
    }

    /**
     * Atualiza display do saldo
     */
    private void updateBalanceDisplay(double balance) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        String formattedBalance = currencyFormat.format(balance);

        binding.balanceTextView.setText(formattedBalance);

        // Animação simples de fade-in (opcional)
        binding.balanceTextView.setAlpha(0f);
        binding.balanceTextView.animate().alpha(1f).setDuration(500).start();
    }

    /**
     * Atualiza informações das contas
     */
    private void updateAccountsInfo(int accountsCount) {
        String accountsText = accountsCount + " conta" + (accountsCount != 1 ? "s" : "");
        binding.accountsInfoTextView.setText(accountsText);
    }

    /**
     * Atualiza visibilidade da seção de transações
     */
    private void updateTransactionsVisibility(boolean isEmpty) {
        binding.recentTransactionsSection.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.emptyTransactionsTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    /**
     * Realiza logout
     */
    private void performLogout() {
        Log.d(TAG, "Realizando logout");

        // Limpa token
        tokenManager.clearToken();

        // Navega para tela de login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "DashboardActivity destruída");
    }
}
