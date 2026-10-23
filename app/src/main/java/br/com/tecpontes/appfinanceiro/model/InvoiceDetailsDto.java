package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Resumo e lançamentos retornados por GET /api/invoices/{cardId}/{year}/{month}. */
public class InvoiceDetailsDto {
    @Nullable @SerializedName("id") public String id;
    @Nullable @SerializedName("closingDate") public String closingDate;
    @Nullable @SerializedName("dueDate") public String dueDate;
    @Nullable @SerializedName("status") public String status;
    @Nullable @SerializedName("previousBalance") public Double previousBalance;
    @Nullable @SerializedName("previousTotalPaid") public Double previousTotalPaid;
    @Nullable @SerializedName("totalPaid") public Double totalPaid;
    @Nullable @SerializedName("totalAmount") public Double totalAmount;
    @Nullable @SerializedName("remainingBalance") public Double remainingBalance;
    @Nullable @SerializedName("ownRemainingBalance") public Double ownRemainingBalance;
    @Nullable @SerializedName("hasPartialPayment") public Boolean hasPartialPayment;
    @Nullable @SerializedName("fixedExpenses") public Double fixedExpenses;
    @Nullable @SerializedName("futureInstallments") public Double futureInstallments;
    @Nullable @SerializedName("usedLimit") public Double usedLimit;
    @Nullable @SerializedName("availableLimit") public Double availableLimit;
    @Nullable @SerializedName("scheduledPayments") public Double scheduledPayments;
    @Nullable @SerializedName("availableAfterPayments") public Double availableAfterPayments;
    @Nullable @SerializedName("totalConciliado") public Double totalConciliado;
    @Nullable @SerializedName("totalNaoConciliado") public Double totalNaoConciliado;
    @Nullable @SerializedName("transactions") public List<TransactionListItemDto> transactions;
    @Nullable @SerializedName("payments") public List<InvoicePaymentEntry> payments;

    public List<TransactionListItemDto> getTransactions() {
        return transactions == null ? new ArrayList<>() : transactions;
    }

    public List<InvoicePaymentEntry> getPayments() {
        return payments == null ? new ArrayList<>() : payments;
    }

    public double amount(@Nullable Double value) { return value == null ? 0d : value; }

    public static class InvoicePaymentEntry {
        @Nullable @SerializedName("invoice_payments") public PaymentInfo payment;
        @Nullable @SerializedName("transactions") public PaymentTransaction transaction;
    }

    public static class PaymentInfo {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("amountPaid") public String amountPaid;
        @Nullable @SerializedName("paymentDate") public String paymentDate;
        @Nullable @SerializedName("transactionId") public String transactionId;
    }

    public static class PaymentTransaction {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("accountId") public String accountId;
        @Nullable @SerializedName("description") public String description;
    }
}
