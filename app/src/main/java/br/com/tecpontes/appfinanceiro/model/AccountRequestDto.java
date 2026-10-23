package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Campos aceitos por POST/PUT /api/accounts. */
public class AccountRequestDto {
    @SerializedName("name") public String name;
    @SerializedName("balance") public String balance;
    @SerializedName("type") public String type;
    @Nullable @SerializedName("branch") public String branch;
    @Nullable @SerializedName("accountNumber") public String accountNumber;
    @Nullable @SerializedName("isActive") public Boolean isActive;
}
