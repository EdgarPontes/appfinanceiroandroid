package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Corpo do PUT /api/user/settings.
 *
 * <p>Espelha o formulário da tela de configurações do app web: todos os campos são enviados
 * juntos, sem {@code dashboardWidgets}, para não sobrescrever o layout do dashboard.</p>
 */
public class UserSettingsUpdateRequestDto {

    @SerializedName("savingsGoalPercentage") public String savingsGoalPercentage;
    @Nullable @SerializedName("monthlyBudgetGoal") public String monthlyBudgetGoal;
    @SerializedName("expenseAlertThreshold") public String expenseAlertThreshold;
    @SerializedName("currency") public String currency;
    @SerializedName("isNotDisturbActive") public boolean isNotDisturbActive;
    @SerializedName("notDisturbStart") public String notDisturbStart;
    @SerializedName("notDisturbEnd") public String notDisturbEnd;
    @SerializedName("messageRetentionDays") public int messageRetentionDays;
}
