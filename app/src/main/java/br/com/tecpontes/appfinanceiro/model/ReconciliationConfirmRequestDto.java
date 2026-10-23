package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReconciliationConfirmRequestDto {
    @SerializedName("accountId") public String accountId;
    @SerializedName("entries") public List<StatementEntryDto> entries;
    @SerializedName("toReconcile") public List<ReconcileItem> toReconcile;
    @SerializedName("toCreate") public List<CreateItem> toCreate;

    public ReconciliationConfirmRequestDto(String accountId, List<StatementEntryDto> entries,
                                           List<ReconcileItem> toReconcile, List<CreateItem> toCreate) {
        this.accountId = accountId;
        this.entries = entries;
        this.toReconcile = toReconcile;
        this.toCreate = toCreate;
    }

    public static class ReconcileItem {
        @SerializedName("statementIndex") public int statementIndex;
        @SerializedName("transactionId") public String transactionId;
        public ReconcileItem(int index, String id) { statementIndex = index; transactionId = id; }
    }

    public static class CreateItem {
        @SerializedName("statementIndex") public int statementIndex;
        @SerializedName("categoryId") public String categoryId;
        public CreateItem(int index, String categoryId) { statementIndex = index; this.categoryId = categoryId; }
    }
}
