package br.com.tecpontes.appfinanceiro.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Gerencia o armazenamento e recuperação do token JWT
 * TODO: Substituir por EncryptedSharedPreferences em produção
 */
@Singleton
public class TokenManager {

    private static final String PREF_NAME = "app_financeiro_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private final SharedPreferences preferences;

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Salva o token JWT
     */
    public void saveToken(@NonNull String token) {
        preferences.edit()
                .putString(KEY_TOKEN, token)
                .apply();
        android.util.Log.d("TokenManager", "Token salvo com sucesso");
    }

    /**
     * Recupera o token JWT
     */
    @Nullable
    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    /**
     * Salva o email do usuário
     */
    public void saveUserEmail(@NonNull String email) {
        preferences.edit()
                .putString(KEY_USER_EMAIL, email)
                .apply();
        android.util.Log.d("TokenManager", "Email do usuário salvo: " + email);
    }

    /**
     * Recupera o email do usuário
     */
    @Nullable
    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Verifica se o usuário está logado
     */
    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    /**
     * Remove o token e dados do usuário (logout)
     */
    public void clearToken() {
        preferences.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_EMAIL)
                .apply();
        android.util.Log.d("TokenManager", "Token e dados do usuário removidos");
    }

    /**
     * Obtém informações do usuário para debug
     */
    public String getDebugInfo() {
        return "TokenManager{" +
                "hasToken=" + (getToken() != null) +
                ", tokenLength=" + (getToken() != null ? getToken().length() : 0) +
                ", userEmail=" + getUserEmail() +
                '}';
    }
}
