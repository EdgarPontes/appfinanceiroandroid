package br.com.tecpontes.appfinanceiro.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
    private final MutableLiveData<Long> sessionExpiredEvents = new MutableLiveData<>();
    private final AtomicBoolean sessionExpiredEventPending = new AtomicBoolean(false);
    private final AtomicLong sessionExpiredEventId = new AtomicLong(0);

    @Nullable
    private volatile String sessionToken;
    @Nullable
    private volatile String sessionUserEmail;

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Salva o token JWT, de forma persistente apenas quando o usuário escolheu manter a sessão.
     */
    public void saveToken(@NonNull String token) {
        saveToken(token, true);
    }

    public void saveToken(@NonNull String token, boolean keepConnected) {
        sessionToken = token;
        sessionUserEmail = null;
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_USER_EMAIL);
        if (keepConnected) {
            editor.putString(KEY_TOKEN, token);
        } else {
            editor.remove(KEY_TOKEN);
        }
        editor.apply();
        android.util.Log.d("TokenManager", "Token salvo com sucesso");
    }

    /**
     * Recupera o token JWT
     */
    @Nullable
    public String getToken() {
        String currentSessionToken = sessionToken;
        return currentSessionToken != null ? currentSessionToken : preferences.getString(KEY_TOKEN, null);
    }

    /**
     * Salva o email do usuário
     */
    public void saveUserEmail(@NonNull String email) {
        saveUserEmail(email, true);
    }

    public void saveUserEmail(@NonNull String email, boolean keepConnected) {
        sessionUserEmail = email;
        SharedPreferences.Editor editor = preferences.edit();
        if (keepConnected) {
            editor.putString(KEY_USER_EMAIL, email);
        } else {
            editor.remove(KEY_USER_EMAIL);
        }
        editor.apply();
        android.util.Log.d("TokenManager", "Email do usuário salvo: " + email);
    }

    /**
     * Recupera o email do usuário
     */
    @Nullable
    public String getUserEmail() {
        String currentSessionEmail = sessionUserEmail;
        return currentSessionEmail != null ? currentSessionEmail : preferences.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Verifica se o usuário está logado
     */
    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    /** Publica uma expiração apenas na primeira resposta 401 da sessão atual. */
    public synchronized void notifySessionExpired() {
        if (!isLoggedIn()) return;

        clearToken();
        sessionExpiredEventPending.set(true);
        sessionExpiredEvents.postValue(sessionExpiredEventId.incrementAndGet());
    }

    public LiveData<Long> getSessionExpiredEvents() {
        return sessionExpiredEvents;
    }

    /** Garante que apenas uma tela ativa processe cada expiração de sessão. */
    public boolean consumeSessionExpiredEvent() {
        return sessionExpiredEventPending.compareAndSet(true, false);
    }

    /**
     * Remove o token e dados do usuário (logout)
     */
    public void clearToken() {
        sessionToken = null;
        sessionUserEmail = null;
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
