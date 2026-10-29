package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Resposta detalhada de GET /api/transactions, usada para apresentar a lista mobile. */
public class TransactionListItemDto {

    @Nullable @SerializedName("id") public String id;
    @Nullable @SerializedName("description") public String description;
    @Nullable @SerializedName("amount") public String amount;
    @Nullable @SerializedName("date") public String date;
    @Nullable @SerializedName("type") public String type;
    @Nullable @SerializedName("accountId") public String accountId;
    @Nullable @SerializedName("cardId") public String cardId;
    @Nullable @SerializedName("invoiceId") public String invoiceId;
    @Nullable @SerializedName("categoryId") public String categoryId;
    @Nullable @SerializedName("installmentNumber") public Integer installmentNumber;
    @Nullable @SerializedName("totalInstallments") public Integer totalInstallments;
    @Nullable @SerializedName("totalValue") public String totalValue;
    @Nullable @SerializedName("installmentValueType") public String installmentValueType;
    @Nullable @SerializedName("installmentGroupId") public String installmentGroupId;
    @Nullable @SerializedName("fixedExpenseGroupId") public String fixedExpenseGroupId;
    @Nullable @SerializedName("isInstallment") public Boolean isInstallment;
    @Nullable @SerializedName("isFixedExpense") public Boolean isFixedExpense;
    @Nullable @SerializedName("conciliado") public Boolean reconciled;
    @Nullable @SerializedName("observacao") public String note;
    @Nullable @SerializedName("category") public CategoryInfo category;
    @Nullable @SerializedName("account") public AccountInfo account;
    @Nullable @SerializedName("card") public CardInfo card;
    @Nullable @SerializedName("invoice") public InvoiceInfo invoice;

    private Double runningBalance;

    /**
     * Campos transitórios usados apenas na lista local. Quando preenchidos, o item
     * representa a fatura consolidada de um cartão, não um lançamento individual.
     */
    @Nullable public transient List<TransactionListItemDto> invoiceTransactions;
    @Nullable public transient InvoicePendingDto invoicePending;
    public transient boolean partialInvoice;
    public transient boolean offlinePending;
    public transient boolean offlineSyncError;

    public boolean isConsolidatedInvoice() {
        return invoiceTransactions != null;
    }

    public boolean isOfflinePending() {
        return offlinePending;
    }

    public double getAmountValue() {
        if (amount == null) return 0;
        try {
            return Double.parseDouble(amount.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public long getDateMillis() {
        return parseTimestamp(date);
    }

    public long getInvoiceDueDateMillis() {
        return parseTimestamp(invoice == null ? null : invoice.dueDate);
    }

    private static long parseTimestamp(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (RuntimeException ignored) {
            try {
                return LocalDate.parse(value.substring(0, Math.min(10, value.length())))
                        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (RuntimeException ignoredDate) {
                return 0;
            }
        }
    }

    public String getCategoryName() {
        return category != null && category.name != null ? category.name : "Sem categoria";
    }

    public String getSourceName() {
        if (account != null && account.name != null) return account.name;
        if (card != null && card.name != null) return card.name;
        return "Conta";
    }

    public boolean isIncome() {
        return "INCOME".equalsIgnoreCase(type) || "INITIAL_BALANCE".equalsIgnoreCase(type);
    }

    public boolean isReconciled() {
        return Boolean.TRUE.equals(reconciled);
    }

    public boolean isInstallmentTransaction() {
        return Boolean.TRUE.equals(isInstallment);
    }

    public boolean isFixedExpenseTransaction() {
        return Boolean.TRUE.equals(isFixedExpense);
    }

    public boolean hasRunningBalance() {
        return runningBalance != null;
    }

    public double getRunningBalance() {
        return runningBalance != null ? runningBalance : 0;
    }

    public void setRunningBalance(double runningBalance) {
        this.runningBalance = runningBalance;
    }

    public void clearRunningBalance() {
        this.runningBalance = null;
    }

    public static class CategoryInfo {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("name") public String name;
        @Nullable @SerializedName("color") public String color;
    }

    public static class AccountInfo {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("name") public String name;
    }

    public static class CardInfo {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("name") public String name;
        @Nullable @SerializedName("debitAccountId") public String debitAccountId;
    }

    public static class InvoiceInfo {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("dueDate") public String dueDate;
        @Nullable @SerializedName("status") public String status;
        @Nullable @SerializedName("totalAmount") public String totalAmount;
    }
}
