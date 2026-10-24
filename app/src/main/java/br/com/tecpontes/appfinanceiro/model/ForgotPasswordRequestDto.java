package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

/** Campos aceitos por POST /api/auth/forgot-password. */
public class ForgotPasswordRequestDto {
    @NonNull @SerializedName("email") public final String email;
    @NonNull @SerializedName("cellphone") public final String cellphone;

    public ForgotPasswordRequestDto(@NonNull String email, @NonNull String cellphone) {
        this.email = email;
        this.cellphone = cellphone;
    }
}
