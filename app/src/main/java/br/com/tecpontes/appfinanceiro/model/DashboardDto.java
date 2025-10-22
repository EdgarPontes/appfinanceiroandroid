package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Modelo para dados do dashboard
 */
public class DashboardDto {

    @SerializedName("totalBalance")
    private double totalBalance;

    @SerializedName("monthlyIncome")
    private double monthlyIncome;

    @SerializedName("monthlyExpenses")
    private double monthlyExpenses;

    @SerializedName("accountsCount")
    private int accountsCount;

    @NonNull
    @SerializedName("recentTransactions")
    private List<TransactionDto> recentTransactions;

    @Nullable
    @SerializedName("currency")
    private String currency;

    public DashboardDto(double totalBalance, double monthlyIncome, double monthlyExpenses,
                       int accountsCount, @NonNull List<TransactionDto> recentTransactions,
                       @Nullable String currency) {
        this.totalBalance = totalBalance;
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.accountsCount = accountsCount;
        this.recentTransactions = recentTransactions;
        this.currency = currency;
    }

    public double getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double totalBalance) {
        this.totalBalance = totalBalance;
    }

    public double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public double getMonthlyExpenses() {
        return monthlyExpenses;
    }

    public void setMonthlyExpenses(double monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses;
    }

    public int getAccountsCount() {
        return accountsCount;
    }

    public void setAccountsCount(int accountsCount) {
        this.accountsCount = accountsCount;
    }

    @NonNull
    public List<TransactionDto> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(@NonNull List<TransactionDto> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }

    @Nullable
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(@Nullable String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "DashboardDto{" +
                "totalBalance=" + totalBalance +
                ", monthlyIncome=" + monthlyIncome +
                ", monthlyExpenses=" + monthlyExpenses +
                ", accountsCount=" + accountsCount +
                ", recentTransactionsCount=" + recentTransactions.size() +
                ", currency='" + currency + '\'' +
                '}';
    }
}
