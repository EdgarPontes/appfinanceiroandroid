package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Corpo de aceite ou recusa de convite. */
public class FinanceSpaceInvitationTokenRequestDto {
    @SerializedName("token") public final String token;

    public FinanceSpaceInvitationTokenRequestDto(String token) {
        this.token = token;
    }
}
