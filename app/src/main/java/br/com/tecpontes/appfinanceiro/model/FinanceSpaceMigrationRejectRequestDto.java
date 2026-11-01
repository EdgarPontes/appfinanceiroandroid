package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Corpo opcional de recusa de migração. */
public class FinanceSpaceMigrationRejectRequestDto {
    @SerializedName("reason") @Nullable public final String reason;

    public FinanceSpaceMigrationRejectRequestDto(@Nullable String reason) {
        this.reason = reason;
    }
}
