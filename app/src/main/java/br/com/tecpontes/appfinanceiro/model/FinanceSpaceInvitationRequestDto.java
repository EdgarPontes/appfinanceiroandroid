package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Corpo de POST /finance-spaces/{id}/invitations. */
public class FinanceSpaceInvitationRequestDto {
    @SerializedName("email") public final String email;

    public FinanceSpaceInvitationRequestDto(String email) {
        this.email = email;
    }
}
