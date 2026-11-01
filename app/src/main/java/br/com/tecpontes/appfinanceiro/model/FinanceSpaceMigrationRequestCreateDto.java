package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Corpo opcional de POST /finance-spaces/{id}/migration-requests. */
public class FinanceSpaceMigrationRequestCreateDto {
    @SerializedName("sourceSpaceId") @Nullable public final String sourceSpaceId;

    public FinanceSpaceMigrationRequestCreateDto(@Nullable String sourceSpaceId) {
        this.sourceSpaceId = sourceSpaceId;
    }
}
