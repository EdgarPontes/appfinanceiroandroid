package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Pedido pendente para excluir lançamento criado por outro membro. */
public class FinanceSpaceDeletionRequestDto {
    @SerializedName("id") public String id;
    @SerializedName("spaceId") public String spaceId;
    @SerializedName("transactionId") @Nullable public String transactionId;
    @SerializedName("requestedByUserId") @Nullable public String requestedByUserId;
    @SerializedName("requestedByEmail") @Nullable public String requestedByEmail;
    @SerializedName("requestedByFirstName") @Nullable public String requestedByFirstName;
    @SerializedName("requestedByLastName") @Nullable public String requestedByLastName;
    @SerializedName("reason") @Nullable public String reason;
    @SerializedName("status") public String status;
    @SerializedName("createdAt") @Nullable public String createdAt;
    @SerializedName("description") @Nullable public String description;
    @SerializedName("amount") @Nullable public String amount;
    @SerializedName("transactionDate") @Nullable public String transactionDate;

    public String requesterName() {
        String first = requestedByFirstName == null ? "" : requestedByFirstName.trim();
        String last = requestedByLastName == null ? "" : requestedByLastName.trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? requestedByEmail : full;
    }
}
