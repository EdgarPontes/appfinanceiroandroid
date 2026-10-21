package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Modelo da resposta de GET /api/dashboard. Valores monetários chegam como strings decimais. */
public class DashboardDto {

    @Nullable @SerializedName("totalBalance") private String totalBalance;
    @Nullable @SerializedName("periodNetFlow") private String periodNetFlow;
    @Nullable @SerializedName("totalIncome") private String totalIncome;
    @Nullable @SerializedName("totalExpenses") private String totalExpenses;
    @Nullable @SerializedName("availableCredit") private String availableCredit;
    @Nullable @SerializedName("totalCreditLimit") private String totalCreditLimit;
    @Nullable @SerializedName("currentSavingsBalance") private String currentSavingsBalance;
    @Nullable @SerializedName("effectiveSavingsBalance") private String effectiveSavingsBalance;
    @Nullable @SerializedName("investmentsBalance") private String investmentsBalance;
    @Nullable @SerializedName("creditCardDebt") private String creditCardDebt;
    @Nullable @SerializedName("futureExpenses") private String futureExpenses;
    @Nullable @SerializedName("netBalance") private String netBalance;

    @SerializedName("accountsDetail") private List<AccountDetail> accountsDetail;
    @SerializedName("investmentsDetail") private List<AccountDetail> investmentsDetail;
    @SerializedName("cardsDetail") private List<CardDetail> cardsDetail;
    @SerializedName("futureExpensesDetail") private List<AmountDetail> futureExpensesDetail;
    @SerializedName("expensesByCategory") private List<CategoryAmount> expensesByCategory;
    @SerializedName("incomeByCategory") private List<CategoryAmount> incomeByCategory;
    @SerializedName("monthlyEvolution") private List<MonthlyEvolution> monthlyEvolution;
    @SerializedName("categoryGoalsProgress") private List<CategoryGoalProgress> categoryGoalsProgress;

    /** Inicializa listas porque o Gson mantém esses valores quando uma chave não vem na resposta. */
    public DashboardDto() {
        accountsDetail = new ArrayList<>();
        investmentsDetail = new ArrayList<>();
        cardsDetail = new ArrayList<>();
        futureExpensesDetail = new ArrayList<>();
        expensesByCategory = new ArrayList<>();
        incomeByCategory = new ArrayList<>();
        monthlyEvolution = new ArrayList<>();
        categoryGoalsProgress = new ArrayList<>();
    }

    @Nullable public String getTotalBalance() { return totalBalance; }
    @Nullable public String getPeriodNetFlow() { return periodNetFlow; }
    @Nullable public String getTotalIncome() { return totalIncome; }
    @Nullable public String getTotalExpenses() { return totalExpenses; }
    @Nullable public String getAvailableCredit() { return availableCredit; }
    @Nullable public String getTotalCreditLimit() { return totalCreditLimit; }
    @Nullable public String getCurrentSavingsBalance() { return currentSavingsBalance; }
    @Nullable public String getEffectiveSavingsBalance() { return effectiveSavingsBalance; }
    @Nullable public String getInvestmentsBalance() { return investmentsBalance; }
    @Nullable public String getCreditCardDebt() { return creditCardDebt; }
    @Nullable public String getFutureExpenses() { return futureExpenses; }
    @Nullable public String getNetBalance() { return netBalance; }

    public List<AccountDetail> getAccountsDetail() { return safeList(accountsDetail); }
    public List<AccountDetail> getInvestmentsDetail() { return safeList(investmentsDetail); }
    public List<CardDetail> getCardsDetail() { return safeList(cardsDetail); }
    public List<AmountDetail> getFutureExpensesDetail() { return safeList(futureExpensesDetail); }
    public List<CategoryAmount> getExpensesByCategory() { return safeList(expensesByCategory); }
    public List<CategoryAmount> getIncomeByCategory() { return safeList(incomeByCategory); }
    public List<MonthlyEvolution> getMonthlyEvolution() { return safeList(monthlyEvolution); }
    public List<CategoryGoalProgress> getCategoryGoalsProgress() { return safeList(categoryGoalsProgress); }

    private static <T> List<T> safeList(@Nullable List<T> values) {
        return values != null ? values : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "DashboardDto{" +
                "totalBalance='" + totalBalance + '\'' +
                ", totalIncome='" + totalIncome + '\'' +
                ", totalExpenses='" + totalExpenses + '\'' +
                ", accounts=" + safeList(accountsDetail).size() +
                ", monthlyEvolution=" + safeList(monthlyEvolution).size() +
                '}';
    }

    public static class CategoryAmount {
        @Nullable @SerializedName("categoryId") public String categoryId;
        @Nullable @SerializedName("category") public String category;
        @Nullable @SerializedName("amount") public String amount;
        @Nullable @SerializedName("color") public String color;
    }

    public static class MonthlyEvolution {
        @Nullable @SerializedName("month") public String month;
        @Nullable @SerializedName("income") public String income;
        @Nullable @SerializedName("expenses") public String expenses;
    }

    public static class AccountDetail {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("name") public String name;
        @Nullable @SerializedName("balance") public String balance;
        @Nullable @SerializedName("pendingInvoices") public String pendingInvoices;
        @Nullable @SerializedName("effectiveBalance") public String effectiveBalance;
    }

    public static class AmountDetail {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("name") public String name;
        @Nullable @SerializedName("amount") public String amount;
    }

    public static class CardDetail {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("name") public String name;
        @Nullable @SerializedName("usedAmount") public String usedAmount;
        @Nullable @SerializedName("limit") public String limit;
    }

    public static class CategoryGoalProgress {
        @Nullable @SerializedName("categoryId") public String categoryId;
        @Nullable @SerializedName("category") public String category;
        @Nullable @SerializedName("goalAmount") public String goalAmount;
        @Nullable @SerializedName("spentAmount") public String spentAmount;
        @Nullable @SerializedName("progressPercent") public String progressPercent;
        @Nullable @SerializedName("color") public String color;
    }
}
