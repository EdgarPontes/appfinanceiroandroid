package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Cartão retornado por GET /api/cards, incluindo o resumo de limite calculado pela API. */
public class CreditCardDto {
    @Nullable @SerializedName("id") private String id;
    @Nullable @SerializedName("name") private String name;
    @Nullable @SerializedName("bandeira") private String bandeira;
    @Nullable @SerializedName("limit") private String limit;
    @Nullable @SerializedName("usedAmount") private String usedAmount;
    @Nullable @SerializedName("availableLimit") private String availableLimit;
    @Nullable @SerializedName("closingDay") private Integer closingDay;
    @Nullable @SerializedName("dueDay") private Integer dueDay;
    @Nullable @SerializedName("debitAccountId") private String debitAccountId;
    @Nullable @SerializedName("debitAccount") private AccountDto debitAccount;
    @Nullable @SerializedName("currentInvoice") private CardInvoiceSummary currentInvoice;

    @Nullable public String getId() { return id; }
    @Nullable public String getName() { return name; }
    @Nullable public String getBandeira() { return bandeira; }
    @Nullable public String getLimit() { return limit; }
    @Nullable public String getUsedAmount() { return usedAmount; }
    @Nullable public String getAvailableLimit() { return availableLimit; }
    @Nullable public Integer getClosingDay() { return closingDay; }
    @Nullable public Integer getDueDay() { return dueDay; }
    @Nullable public String getDebitAccountId() { return debitAccountId; }
    @Nullable public AccountDto getDebitAccount() { return debitAccount; }
    @Nullable public CardInvoiceSummary getCurrentInvoice() { return currentInvoice; }

    @Override public String toString() {
        String cardName = name == null || name.trim().isEmpty() ? "Cartão" : name;
        return bandeira == null || bandeira.trim().isEmpty() ? cardName : cardName + " · " + bandeira;
    }

    public static class CardInvoiceSummary {
        @Nullable @SerializedName("referenceMonth") public Integer referenceMonth;
        @Nullable @SerializedName("referenceYear") public Integer referenceYear;
        @Nullable @SerializedName("status") public String status;
    }
}
