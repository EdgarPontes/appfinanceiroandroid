package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Modelo para dados de conta
 */
public class AccountDto {

    @NonNull
    @SerializedName("id")
    private String id;

    @NonNull
    @SerializedName("name")
    private String name;

    @SerializedName("balance")
    private double balance;

    @NonNull
    @SerializedName("currency")
    private String currency;

    @Nullable
    @SerializedName("description")
    private String description;

    public AccountDto(@NonNull String id, @NonNull String name, double balance,
                     @NonNull String currency, @Nullable String description) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.currency = currency;
        this.description = description;
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

    @Override
    public String toString() {
        return "AccountDto{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
