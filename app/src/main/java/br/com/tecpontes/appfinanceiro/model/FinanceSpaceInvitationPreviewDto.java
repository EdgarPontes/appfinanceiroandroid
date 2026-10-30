package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Dados mínimos da prévia de convite, sem dados financeiros. */
public class FinanceSpaceInvitationPreviewDto {
    @SerializedName("id") public String id;
    @SerializedName("spaceId") public String spaceId;
    @SerializedName("spaceName") public String spaceName;
    @SerializedName("invitedByName") @Nullable public String invitedByName;
    @SerializedName("email") public String email;
    @SerializedName("expiresAt") public String expiresAt;
    @SerializedName("requiresEmailVerification") public boolean requiresEmailVerification;
}
