package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Histórico retornado por GET /api/balance-history?accountId=... */
public class AccountBalanceHistoryDto {
    @Nullable @SerializedName("accountId") public String accountId;
    @Nullable @SerializedName("accountName") public String accountName;
    @Nullable @SerializedName("history") public List<Item> history;

    public List<Item> getHistory() { return history == null ? new ArrayList<>() : history; }

    public static class Item {
        @Nullable @SerializedName("date") public String date;
        @Nullable @SerializedName("balance") public String balance;
        @Nullable @SerializedName("change") public String change;
        @Nullable @SerializedName("transactionsCount") public Integer transactionsCount;
    }
}
