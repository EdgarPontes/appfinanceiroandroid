package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Membro ativo do espaço financeiro. */
public class FinanceSpaceMemberDto {
    @SerializedName("id") public String id;
    @SerializedName("userId") public String userId;
    @SerializedName("email") public String email;
    @SerializedName("firstName") @Nullable public String firstName;
    @SerializedName("lastName") @Nullable public String lastName;
    @SerializedName("role") public String role;
    @SerializedName("joinedAt") @Nullable public String joinedAt;

    public String displayName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? email : full;
    }
}
