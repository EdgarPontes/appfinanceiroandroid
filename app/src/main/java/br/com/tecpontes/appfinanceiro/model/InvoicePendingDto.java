package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Valores de pendência de uma fatura retornados pela API. */
public class InvoicePendingDto {
    @Nullable @SerializedName("totalAmount") public String totalAmount;
    @Nullable @SerializedName("totalPaid") public String totalPaid;
    @Nullable @SerializedName("pending") public String pending;
    @Nullable @SerializedName("previousBalance") public String previousBalance;
    @Nullable @SerializedName("amountDue") public String amountDue;
    @Nullable @SerializedName("status") public String status;

    public double pendingValue() { return parse(pending); }
    public double totalPaidValue() { return parse(totalPaid); }
    public double previousBalanceValue() { return parse(previousBalance); }
    public double amountDueValue() { return parse(amountDue); }

    private static double parse(@Nullable String value) {
        if (value == null) return 0;
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
