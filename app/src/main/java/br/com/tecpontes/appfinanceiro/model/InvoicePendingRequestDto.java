package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Corpo aceito por POST /api/invoices/pending. */
public class InvoicePendingRequestDto {
    @SerializedName("invoiceIds") public final List<String> invoiceIds;

    public InvoicePendingRequestDto(List<String> invoiceIds) {
        this.invoiceIds = invoiceIds;
    }
}
