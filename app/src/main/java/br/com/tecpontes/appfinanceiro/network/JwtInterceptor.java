package br.com.tecpontes.appfinanceiro.network;

import android.util.Log;
import br.com.tecpontes.appfinanceiro.BuildConfig;
import androidx.annotation.NonNull;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor do OkHttp que adiciona o token JWT automaticamente nas requisições
 * e trata respostas 401 (não autorizado)
 */
@Singleton
public class JwtInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    @Inject
    public JwtInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenManager.getToken();
        boolean hasExplicitAuthorization = original.header("Authorization") != null;
        boolean authenticatedRequest = hasExplicitAuthorization || (token != null && !token.isEmpty());

        Log.d("JwtInterceptor", "Interceptando requisição para: " + original.url());

        // Adiciona o token JWT se disponível
        Request.Builder builder = original.newBuilder();
        builder.header("X-App-Client", "android");
        builder.header("X-App-Version", BuildConfig.VERSION_NAME);
        if (!hasExplicitAuthorization && token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
            Log.d("JwtInterceptor", "Token JWT adicionado à requisição");
        } else if (hasExplicitAuthorization) {
            Log.d("JwtInterceptor", "Autorização explícita preservada para a requisição");
        } else {
            Log.d("JwtInterceptor", "Nenhum token disponível para adicionar");
        }

        Request request = builder.build();
        Response response = chain.proceed(request);

        // Trata expiração em qualquer endpoint que tenha recebido um token.
        if (response.code() == 401 && authenticatedRequest) {
            Log.d("JwtInterceptor", "401 detectado — encerrando sessão e notificando a interface");
            tokenManager.notifySessionExpired();
        }

        return response;
    }
}
