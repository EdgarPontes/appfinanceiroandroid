package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Confirmação de senha exigida para DELETE /api/users/deactivate. */
public class DeactivateAccountRequestDto {
    @SerializedName("password") public String password;
    public DeactivateAccountRequestDto(String password) { this.password = password; }
}
