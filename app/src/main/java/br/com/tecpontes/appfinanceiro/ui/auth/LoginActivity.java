package br.com.tecpontes.appfinanceiro.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.data.repository.OptionsBootstrap;
import br.com.tecpontes.appfinanceiro.databinding.ActivityLoginBinding;
import br.com.tecpontes.appfinanceiro.ui.dashboard.DashboardActivity;
import br.com.tecpontes.appfinanceiro.notifications.PushRegistrationManager;
import br.com.tecpontes.appfinanceiro.sync.TransactionSyncScheduler;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.LoginViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/**
 * Tela de autenticação com abas de login e criação de conta.
 */
@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    public static final String EXTRA_SESSION_EXPIRED = "session_expired";

    private ActivityLoginBinding binding;
    private LoginViewModel loginViewModel;
    private boolean showingRegister;
    private boolean formattingCellphone;
    private boolean googleSignInInProgress;

    @Inject
    TokenManager tokenManager;

    @Inject
    PushRegistrationManager pushRegistrationManager;

    @Inject
    TransactionSyncScheduler transactionSyncScheduler;

    @Inject
    GoogleSignInClient googleSignInClient;

    @Inject
    OptionsBootstrap optionsBootstrap;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        boolean expiredSessionPending = tokenManager.consumeSessionExpiredEvent();
        if (getIntent().getBooleanExtra(EXTRA_SESSION_EXPIRED, false) || expiredSessionPending) {
            Toast.makeText(this, "Sua sessão expirou. Faça login novamente.", Toast.LENGTH_LONG).show();
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        if (isUserLoggedIn()) {
            navigateToDashboard();
            return;
        }

        setupObservers();
        setupListeners();
        showForm(false);
        updatePasswordStrength("");
        setupTermsText();

        Log.d(TAG, "LoginActivity criada");
    }

    private void setupObservers() {
        loginViewModel.loading.observe(this, loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.loginButton.setEnabled(!loading);
            binding.registerButton.setEnabled(!loading);
            binding.googleAuthButton.setEnabled(!loading);
            binding.loginTabButton.setEnabled(!loading);
            binding.registerTabButton.setEnabled(!loading);
            binding.keepConnectedCheckBox.setEnabled(!loading);
        });

        loginViewModel.error.observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        loginViewModel.emailError.observe(this, error -> {
            binding.loginEmailInputLayout.setError(showingRegister ? null : error);
            binding.registerEmailInputLayout.setError(showingRegister ? error : null);
        });

        loginViewModel.passwordError.observe(this, error -> {
            binding.loginPasswordInputLayout.setError(showingRegister ? null : error);
            binding.registerPasswordInputLayout.setError(showingRegister ? error : null);
        });

        loginViewModel.loginSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }
        });

        loginViewModel.registerSuccess.observe(this, success -> {
            if (!Boolean.TRUE.equals(success)) return;
            String registeredEmail = textOf(binding.registerEmailEditText).trim();
            Toast.makeText(this,
                    "Conta criada com sucesso! Faça login para continuar.",
                    Toast.LENGTH_LONG).show();
            loginViewModel.clearSuccessStates();
            binding.authTabGroup.check(R.id.loginTabButton);
            showForm(false);
            binding.loginEmailEditText.setText(registeredEmail);
            binding.registerPasswordEditText.setText("");
            binding.registerConfirmPasswordEditText.setText("");
            updatePasswordStrength("");
        });

        loginViewModel.forgotPasswordMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });

        loginViewModel.forgotPasswordError.observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupListeners() {
        binding.authTabGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                showForm(checkedId == R.id.registerTabButton);
            }
        });

        binding.googleAuthButton.setOnClickListener(v -> startGoogleSignIn());

        binding.loginButton.setOnClickListener(v -> loginViewModel.login(
                textOf(binding.loginEmailEditText).trim(),
                textOf(binding.loginPasswordEditText),
                binding.keepConnectedCheckBox.isChecked()));

        binding.registerButton.setOnClickListener(v -> submitRegistration());
        binding.forgotPasswordTextView.setOnClickListener(v -> openForgotPasswordDialog());

        TextWatcher clearErrorsWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loginViewModel.clearErrors();
                clearAllFieldErrors();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        binding.loginEmailEditText.addTextChangedListener(clearErrorsWatcher);
        binding.loginPasswordEditText.addTextChangedListener(clearErrorsWatcher);
        binding.registerNameEditText.addTextChangedListener(clearErrorsWatcher);
        binding.registerEmailEditText.addTextChangedListener(clearErrorsWatcher);
        binding.registerCellphoneEditText.addTextChangedListener(clearErrorsWatcher);
        binding.registerPasswordEditText.addTextChangedListener(clearErrorsWatcher);
        binding.registerConfirmPasswordEditText.addTextChangedListener(clearErrorsWatcher);

        binding.registerPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        setupCellphoneFormatter();
    }

    private void startGoogleSignIn() {
        if (googleSignInInProgress) return;
        googleSignInInProgress = true;
        binding.googleAuthButton.setEnabled(false);

        googleSignInClient.signIn(this, new GoogleSignInClient.Callback() {
            @Override
            public void onSuccess(@NonNull String idToken) {
                googleSignInInProgress = false;
                if (binding == null) return;
                loginViewModel.loginWithGoogle(idToken,
                        binding.keepConnectedCheckBox.isChecked());
            }

            @Override
            public void onCancelled() {
                googleSignInInProgress = false;
                if (binding == null) return;
                binding.googleAuthButton.setEnabled(true);
            }

            @Override
            public void onError(@NonNull String message) {
                googleSignInInProgress = false;
                if (binding == null) return;
                binding.googleAuthButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showForm(boolean register) {
        showingRegister = register;
        binding.loginFormContainer.setVisibility(register ? View.GONE : View.VISIBLE);
        binding.registerFormContainer.setVisibility(register ? View.VISIBLE : View.GONE);
        binding.googleAuthButton.setText(register
                ? "Inscrever-se no Google"
                : "Continuar com o Google");
        clearAllFieldErrors();
        loginViewModel.clearErrors();
        binding.loginScrollView.post(() -> binding.loginScrollView.smoothScrollTo(0, 0));
    }

    private void submitRegistration() {
        clearAllFieldErrors();
        loginViewModel.clearErrors();

        String name = textOf(binding.registerNameEditText).trim();
        String email = textOf(binding.registerEmailEditText).trim();
        String cellphone = textOf(binding.registerCellphoneEditText);
        String password = textOf(binding.registerPasswordEditText);
        String confirmPassword = textOf(binding.registerConfirmPasswordEditText);

        boolean valid = true;
        if (name.isEmpty()) {
            binding.registerNameInputLayout.setError("Informe seu nome");
            valid = false;
        }

        String digits = cellphone.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 11) {
            binding.registerCellphoneInputLayout.setError("Informe um celular válido com DDD");
            valid = false;
        }

        if (!confirmPassword.equals(password)) {
            binding.registerConfirmPasswordInputLayout.setError("As senhas não coincidem");
            valid = false;
        }

        if (!valid) return;

        loginViewModel.register(name, email, normalizeCellphone(digits), password);
    }

    private void setupCellphoneFormatter() {
        binding.registerCellphoneEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (formattingCellphone) return;
                String digits = s.toString().replaceAll("\\D", "");
                if (digits.length() > 11) digits = digits.substring(0, 11);
                String formatted = formatCellphone(digits);
                if (formatted.equals(s.toString())) return;
                formattingCellphone = true;
                binding.registerCellphoneEditText.setText(formatted);
                binding.registerCellphoneEditText.setSelection(formatted.length());
                formattingCellphone = false;
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void updatePasswordStrength(String password) {
        int level = 0;
        if (!password.isEmpty()) {
            boolean hasLetter = password.matches(".*[A-Za-zÀ-ÿ].*");
            boolean hasDigit = password.matches(".*\\d.*");
            if (password.length() >= 8 && hasLetter && hasDigit) {
                level = 3;
            } else if (password.length() >= 6) {
                level = 2;
            } else {
                level = 1;
            }
        }

        int emptyColor = getColor(R.color.outline);
        int filledColor;
        String label;
        if (level == 3) {
            filledColor = getColor(R.color.money_positive);
            label = "boa";
        } else if (level == 2) {
            filledColor = getColor(R.color.password_strength_medium);
            label = "média";
        } else if (level == 1) {
            filledColor = getColor(R.color.money_negative);
            label = "fraca";
        } else {
            filledColor = emptyColor;
            label = "";
        }

        binding.passwordStrengthBar1.setBackgroundTintList(
                ColorStateList.valueOf(level >= 1 ? filledColor : emptyColor));
        binding.passwordStrengthBar2.setBackgroundTintList(
                ColorStateList.valueOf(level >= 2 ? filledColor : emptyColor));
        binding.passwordStrengthBar3.setBackgroundTintList(
                ColorStateList.valueOf(level >= 3 ? filledColor : emptyColor));
        binding.passwordStrengthTextView.setText(label);
        binding.passwordStrengthTextView.setTextColor(level == 0
                ? getColor(R.color.on_surface_variant) : filledColor);
    }

    private void openForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.forgotEmailInputLayout);
        TextInputLayout cellphoneLayout = dialogView.findViewById(R.id.forgotCellphoneInputLayout);
        TextInputEditText emailInput = dialogView.findViewById(R.id.forgotEmailEditText);
        TextInputEditText cellphoneInput = dialogView.findViewById(R.id.forgotCellphoneEditText);

        String currentEmail = showingRegister
                ? textOf(binding.registerEmailEditText).trim()
                : textOf(binding.loginEmailEditText).trim();
        emailInput.setText(currentEmail);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Recuperar senha")
                .setView(dialogView)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    emailLayout.setError(null);
                    cellphoneLayout.setError(null);
                    String email = textOf(emailInput).trim();
                    String digits = textOf(cellphoneInput).replaceAll("\\D", "");
                    boolean valid = true;
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailLayout.setError("Informe um e-mail válido");
                        valid = false;
                    }
                    if (digits.length() < 10 || digits.length() > 11) {
                        cellphoneLayout.setError("Informe um celular válido com DDD");
                        valid = false;
                    }
                    if (!valid) return;
                    dialog.dismiss();
                    loginViewModel.requestPasswordReset(email, normalizeCellphone(digits));
                }));

        dialog.show();
    }

    private void setupTermsText() {
        String text = "Ao criar a conta você concorda com os Termos e a Privacidade.";
        SpannableString spannable = new SpannableString(text);
        int termsStart = text.indexOf("Termos");
        if (termsStart >= 0) {
            int end = termsStart + "Termos".length();
            spannable.setSpan(new UnderlineSpan(), termsStart, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(getColor(R.color.brand_primary)),
                    termsStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int privacyStart = text.indexOf("Privacidade");
        if (privacyStart >= 0) {
            int end = privacyStart + "Privacidade".length();
            spannable.setSpan(new UnderlineSpan(), privacyStart, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(getColor(R.color.brand_primary)),
                    privacyStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.termsTextView.setText(spannable);
    }

    private void clearAllFieldErrors() {
        binding.loginEmailInputLayout.setError(null);
        binding.loginPasswordInputLayout.setError(null);
        binding.registerNameInputLayout.setError(null);
        binding.registerEmailInputLayout.setError(null);
        binding.registerCellphoneInputLayout.setError(null);
        binding.registerPasswordInputLayout.setError(null);
        binding.registerConfirmPasswordInputLayout.setError(null);
    }

    private String formatCellphone(String digits) {
        if (digits.isEmpty()) return "";
        if (digits.length() <= 2) return "(" + digits;
        String area = digits.substring(0, 2);
        String rest = digits.substring(2);
        if (rest.length() <= 4) return "(" + area + ") " + rest;
        if (rest.length() <= 8) {
            return "(" + area + ") " + rest.substring(0, 4) + "-" + rest.substring(4);
        }
        return "(" + area + ") " + rest.substring(0, 5) + "-" + rest.substring(5);
    }

    private String normalizeCellphone(String digits) {
        if (digits.length() == 11) {
            String area = digits.substring(0, 2);
            String number = digits.substring(2);
            if (number.startsWith("9")) {
                return "55" + area + number.substring(1);
            }
            return "55" + digits;
        }
        return digits;
    }

    private static String textOf(TextInputEditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString();
    }

    private boolean isUserLoggedIn() {
        return loginViewModel.isUserLoggedIn();
    }

    private void navigateToDashboard() {
        pushRegistrationManager.requestSync();
        transactionSyncScheduler.requestSync();
        optionsBootstrap.refresh();
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
