package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Resposta após aceitar um convite. */
public class FinanceSpaceInvitationAcceptResponseDto {
    @SerializedName("success") public boolean success;
    @SerializedName("spaceId") public String spaceId;
}
