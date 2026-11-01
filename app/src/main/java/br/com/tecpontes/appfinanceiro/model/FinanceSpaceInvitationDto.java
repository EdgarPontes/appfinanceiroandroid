package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Convite ou resultado da criação de convite para um espaço financeiro. */
public class FinanceSpaceInvitationDto {
    public static final String CHANNEL_EMAIL = "email";
    public static final String CHANNEL_WHATSAPP = "whatsapp";
    public static final String CHANNEL_PUSH = "push";

    @SerializedName("id") public String id;
    @SerializedName("spaceId") public String spaceId;
    @SerializedName("email") public String email;
    @SerializedName("status") public String status;
    @SerializedName("expiresAt") public String expiresAt;
    @SerializedName("deliveryChannel") @Nullable public String deliveryChannel;
    @SerializedName("emailSent") public boolean emailSent;
    @SerializedName("pushSent") public boolean pushSent;
    @SerializedName("shareUrl") @Nullable public String shareUrl;
    @SerializedName("invitedByUserId") @Nullable public String invitedByUserId;
    @SerializedName("createdAt") @Nullable public String createdAt;
}
