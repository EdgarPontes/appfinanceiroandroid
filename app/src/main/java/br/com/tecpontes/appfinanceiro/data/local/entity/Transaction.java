package br.com.tecpontes.appfinanceiro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidade Room para tabela de transações
 */
@Entity(
    tableName = "transactions",
    foreignKeys = @ForeignKey(
        entity = Account.class,
        parentColumns = "id",
        childColumns = "accountId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = "accountId"),
        @Index(value = {"accountId", "date"}),
        @Index(value = "category")
    }
)
public class Transaction {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String accountId;

    private double amount;

    private long date;

    @NonNull
    private String category;

    @Nullable
    private String note;

    @NonNull
    private String type; // "income" ou "expense"

    // Campo para controle de sincronização
    private long lastSync;

    public Transaction(@NonNull String id, @NonNull String accountId, double amount,
                      long date, @NonNull String category, @Nullable String note,
                      @NonNull String type) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.note = note;
        this.type = type;
        this.lastSync = System.currentTimeMillis();
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

    public long getLastSync() {
        return lastSync;
    }

    public void setLastSync(long lastSync) {
        this.lastSync = lastSync;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", accountId='" + accountId + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", category='" + category + '\'' +
                ", note='" + note + '\'' +
                ", type='" + type + '\'' +
                ", lastSync=" + lastSync +
                '}';
    }
}
