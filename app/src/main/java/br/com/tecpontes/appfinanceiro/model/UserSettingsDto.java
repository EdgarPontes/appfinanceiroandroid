package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Configurações do usuário retornadas por GET /api/user/settings. */
public class UserSettingsDto {

    @Nullable @SerializedName("savingsGoalPercentage") public String savingsGoalPercentage;
    @Nullable @SerializedName("monthlyBudgetGoal") public String monthlyBudgetGoal;
    @Nullable @SerializedName("expenseAlertThreshold") public String expenseAlertThreshold;
    @Nullable @SerializedName("currency") public String currency;
    @Nullable @SerializedName("isNotDisturbActive") public Boolean isNotDisturbActive;
    @Nullable @SerializedName("notDisturbStart") public String notDisturbStart;
    @Nullable @SerializedName("notDisturbEnd") public String notDisturbEnd;
    @Nullable @SerializedName("messageRetentionDays") public Integer messageRetentionDays;
    @Nullable @SerializedName("dashboardWidgets") public Object dashboardWidgets;

    public boolean isNotDisturbActive() {
        return Boolean.TRUE.equals(isNotDisturbActive);
    }

    public int retentionDays(int fallback) {
        return messageRetentionDays == null ? fallback : messageRetentionDays;
    }
}
