package br.com.tecpontes.appfinanceiro.ui.auth;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.core.content.ContextCompat;
import br.com.tecpontes.appfinanceiro.BuildConfig;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Obtém o ID token do Google via Credential Manager. O token é enviado ao backend,
 * que valida a identidade e devolve o JWT da aplicação.
 *
 * <p>O ID token nunca deve ser logado ou persistido localmente.</p>
 */
@Singleton
public class GoogleSignInClient {

    private static final String TAG = "GoogleSignInClient";

    public interface Callback {
        void onSuccess(@NonNull String idToken);

        /** Usuário fechou o seletor de contas; não é um erro. */
        void onCancelled();

        void onError(@NonNull String message);
    }

    private final Context applicationContext;

    @Inject
    public GoogleSignInClient(@ApplicationContext Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void signIn(@NonNull Activity activity, @NonNull Callback callback) {
        String serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID;
        if (serverClientId == null || serverClientId.trim().isEmpty()) {
            Log.w(TAG, "GOOGLE_WEB_CLIENT_ID não configurado");
            callback.onError("Login com Google não configurado neste build.");
            return;
        }

        final GetCredentialRequest request;
        try {
            request = new GetCredentialRequest.Builder()
                    .addCredentialOption(
                            new GetSignInWithGoogleOption.Builder(serverClientId.trim()).build())
                    .build();
        } catch (RuntimeException error) {
            Log.e(TAG, "Configuração inválida do client ID do Google", error);
            callback.onError("Login com Google não configurado neste build.");
            return;
        }

        try {
            CredentialManager credentialManager = CredentialManager.create(applicationContext);
            credentialManager.getCredentialAsync(
                    activity,
                    request,
                    null,
                    ContextCompat.getMainExecutor(activity),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(@NonNull GetCredentialResponse result) {
                            handleResult(result, callback);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException error) {
                            handleError(error, callback);
                        }
                    });
        } catch (RuntimeException error) {
            Log.e(TAG, "Falha ao iniciar o Credential Manager", error);
            callback.onError("Não foi possível abrir o login do Google. Tente novamente.");
        }
    }

    private void handleResult(@NonNull GetCredentialResponse result, @NonNull Callback callback) {
        Credential credential = result.getCredential();
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        .equals(credential.getType())) {
            Log.w(TAG, "Tipo de credencial inesperado: " + credential.getType());
            callback.onError("Não foi possível entrar com o Google. Tente novamente.");
            return;
        }

        try {
            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(credential.getData());
            String idToken = googleCredential.getIdToken();
            if (idToken == null || idToken.trim().isEmpty()) {
                callback.onError("Não foi possível validar a conta Google. Tente novamente.");
                return;
            }
            callback.onSuccess(idToken);
        } catch (RuntimeException error) {
            Log.e(TAG, "Credencial do Google inválida", error);
            callback.onError("Não foi possível validar a conta Google. Tente novamente.");
        }
    }

    private void handleError(@NonNull GetCredentialException error, @NonNull Callback callback) {
        if (error instanceof GetCredentialCancellationException) {
            callback.onCancelled();
            return;
        }
        if (error instanceof NoCredentialException) {
            Log.d(TAG, "Nenhuma conta Google disponível no dispositivo");
            callback.onError("Nenhuma conta Google disponível neste dispositivo.");
            return;
        }
        Log.e(TAG, "Falha no login com Google: " + error.getType(), error);
        callback.onError("Não foi possível entrar com o Google. Tente novamente.");
    }
}
