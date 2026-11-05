package br.com.tecpontes.appfinanceiro.ui.help;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Estado dos guias de ajuda, salvo localmente e separado por usuário.
 *
 * <p>O mesmo aparelho pode ser usado por mais de uma conta. Por isso, tanto o estado geral
 * quanto a lista de guias concluídos usam a identidade estável fornecida pelo {@link TokenManager}.
 */
@Singleton
public class HelpPreferences {

    private static final String PREFS_NAME = "app_financeiro_help";
    private static final String KEY_ENABLED_PREFIX = "enabled_";
    private static final String KEY_COMPLETED_PREFIX = "completed_";

    private final SharedPreferences preferences;
    private final TokenManager tokenManager;

    @Inject
    public HelpPreferences(
            @ApplicationContext @NonNull Context context,
            @NonNull TokenManager tokenManager) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.tokenManager = tokenManager;
    }

    /** Indica se os guias automáticos estão habilitados para o usuário atual. */
    public boolean isHelpEnabled() {
        String userScope = userScope();
        return userScope != null
                && preferences.getBoolean(KEY_ENABLED_PREFIX + userScope, true);
    }

    public void setHelpEnabled(boolean enabled) {
        String userScope = userScope();
        if (userScope == null) return;
        preferences.edit()
                .putBoolean(KEY_ENABLED_PREFIX + userScope, enabled)
                .apply();
    }

    /** Verifica se o guia ainda deve ser apresentado ao usuário atual. */
    public boolean shouldShow(@NonNull String guideId) {
        String userScope = userScope();
        if (userScope == null || !isHelpEnabled()) return false;
        return !preferences.getBoolean(completedKey(userScope, guideId), false);
    }

    /** Marca o guia como concluído ou pulado pelo usuário. */
    public void markCompleted(@NonNull String guideId) {
        String userScope = userScope();
        if (userScope == null) return;
        preferences.edit()
                .putBoolean(completedKey(userScope, guideId), true)
                .apply();
    }

    /** Libera todos os guias novamente sem alterar o estado geral de ajuda. */
    public void resetAllGuides() {
        String userScope = userScope();
        if (userScope == null) return;
        String prefix = KEY_COMPLETED_PREFIX + userScope + "_";
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (entry.getKey().startsWith(prefix)) editor.remove(entry.getKey());
        }
        editor.apply();
    }

    /** Reativa a ajuda e libera todos os guias para as próximas telas. */
    public void enableAndResetAll() {
        setHelpEnabled(true);
        resetAllGuides();
    }

    private String completedKey(String userScope, @NonNull String guideId) {
        return KEY_COMPLETED_PREFIX + userScope + "_" + guideId;
    }

    private String userScope() {
        String identity = tokenManager.getUserIdentity();
        if (identity == null || identity.trim().isEmpty()) return null;
        return Integer.toHexString(identity.hashCode());
    }
}
