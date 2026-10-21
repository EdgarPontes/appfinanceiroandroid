package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Campos aceitos pelas rotas POST/PUT de transações. */
public class TransactionWriteDto {
    @Nullable @SerializedName("description") public String description;
    @Nullable @SerializedName("amount") public String amount;
    @Nullable @SerializedName("date") public String date;
    @Nullable @SerializedName("type") public String type;
    @Nullable @SerializedName("categoryId") public String categoryId;
    @Nullable @SerializedName("accountId") public String accountId;
    @Nullable @SerializedName("cardId") public String cardId;
    @Nullable @SerializedName("observacao") public String note;
    @Nullable @SerializedName("updateScope") public String updateScope;
    @Nullable @SerializedName("isInstallment") public Boolean isInstallment;
    @Nullable @SerializedName("totalInstallments") public Integer totalInstallments;
    @Nullable @SerializedName("installmentValueType") public String installmentValueType;
    @Nullable @SerializedName("installmentGroupId") public String installmentGroupId;
    @Nullable @SerializedName("totalValue") public String totalValue;
    @Nullable @SerializedName("isFixedExpense") public Boolean isFixedExpense;
    @Nullable @SerializedName("fixedExpenseGroupId") public String fixedExpenseGroupId;
}
