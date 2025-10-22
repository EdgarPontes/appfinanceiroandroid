package br.com.tecpontes.appfinanceiro.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import br.com.tecpontes.appfinanceiro.model.AuthRequest;
import br.com.tecpontes.appfinanceiro.model.AuthResponse;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository para operações de autenticação
 */
@Singleton
public class AuthRepository {

    private static final String TAG = "AuthRepository";

    private final ApiService apiService;
    private final TokenManager tokenManager;

    @Inject
    public AuthRepository(ApiService apiService, TokenManager tokenManager) {
        this.apiService = apiService;
        this.tokenManager = tokenManager;
    }

    /**
     * Realiza login do usuário
     */
    public void login(@NonNull String email, @NonNull String password,
                     @NonNull AuthCallback callback) {
        Log.d(TAG, "Tentando fazer login para: " + email);

        AuthRequest request = new AuthRequest(email, password);
        Call<AuthResponse> call = apiService.login(request);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call,
                                 @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    // Salva token e dados do usuário
                    tokenManager.saveToken(authResponse.getToken());
                    if (authResponse.getUser() != null) {
                        tokenManager.saveUserEmail(authResponse.getUser().getEmail());
                    }

                    Log.d(TAG, "Login realizado com sucesso");
                    callback.onSuccess(authResponse);
                } else {
                    Log.d(TAG, "Erro no login: " + response.message());
                    callback.onError("Credenciais inválidas");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha na requisição de login", t);
                callback.onError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    /**
     * Realiza registro do usuário
     */
    public void register(@NonNull String email, @NonNull String password,
                        @NonNull AuthCallback callback) {
        Log.d(TAG, "Tentando registrar usuário: " + email);

        AuthRequest request = new AuthRequest(email, password);
        Call<AuthResponse> call = apiService.register(request);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call,
                                 @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    // Salva token e dados do usuário
                    tokenManager.saveToken(authResponse.getToken());
                    if (authResponse.getUser() != null) {
                        tokenManager.saveUserEmail(authResponse.getUser().getEmail());
                    }

                    Log.d(TAG, "Registro realizado com sucesso");
                    callback.onSuccess(authResponse);
                } else {
                    Log.d(TAG, "Erro no registro: " + response.message());
                    callback.onError("Erro ao criar conta");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha na requisição de registro", t);
                callback.onError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    /**
     * Verifica se o usuário está logado
     */
    public boolean isLoggedIn() {
        return tokenManager.isLoggedIn();
    }

    /**
     * Realiza logout
     */
    public void logout() {
        Log.d(TAG, "Realizando logout");
        tokenManager.clearToken();
    }

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return "AuthRepository{" +
                "isLoggedIn=" + isLoggedIn() +
                ", " + tokenManager.getDebugInfo() +
                '}';
    }

    /**
     * Interface para callbacks de autenticação
     */
    public interface AuthCallback {
        void onSuccess(AuthResponse response);
        void onError(String error);
    }
}
