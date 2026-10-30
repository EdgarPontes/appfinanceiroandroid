package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Confirma que a pessoa convidada controla o e-mail do convite. */
public class FinanceSpaceInvitationVerificationRequestDto {
    @SerializedName("token") public final String token;
    @SerializedName("code") public final String code;

    public FinanceSpaceInvitationVerificationRequestDto(String token, String code) {
        this.token = token;
        this.code = code;
    }
}
