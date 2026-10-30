package br.com.tecpontes.appfinanceiro.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceDeletionRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationAcceptResponseDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationPreviewDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationTokenRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationVerificationRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceMemberDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.ApiErrorUtils;
import br.com.tecpontes.appfinanceiro.utils.NetworkErrors;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Operações de espaços financeiros, convites e seleção do espaço ativo. */
@Singleton
public class FinanceSpaceRepository {
    private final ApiService apiService;
    private final ActiveFinanceSpaceStore activeSpaceStore;

    @Inject
    public FinanceSpaceRepository(ApiService apiService, ActiveFinanceSpaceStore activeSpaceStore) {
        this.apiService = apiService;
        this.activeSpaceStore = activeSpaceStore;
    }

    public void getSpaces(@NonNull ResultCallback<List<FinanceSpaceDto>> callback) {
        enqueue(apiService.getFinanceSpaces(), callback, "Não foi possível carregar os espaços.");
    }

    public void getMembers(
            @NonNull String spaceId,
            @NonNull ResultCallback<List<FinanceSpaceMemberDto>> callback) {
        enqueue(apiService.getFinanceSpaceMembers(spaceId), callback, "Não foi possível carregar os membros.");
    }

    public void getInvitations(
            @NonNull String spaceId,
            @NonNull ResultCallback<List<FinanceSpaceInvitationDto>> callback) {
        enqueue(apiService.getFinanceSpaceInvitations(spaceId), callback, "Não foi possível carregar os convites.");
    }

    public void getDeletionRequests(
            @NonNull String spaceId,
            @NonNull ResultCallback<List<FinanceSpaceDeletionRequestDto>> callback) {
        enqueue(apiService.getFinanceSpaceDeletionRequests(spaceId), callback,
                "Não foi possível carregar os pedidos de exclusão.");
    }

    public void decideDeletionRequest(
            @NonNull String spaceId,
            @NonNull String requestId,
            boolean approve,
            @NonNull ActionCallback callback) {
        Call<ResponseBody> call = approve
                ? apiService.approveFinanceSpaceDeletionRequest(spaceId, requestId)
                : apiService.rejectFinanceSpaceDeletionRequest(spaceId, requestId);
        enqueueAction(call, callback,
                approve ? "Não foi possível aprovar a exclusão." : "Não foi possível recusar o pedido.");
    }

    public void createInvitation(
            @NonNull String spaceId,
            @NonNull String email,
            @NonNull ResultCallback<FinanceSpaceInvitationDto> callback) {
        enqueue(apiService.createFinanceSpaceInvitation(
                spaceId, new FinanceSpaceInvitationRequestDto(email)),
                callback, "Não foi possível criar o convite.");
    }

    public void revokeInvitation(
            @NonNull String spaceId,
            @NonNull String invitationId,
            @NonNull ActionCallback callback) {
        enqueueAction(apiService.revokeFinanceSpaceInvitation(spaceId, invitationId),
                callback, "Não foi possível cancelar o convite.");
    }

    public void resendInvitation(
            @NonNull String spaceId,
            @NonNull String invitationId,
            @NonNull ResultCallback<FinanceSpaceInvitationDto> callback) {
        enqueue(apiService.resendFinanceSpaceInvitation(spaceId, invitationId), callback,
                "Não foi possível reenviar o convite.");
    }

    public void previewInvitation(
            @NonNull String token,
            @NonNull ResultCallback<FinanceSpaceInvitationPreviewDto> callback) {
        enqueue(apiService.previewFinanceSpaceInvitation(token), callback,
                "Não foi possível carregar o convite.");
    }

    public void acceptInvitation(
            @NonNull String token,
            @NonNull ResultCallback<FinanceSpaceInvitationAcceptResponseDto> callback) {
        enqueue(apiService.acceptFinanceSpaceInvitation(new FinanceSpaceInvitationTokenRequestDto(token)),
                new ResultCallback<FinanceSpaceInvitationAcceptResponseDto>() {
                    @Override public void onSuccess(
                            @Nullable FinanceSpaceInvitationAcceptResponseDto value) {
                        if (value != null && value.spaceId != null) {
                            activeSpaceStore.setActiveSpaceId(value.spaceId);
                        }
                        callback.onSuccess(value);
                    }

                    @Override public void onError(@NonNull String message) {
                        callback.onError(message);
                    }
                }, "Não foi possível aceitar o convite.");
    }

    public void verifyInvitationEmail(
            @NonNull String token,
            @NonNull String code,
            @NonNull ActionCallback callback) {
        enqueueAction(apiService.verifyFinanceSpaceInvitationEmail(
                new FinanceSpaceInvitationVerificationRequestDto(token, code)),
                callback, "Não foi possível confirmar o e-mail.");
    }

    public void declineInvitation(@NonNull String token, @NonNull ActionCallback callback) {
        enqueueAction(apiService.declineFinanceSpaceInvitation(
                new FinanceSpaceInvitationTokenRequestDto(token)),
                callback, "Não foi possível recusar o convite.");
    }

    public void selectSpace(@NonNull String spaceId) {
        activeSpaceStore.setActiveSpaceId(spaceId);
    }

    @Nullable
    public String getSelectedSpaceId() {
        return activeSpaceStore.getActiveSpaceId();
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
                callback.onError(NetworkErrors.message(error, failureMessage));
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
                    @NonNull Call<ResponseBody> ignored,
                    @NonNull Throwable error) {
                callback.onError(NetworkErrors.message(error, failureMessage));
            }
        });
    }

    public interface ResultCallback<T> {
        void onSuccess(@Nullable T value);
        void onError(@NonNull String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }
}
