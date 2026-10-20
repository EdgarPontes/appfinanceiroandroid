package br.com.tecpontes.appfinanceiro.viewmodel;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.repository.AuthRepository;
import br.com.tecpontes.appfinanceiro.model.AuthResponse;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/**
 * ViewModel para tela de login
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private static final String TAG = "LoginViewModel";

    private final AuthRepository authRepository;

    // LiveData para estados da UI
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public final LiveData<Boolean> loading = _loading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _loginSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> loginSuccess = _loginSuccess;

    private final MutableLiveData<Boolean> _registerSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> registerSuccess = _registerSuccess;

    // Validação de formulário
    private final MutableLiveData<String> _emailError = new MutableLiveData<>();
    public final LiveData<String> emailError = _emailError;

    private final MutableLiveData<String> _passwordError = new MutableLiveData<>();
    public final LiveData<String> passwordError = _passwordError;

    @Inject
    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
        Log.d(TAG, "LoginViewModel inicializado");
    }

    /**
     * Realiza login do usuário
     */
    public void login(@NonNull String email, @NonNull String password) {
        Log.d(TAG, "Tentando login para: " + email);

        // Valida formulário
        if (!validateForm(email, password)) {
            return;
        }

        // Define estado de loading
        _loading.setValue(true);
        _error.setValue(null);

        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                _loading.postValue(false);
                _loginSuccess.postValue(true);
                Log.d(TAG, "Login realizado com sucesso");
            }

            @Override
            public void onError(String error) {
                _loading.postValue(false);
                _error.postValue(error);
                Log.d(TAG, "Erro no login: " + error);
            }
        });
    }

    /**
     * Realiza registro do usuário
     */
    public void register(@NonNull String email, @NonNull String password) {
        Log.d(TAG, "Tentando registro para: " + email);

        // Valida formulário
        if (!validateForm(email, password)) {
            return;
        }

        // Define estado de loading
        _loading.setValue(true);
        _error.setValue(null);

        authRepository.register(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                _loading.postValue(false);
                _registerSuccess.postValue(true);
                Log.d(TAG, "Registro realizado com sucesso");
            }

            @Override
            public void onError(String error) {
                _loading.postValue(false);
                _error.postValue(error);
                Log.d(TAG, "Erro no registro: " + error);
            }
        });
    }

    /**
     * Valida formulário de login/registro
     */
    private boolean validateForm(@NonNull String email, @NonNull String password) {
        boolean isValid = true;

        // Valida email
        if (email.trim().isEmpty()) {
            _emailError.setValue("Email é obrigatório");
            isValid = false;
        } else if (!isValidEmail(email)) {
            _emailError.setValue("Email inválido");
            isValid = false;
        } else {
            _emailError.setValue(null);
        }

        // Valida senha
        if (password.trim().isEmpty()) {
            _passwordError.setValue("Senha é obrigatória");
            isValid = false;
        } else if (password.length() < 6) {
            _passwordError.setValue("Senha deve ter pelo menos 6 caracteres");
            isValid = false;
        } else {
            _passwordError.setValue(null);
        }

        return isValid;
    }

    /**
     * Valida formato de email
     */
    private boolean isValidEmail(@NonNull String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Limpa erros de validação
     */
    public void clearErrors() {
        _emailError.setValue(null);
        _passwordError.setValue(null);
        _error.setValue(null);
    }

    /**
     * Limpa estados de sucesso
     */
    public void clearSuccessStates() {
        _loginSuccess.setValue(false);
        _registerSuccess.setValue(false);
    }

    /**
     * Verifica se usuário já está logado
     */
    public boolean isUserLoggedIn() {
        return authRepository.isLoggedIn();
    }

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return "LoginViewModel{" +
                "loading=" + _loading.getValue() +
                ", hasError=" + (_error.getValue() != null) +
                ", isLoggedIn=" + isUserLoggedIn() +
                '}';
    }
}
