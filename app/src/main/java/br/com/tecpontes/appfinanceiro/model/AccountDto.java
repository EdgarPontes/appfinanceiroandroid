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

    @Nullable @SerializedName("type") private String type;
    @Nullable @SerializedName("isActive") private Boolean isActive;
    @Nullable @SerializedName("branch") private String branch;
    @Nullable @SerializedName("accountNumber") private String accountNumber;
    @Nullable @SerializedName("pendingInvoices") private String pendingInvoices;
    @Nullable @SerializedName("effectiveBalance") private String effectiveBalance;
    @Nullable @SerializedName("openingBalance") private String openingBalance;

    /** A API web não expõe moeda por conta; BRL é o padrão atual do produto. */
    public AccountDto() {
        this.currency = "BRL";
    }

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
        return currency == null || currency.trim().isEmpty() ? "BRL" : currency;
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

    @Nullable public String getType() { return type; }
    public void setType(@Nullable String type) { this.type = type; }
    public boolean isActive() { return isActive == null || isActive; }
    public void setActive(boolean active) { isActive = active; }
    @Nullable public String getBranch() { return branch; }
    public void setBranch(@Nullable String branch) { this.branch = branch; }
    @Nullable public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(@Nullable String accountNumber) { this.accountNumber = accountNumber; }
    @Nullable public String getPendingInvoices() { return pendingInvoices; }
    public void setPendingInvoices(@Nullable String pendingInvoices) { this.pendingInvoices = pendingInvoices; }
    @Nullable public String getEffectiveBalance() { return effectiveBalance; }
    public void setEffectiveBalance(@Nullable String effectiveBalance) { this.effectiveBalance = effectiveBalance; }
    @Nullable public String getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(@Nullable String openingBalance) { this.openingBalance = openingBalance; }

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
