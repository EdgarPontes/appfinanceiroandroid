package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Espaço financeiro disponível para o usuário autenticado. */
public class FinanceSpaceDto {
    @SerializedName("id") public String id;
    @SerializedName("name") public String name;
    @SerializedName("ownerUserId") public String ownerUserId;
    @SerializedName("role") public String role;
    @SerializedName("joinedAt") @Nullable public String joinedAt;

    public boolean isOwner() {
        return "OWNER".equalsIgnoreCase(role);
    }
}
