package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.Map;

/** Resposta de GET /api/balance-history/batch. */
public class BalanceHistoryDto {
    @Nullable @SerializedName("byAccount") public Map<String, String> byAccount;
    @Nullable @SerializedName("total") public String total;

    public double getTotalValue() {
        if (total == null) return 0;
        try {
            return Double.parseDouble(total.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
