package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Modelo para dados de transação
 */
public class TransactionDto {

    @NonNull
    @SerializedName("id")
    private String id;

    @NonNull
    @SerializedName("accountId")
    private String accountId;

    @SerializedName("amount")
    private double amount;

    @SerializedName("date")
    private long date;

    @NonNull
    @SerializedName("category")
    private String category;

    @Nullable
    @SerializedName("note")
    private String note;

    @NonNull
    @SerializedName("type")
    private String type; // "income" ou "expense"

    public TransactionDto(@NonNull String id, @NonNull String accountId, double amount,
                         long date, @NonNull String category, @Nullable String note,
                         @NonNull String type) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.note = note;
        this.type = type;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(@NonNull String accountId) {
        this.accountId = accountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    public void setCategory(@NonNull String category) {
        this.category = category;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "TransactionDto{" +
                "id='" + id + '\'' +
                ", accountId='" + accountId + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", category='" + category + '\'' +
                ", note='" + note + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
