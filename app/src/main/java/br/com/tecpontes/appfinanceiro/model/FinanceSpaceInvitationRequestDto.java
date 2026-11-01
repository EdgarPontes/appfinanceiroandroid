package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

/** Corpo de POST /finance-spaces/{id}/invitations. */
public class FinanceSpaceInvitationRequestDto {
    @SerializedName("email") public final String email;
    @SerializedName("channel") public final String channel;

    public FinanceSpaceInvitationRequestDto(@NonNull String email, @NonNull String channel) {
        this.email = email;
        this.channel = channel;
    }
}
