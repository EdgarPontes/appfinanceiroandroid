package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Corpo aceito por PUT /api/me/password. */
public class PasswordUpdateRequestDto {
    @SerializedName("password") public String password;
    public PasswordUpdateRequestDto(String password) { this.password = password; }
}
