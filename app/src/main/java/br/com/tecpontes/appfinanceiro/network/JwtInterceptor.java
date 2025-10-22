package br.com.tecpontes.appfinanceiro.network;

import android.util.Log;
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

        Log.d("JwtInterceptor", "Interceptando requisição para: " + original.url());

        // Adiciona o token JWT se disponível
        Request.Builder builder = original.newBuilder();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
            Log.d("JwtInterceptor", "Token JWT adicionado à requisição");
        } else {
            Log.d("JwtInterceptor", "Nenhum token disponível para adicionar");
        }

        Request request = builder.build();
        Response response = chain.proceed(request);

        // Trata resposta 401 (não autorizado)
        if (response.code() == 401) {
            Log.d("JwtInterceptor", "401 detectado — limpando token");
            tokenManager.clearToken();

            // TODO: Emitir evento para forçar tela de login
            // Pode ser implementado com LiveData, EventBus ou Broadcast
        }

        return response;
    }
}
