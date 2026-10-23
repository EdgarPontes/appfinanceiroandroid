package br.com.tecpontes.appfinanceiro.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Response;

/** Centraliza a leitura das mensagens de erro devolvidas pela API. */
public final class ApiErrorUtils {

    private ApiErrorUtils() {}

    /**
     * A API usa {@code message} em alguns endpoints e {@code error} em outros. Lê os dois e,
     * quando não encontra nada, devolve o fallback com o status HTTP.
     */
    @NonNull
    public static String readError(@NonNull Response<?> response, @NonNull String fallback) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                String body = errorBody.string();
                JSONObject json = new JSONObject(body);
                String message = json.optString("message");
                if (!message.isEmpty()) return message;
                String error = json.optString("error");
                if (!error.isEmpty()) return error;
            } catch (Exception ignored) {
                // Corpo não-JSON: usa o fallback.
            }
        }
        String reason = response.message();
        return reason == null || reason.isEmpty() ? fallback : fallback + ": " + reason;
    }

    public static void closeBody(@Nullable ResponseBody body) {
        if (body == null) return;
        try {
            body.close();
        } catch (Exception ignored) {
            // Ignora falhas ao fechar o corpo.
        }
    }
}
