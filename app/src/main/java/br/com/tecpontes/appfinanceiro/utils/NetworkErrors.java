package br.com.tecpontes.appfinanceiro.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

/** Converte falhas técnicas de rede em mensagens compreensíveis ao usuário. */
public final class NetworkErrors {

    public static final String OFFLINE_MESSAGE =
            "Sem conexão com a internet. Verifique sua rede e tente novamente.";

    private NetworkErrors() {
    }

    public static boolean isOffline(@Nullable Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof UnknownHostException
                    || current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof SocketException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("unable to resolve host")
                        || normalized.contains("failed to connect")
                        || normalized.contains("timeout")
                        || normalized.contains("network is unreachable")
                        || normalized.contains("no address associated with hostname")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return error instanceof IOException && error.getMessage() != null
                && error.getMessage().toLowerCase(Locale.ROOT).contains("offline");
    }

    @NonNull
    public static String message(
            @Nullable Throwable error, @NonNull String fallback) {
        return isOffline(error) ? OFFLINE_MESSAGE : fallback;
    }
}
