package br.com.tecpontes.appfinanceiro.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import br.com.tecpontes.appfinanceiro.model.AuthRequest;
import br.com.tecpontes.appfinanceiro.model.AuthResponse;
import br.com.tecpontes.appfinanceiro.model.ForgotPasswordRequestDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.ApiErrorUtils;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.ResponseBody;
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
    public void login(@NonNull String email, @NonNull String password, boolean keepConnected,
                      @NonNull AuthCallback callback) {
        Log.d(TAG, "Tentando fazer login para: " + email);

        AuthRequest request = new AuthRequest(email, password, keepConnected);
        Call<AuthResponse> call = apiService.login(request);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call,
                                 @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    String token = authResponse.getToken();
                    if (token == null || token.trim().isEmpty()) {
                        callback.onError("A API não retornou um token de autenticação");
                        return;
                    }

                    // Salva token e dados do usuário
                    tokenManager.saveToken(token, keepConnected);
                    if (authResponse.getUser() != null) {
                        tokenManager.saveUserEmail(authResponse.getUser().getEmail(), keepConnected);
                    }

                    Log.d(TAG, "Login realizado com sucesso");
                    callback.onSuccess(authResponse);
                } else {
                    Log.d(TAG, "Erro no login: " + response.message());
                    callback.onError(ApiErrorUtils.readError(response, "Credenciais inválidas"));
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
    public void register(@NonNull String name, @NonNull String email,
                         @NonNull String cellphone, @NonNull String password,
                         @NonNull AuthCallback callback) {
        Log.d(TAG, "Tentando registrar usuário: " + email);

        AuthRequest request = new AuthRequest(email, password, name, cellphone);
        Call<AuthResponse> call = apiService.register(request);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call,
                                 @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    // O cadastro não inicia sessão; a autenticação é feita pelo fluxo de login.
                    Log.d(TAG, "Registro realizado com sucesso");
                    callback.onSuccess(authResponse);
                } else {
                    Log.d(TAG, "Erro no registro: " + response.message());
                    callback.onError(ApiErrorUtils.readError(response, "Erro ao criar conta"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha na requisição de registro", t);
                callback.onError("Erro de conexão: " + t.getMessage());
            }
        });
    }

    /** Solicita as instruções de recuperação de senha. */
    public void requestPasswordReset(
            @NonNull String email,
            @NonNull String cellphone,
            @NonNull ForgotPasswordCallback callback) {
        apiService.forgotPassword(new ForgotPasswordRequestDto(email, cellphone))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call,
                                           @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            ApiErrorUtils.closeBody(response.body());
                            callback.onSuccess(
                                    "Se os dados estiverem corretos, as instruções serão enviadas.");
                        } else {
                            callback.onError(ApiErrorUtils.readError(response,
                                    "Não foi possível solicitar a recuperação de senha"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call,
                                          @NonNull Throwable error) {
                        callback.onError("Erro de conexão: " + error.getMessage());
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

    public interface ForgotPasswordCallback {
        void onSuccess(@NonNull String message);
        void onError(@NonNull String error);
    }
}
