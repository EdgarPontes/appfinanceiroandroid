package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Corpo aceito por POST /api/invoices/{id}/pay. */
public class InvoicePaymentRequestDto {
    @SerializedName("amount") public String amount;
    @SerializedName("accountId") public String accountId;
    @SerializedName("paymentDate") public String paymentDate;
    @Nullable @SerializedName("categoryId") public String categoryId;
}
