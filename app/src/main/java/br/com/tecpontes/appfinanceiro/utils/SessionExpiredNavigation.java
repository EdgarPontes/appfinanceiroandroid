package br.com.tecpontes.appfinanceiro.utils;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tecpontes.appfinanceiro.ui.auth.LoginActivity;

/** Observa a expiração global da sessão e leva a tela ativa de volta ao login. */
public final class SessionExpiredNavigation {

    private SessionExpiredNavigation() {}

    public static void observe(AppCompatActivity activity, TokenManager tokenManager) {
        tokenManager.getSessionExpiredEvents().observe(activity, eventId -> {
            if (eventId == null || activity.isFinishing() || !tokenManager.consumeSessionExpiredEvent()) {
                return;
            }

            Intent intent = new Intent(activity, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
        });
    }
}
