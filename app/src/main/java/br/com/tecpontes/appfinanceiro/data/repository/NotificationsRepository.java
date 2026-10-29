package br.com.tecpontes.appfinanceiro.data.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;
import br.com.tecpontes.appfinanceiro.model.MessageLogDto;
import br.com.tecpontes.appfinanceiro.model.MessageLogsResponseDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.NetworkErrors;
import br.com.tecpontes.appfinanceiro.utils.ApiErrorUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Configurações de notificação, lembretes personalizados e histórico. */
@Singleton
public class NotificationsRepository {

    public static final int HISTORY_LIMIT = 50;
    public static final int UNREAD_LIMIT = 100;

    private final ApiService apiService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Inject
    public NotificationsRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    // ========== CONFIGURAÇÕES / TEMPLATES ==========

    public void getConfigs(@NonNull ResultCallback<List<MessageConfigDto>> callback) {
        enqueue(apiService.getMessageConfigs(), callback,
                "Não foi possível carregar as notificações");
    }

    public void createConfig(
            @NonNull MessageConfigDto body,
            @NonNull ResultCallback<MessageConfigDto> callback) {
        enqueue(apiService.createMessageConfig(body), callback,
                "Não foi possível criar o lembrete");
    }

    public void updateConfig(
            @NonNull String id,
            @NonNull MessageConfigDto body,
            @NonNull ResultCallback<MessageConfigDto> callback) {
        enqueue(apiService.updateMessageConfig(id, body), callback,
                "Não foi possível atualizar a notificação");
    }

    public void patchConfig(
            @NonNull String id,
            @NonNull MessageConfigDto body,
            @NonNull ResultCallback<MessageConfigDto> callback) {
        enqueue(apiService.patchMessageConfig(id, body), callback,
                "Não foi possível atualizar a notificação");
    }

    public void deleteConfig(@NonNull String id, @NonNull ActionCallback callback) {
        enqueueAction(apiService.deleteMessageConfig(id), callback,
                "Não foi possível excluir o lembrete");
    }

    public void createDefaults(@NonNull ActionCallback callback) {
        enqueueAction(apiService.createDefaultMessageConfigs(), callback,
                "Não foi possível criar as configurações padrão");
    }

    // ========== HISTÓRICO ==========

    public void getLogs(
            @Nullable String searchTerm,
            @Nullable String type,
            @Nullable String status,
            boolean showArchived,
            @NonNull ResultCallback<MessageLogsResponseDto> callback) {
        enqueue(apiService.getMessageLogs(
                        HISTORY_LIMIT,
                        0,
                        emptyToNull(searchTerm),
                        emptyToNull(type),
                        emptyToNull(status),
                        null,
                        showArchived ? "true" : "false"),
                callback,
                "Não foi possível carregar o histórico de notificações");
    }

    public void markRead(
            @NonNull String id,
            @NonNull ResultCallback<MessageLogDto> callback) {
        enqueue(apiService.markMessageLogAsRead(id), callback,
                "Não foi possível marcar a notificação como lida");
    }

    public void archive(
            @NonNull String id,
            @NonNull ResultCallback<MessageLogDto> callback) {
        enqueue(apiService.archiveMessageLog(id), callback,
                "Não foi possível arquivar a notificação");
    }

    public void deleteLog(@NonNull String id, @NonNull ActionCallback callback) {
        enqueueAction(apiService.deleteMessageLog(id), callback,
                "Não foi possível excluir a notificação");
    }

    /** Marca várias notificações como lidas sequencialmente, fora da thread principal. */
    public void markAllAsRead(@NonNull List<String> ids, @NonNull ActionCallback callback) {
        new Thread(() -> {
            try {
                for (String id : ids) {
                    Response<MessageLogDto> response =
                            apiService.markMessageLogAsRead(id).execute();
                    if (!response.isSuccessful()) {
                        throw new IOException(ApiErrorUtils.readError(response,
                                "Não foi possível marcar as notificações como lidas"));
                    }
                }
                mainHandler.post(callback::onSuccess);
            } catch (Exception error) {
                String message = error.getMessage() == null
                        ? "Não foi possível marcar as notificações como lidas"
                        : error.getMessage();
                mainHandler.post(() -> callback.onError(message));
            }
        }, "notifications-mark-all-read").start();
    }

    public void getUnreadCount(@NonNull ResultCallback<Integer> callback) {
        apiService.getMessageLogs(UNREAD_LIMIT, 0, null, null, "unread", null, "false")
                .enqueue(new Callback<MessageLogsResponseDto>() {
                    @Override public void onResponse(
                            @NonNull Call<MessageLogsResponseDto> ignored,
                            @NonNull Response<MessageLogsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().logsOrEmpty().size());
                        } else {
                            callback.onError(ApiErrorUtils.readError(response,
                                    "Não foi possível contar as notificações não lidas"));
                        }
                    }

                    @Override public void onFailure(
                            @NonNull Call<MessageLogsResponseDto> ignored,
                            @NonNull Throwable error) {
                        callback.onError(NetworkErrors.message(
                                error, "Não foi possível carregar as notificações. Tente novamente."));
                    }
                });
    }

    // ========== HELPERS ==========

    @Nullable
    private String emptyToNull(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private <T> void enqueue(
            @NonNull Call<T> call,
            @NonNull ResultCallback<T> callback,
            @NonNull String failureMessage) {
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(@NonNull Call<T> ignored, @NonNull Response<T> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(ApiErrorUtils.readError(response, failureMessage));
                }
            }

            @Override public void onFailure(@NonNull Call<T> ignored, @NonNull Throwable error) {
                callback.onError(NetworkErrors.message(
                        error, failureMessage + ". Tente novamente."));
            }
        });
    }

    private void enqueueAction(
            @NonNull Call<ResponseBody> call,
            @NonNull ActionCallback callback,
            @NonNull String failureMessage) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(
                    @NonNull Call<ResponseBody> ignored,
                    @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    ApiErrorUtils.closeBody(response.body());
                    callback.onSuccess();
                } else {
                    callback.onError(ApiErrorUtils.readError(response, failureMessage));
                }
            }

            @Override public void onFailure(
                    @NonNull Call<ResponseBody> ignored, @NonNull Throwable error) {
                callback.onError(NetworkErrors.message(
                        error, failureMessage + ". Tente novamente."));
            }
        });
    }

    public interface ResultCallback<T> {
        void onSuccess(@Nullable T result);
        void onError(@NonNull String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }
}
