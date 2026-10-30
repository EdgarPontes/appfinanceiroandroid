package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Convite ou resultado da criação de convite para um espaço financeiro. */
public class FinanceSpaceInvitationDto {
    @SerializedName("id") public String id;
    @SerializedName("spaceId") public String spaceId;
    @SerializedName("email") public String email;
    @SerializedName("status") public String status;
    @SerializedName("expiresAt") public String expiresAt;
    @SerializedName("emailSent") public boolean emailSent;
    @SerializedName("shareUrl") @Nullable public String shareUrl;
    @SerializedName("invitedByUserId") @Nullable public String invitedByUserId;
    @SerializedName("createdAt") @Nullable public String createdAt;
}
