package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Dados retornados por GET /api/me. */
public class ProfileDto {
    @Nullable @SerializedName("email") public String email;
    @Nullable @SerializedName("cellphone") public String cellphone;
    @Nullable @SerializedName("profileImageUrl") public String profileImageUrl;
    @Nullable @SerializedName("firstName") public String firstName;
    @Nullable @SerializedName("lastName") public String lastName;
    @Nullable @SerializedName("telegramId") public String telegramId;
    @Nullable @SerializedName("role") public String role;

    public String initials() {
        String first = firstName == null || firstName.trim().isEmpty() ? "" : firstName.trim().substring(0, 1);
        String last = lastName == null || lastName.trim().isEmpty() ? "" : lastName.trim().substring(0, 1);
        String value = (first + last).toUpperCase(java.util.Locale.ROOT);
        return value.isEmpty() ? "?" : value;
    }
}
