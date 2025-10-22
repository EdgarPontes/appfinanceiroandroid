package br.com.tecpontes.appfinanceiro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidade Room para tabela de contas
 */
@Entity(tableName = "accounts")
public class Account {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String name;

    private double balance;

    @NonNull
    private String currency;

    @Nullable
    private String description;

    // Campo para controle de sincronização
    private long lastSync;

    public Account(@NonNull String id, @NonNull String name, double balance,
                  @NonNull String currency, @Nullable String description) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.currency = currency;
        this.description = description;
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
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @NonNull
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(@NonNull String currency) {
        this.currency = currency;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public long getLastSync() {
        return lastSync;
    }

    public void setLastSync(long lastSync) {
        this.lastSync = lastSync;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                ", lastSync=" + lastSync +
                '}';
    }
}
