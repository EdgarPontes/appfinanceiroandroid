package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Campos editáveis do perfil. */
public class ProfileUpdateRequestDto {
    @SerializedName("firstName") public String firstName;
    @Nullable @SerializedName("lastName") public String lastName;
    @Nullable @SerializedName("cellphone") public String cellphone;
    @Nullable @SerializedName("telegramId") public String telegramId;
}
