package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

/** Payload aceito por POST /api/transactions/transfer. */
public class TransactionTransferRequestDto {
    @NonNull @SerializedName("description") public String description;
    @NonNull @SerializedName("amount") public String amount;
    @NonNull @SerializedName("date") public String date;
    @NonNull @SerializedName("fromAccountId") public String fromAccountId;
    @NonNull @SerializedName("toAccountId") public String toAccountId;
}
