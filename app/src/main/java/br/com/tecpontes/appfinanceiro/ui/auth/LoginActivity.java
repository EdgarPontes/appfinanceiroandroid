package br.com.tecpontes.appfinanceiro.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import br.com.tecpontes.appfinanceiro.databinding.ActivityLoginBinding;
import br.com.tecpontes.appfinanceiro.ui.dashboard.DashboardActivity;
import br.com.tecpontes.appfinanceiro.viewmodel.LoginViewModel;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity para tela de login
 */
@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configura Edge-to-Edge
        EdgeToEdge.enable(this);

        // Infla layout com ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configura window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa ViewModel
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Verifica se usuário já está logado
        if (isUserLoggedIn()) {
            navigateToDashboard();
            return;
        }

        // Configura observadores
        setupObservers();

        // Configura listeners
        setupListeners();

        Log.d(TAG, "LoginActivity criada");
    }

    /**
     * Configura observadores do ViewModel
     */
    private void setupObservers() {
        // Observa estado de loading
        loginViewModel.loading.observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.loginButton.setEnabled(!loading);
            binding.registerButton.setEnabled(!loading);
        });

        // Observa erros
        loginViewModel.error.observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        // Observa erros de validação do email
        loginViewModel.emailError.observe(this, error -> {
            binding.emailInputLayout.setError(error);
        });

        // Observa erros de validação da senha
        loginViewModel.passwordError.observe(this, error -> {
            binding.passwordInputLayout.setError(error);
        });

        // Observa sucesso no login
        loginViewModel.loginSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }
        });

        // Observa sucesso no registro
        loginViewModel.registerSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Registro realizado com sucesso!", Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }
        });
    }

    /**
     * Configura listeners dos componentes
     */
    private void setupListeners() {
        // Listener para botão de login
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            loginViewModel.login(email, password);
        });

        // Listener para botão de registro
        binding.registerButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            loginViewModel.register(email, password);
        });

        // TextWatcher para limpar erros ao digitar
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loginViewModel.clearErrors();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.emailEditText.addTextChangedListener(textWatcher);
        binding.passwordEditText.addTextChangedListener(textWatcher);
    }

    /**
     * Verifica se usuário já está logado
     */
    private boolean isUserLoggedIn() {
        return loginViewModel.isUserLoggedIn();
    }

    /**
     * Navega para tela do dashboard
     */
    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "LoginActivity destruída");
    }
}
