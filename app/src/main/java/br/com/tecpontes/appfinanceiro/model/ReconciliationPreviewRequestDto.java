package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReconciliationPreviewRequestDto {
    @SerializedName("accountId") public String accountId;
    @SerializedName("entries") public List<StatementEntryDto> entries;

    public ReconciliationPreviewRequestDto(String accountId, List<StatementEntryDto> entries) {
        this.accountId = accountId;
        this.entries = entries;
    }
}
