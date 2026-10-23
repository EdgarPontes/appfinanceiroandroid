package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Campos aceitos por POST/PUT /api/cards. */
public class CreditCardRequestDto {
    @SerializedName("name") public String name;
    @SerializedName("limit") public String limit;
    @Nullable @SerializedName("bandeira") public String bandeira;
    @SerializedName("closingDay") public Integer closingDay;
    @SerializedName("dueDay") public Integer dueDay;
    @Nullable @SerializedName("debitAccountId") public String debitAccountId;
    @Nullable @SerializedName("isActive") public Boolean isActive;
}
