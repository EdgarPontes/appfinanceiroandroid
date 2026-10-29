package br.com.tecpontes.appfinanceiro.data.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.DeactivateAccountRequestDto;
import br.com.tecpontes.appfinanceiro.model.PasswordUpdateRequestDto;
import br.com.tecpontes.appfinanceiro.model.ProfileDto;
import br.com.tecpontes.appfinanceiro.model.ProfileUpdateRequestDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.NetworkErrors;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ViewModelScoped;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.inject.Inject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Chamadas remotas usadas pela página de perfil. */
@ViewModelScoped
public class ProfileRepository {
    private static final long MAX_AVATAR_BYTES = 5L * 1024L * 1024L;
    private final ApiService apiService;
    private final ContentResolver contentResolver;
    private final File cacheDirectory;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Inject
    public ProfileRepository(ApiService apiService, @ApplicationContext Context context) {
        this.apiService = apiService;
        this.contentResolver = context.getContentResolver();
        this.cacheDirectory = context.getCacheDir();
    }

    public void getProfile(@NonNull ResultCallback<ProfileDto> callback) {
        enqueue(apiService.getMyProfile(), callback, "Não foi possível carregar o perfil");
    }

    public void updateProfile(@NonNull ProfileUpdateRequestDto request, @NonNull ActionCallback callback) {
        enqueueAction(apiService.updateMyProfile(request), callback, "Não foi possível atualizar o perfil");
    }

    public void updatePassword(@NonNull String password, @NonNull ActionCallback callback) {
        enqueueAction(apiService.updateMyPassword(new PasswordUpdateRequestDto(password)), callback,
                "Não foi possível alterar a senha");
    }

    public void deactivateAccount(@NonNull String password, @NonNull ActionCallback callback) {
        enqueueAction(apiService.deactivateMyAccount(new DeactivateAccountRequestDto(password)), callback,
                "Não foi possível desativar a conta");
    }

    public void uploadAvatar(@NonNull Uri uri, @NonNull ActionCallback callback) {
        new Thread(() -> prepareAndUploadAvatar(uri, callback), "profile-avatar-upload").start();
    }

    private void prepareAndUploadAvatar(@NonNull Uri uri, @NonNull ActionCallback callback) {
        File temporaryFile = null;
        try {
            String mimeType = contentResolver.getType(uri);
            if (!isAcceptedImageType(mimeType)) {
                mainHandler.post(() -> callback.onError("Escolha uma imagem JPEG, PNG, GIF ou WebP."));
                return;
            }
            String filename = displayName(uri);
            String suffix = suffix(filename, mimeType);
            temporaryFile = File.createTempFile("profile-avatar-", suffix, cacheDirectory);
            copyImage(uri, temporaryFile);
            RequestBody body = RequestBody.create(MediaType.parse(mimeType), temporaryFile);
            MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", filename, body);
            File fileToDelete = temporaryFile;
            apiService.uploadMyAvatar(part).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(@NonNull Call<ResponseBody> call,
                                                @NonNull Response<ResponseBody> response) {
                    fileToDelete.delete();
                    if (response.isSuccessful()) {
                        closeBody(response.body());
                        callback.onSuccess();
                    }
                    else callback.onError(readError(response, "Não foi possível enviar a foto"));
                }
                @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable error) {
                    fileToDelete.delete();
                    callback.onError(NetworkErrors.message(
                            error, "Não foi possível enviar a foto. Tente novamente."));
                }
            });
        } catch (Exception error) {
            if (temporaryFile != null) temporaryFile.delete();
            String message = error.getMessage() == null
                    ? "Não foi possível preparar a imagem." : error.getMessage();
            mainHandler.post(() -> callback.onError(message));
        }
    }

    private void copyImage(Uri uri, File destination) throws Exception {
        try (InputStream input = contentResolver.openInputStream(uri);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new IllegalArgumentException("Não foi possível abrir a imagem selecionada.");
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_AVATAR_BYTES) {
                    throw new IllegalArgumentException("A imagem deve ter no máximo 5 MB.");
                }
                output.write(buffer, 0, read);
            }
            if (total == 0) throw new IllegalArgumentException("O arquivo de imagem está vazio.");
        }
    }

    private boolean isAcceptedImageType(@Nullable String mimeType) {
        return "image/jpeg".equalsIgnoreCase(mimeType)
                || "image/png".equalsIgnoreCase(mimeType)
                || "image/gif".equalsIgnoreCase(mimeType)
                || "image/webp".equalsIgnoreCase(mimeType);
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String name = cursor.getString(column);
                    if (name != null && !name.trim().isEmpty()) return name;
                }
            }
        } catch (Exception ignored) { }
        return "avatar" + suffix("", contentResolver.getType(uri));
    }

    private String suffix(String filename, @Nullable String mimeType) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) return filename.substring(dot);
        if ("image/png".equalsIgnoreCase(mimeType)) return ".png";
        if ("image/gif".equalsIgnoreCase(mimeType)) return ".gif";
        if ("image/webp".equalsIgnoreCase(mimeType)) return ".webp";
        return ".jpg";
    }

    private <T> void enqueue(@NonNull Call<T> call, @NonNull ResultCallback<T> callback,
                             @NonNull String failureMessage) {
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(@NonNull Call<T> ignored, @NonNull Response<T> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(readError(response, failureMessage));
            }
            @Override public void onFailure(@NonNull Call<T> ignored, @NonNull Throwable error) {
                callback.onError(NetworkErrors.message(
                        error, "Não foi possível concluir a operação. Tente novamente."));
            }
        });
    }

    private void enqueueAction(@NonNull Call<ResponseBody> call, @NonNull ActionCallback callback,
                               @NonNull String failureMessage) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(@NonNull Call<ResponseBody> ignored,
                                            @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    closeBody(response.body());
                    callback.onSuccess();
                }
                else callback.onError(readError(response, failureMessage));
            }
            @Override public void onFailure(@NonNull Call<ResponseBody> ignored, @NonNull Throwable error) {
                callback.onError(NetworkErrors.message(
                        error, "Não foi possível concluir a operação. Tente novamente."));
            }
        });
    }

    private String readError(Response<?> response, String fallback) {
        if (response.errorBody() != null) {
            try {
                String body = response.errorBody().string();
                org.json.JSONObject json = new org.json.JSONObject(body);
                String message = json.optString("message");
                if (!message.isEmpty()) return message;
            } catch (Exception ignored) { }
        }
        return fallback + ": " + response.message();
    }

    private void closeBody(@Nullable ResponseBody body) {
        if (body != null) {
            try { body.close(); } catch (Exception ignored) { }
        }
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
