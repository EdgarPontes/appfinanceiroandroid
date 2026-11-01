package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

/** Corpo de POST /finance-spaces/{id}/transfer-ownership. */
public class FinanceSpaceOwnershipTransferRequestDto {
    @SerializedName("userId") public final String userId;

    public FinanceSpaceOwnershipTransferRequestDto(@NonNull String userId) {
        this.userId = userId;
    }
}
