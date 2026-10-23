package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Resultado da prévia de conciliação de um extrato. */
public class ReconciliationPreviewDto {
    @Nullable @SerializedName("accountId") public String accountId;
    @Nullable @SerializedName("entries") public List<StatementEntryDto> entries;
    @Nullable @SerializedName("matches") public List<Match> matches;
    @Nullable @SerializedName("alreadyImportedIndices") public List<Integer> alreadyImportedIndices;
    @Nullable @SerializedName("unmatchedStatementIndices") public List<Integer> unmatchedStatementIndices;
    @Nullable @SerializedName("unmatchedTransactionIds") public List<String> unmatchedTransactionIds;
    @Nullable @SerializedName("suggestions") public Map<String, List<Suggestion>> suggestions;
    @Nullable @SerializedName("summary") public Summary summary;

    public List<StatementEntryDto> getEntries() { return entries == null ? new ArrayList<>() : entries; }
    public List<Match> getMatches() { return matches == null ? new ArrayList<>() : matches; }
    public List<Integer> getAlreadyImportedIndices() { return alreadyImportedIndices == null ? new ArrayList<>() : alreadyImportedIndices; }
    public List<Integer> getUnmatchedStatementIndices() { return unmatchedStatementIndices == null ? new ArrayList<>() : unmatchedStatementIndices; }

    public static class Match {
        @SerializedName("statementIndex") public int statementIndex;
        @Nullable @SerializedName("transactionId") public String transactionId;
        @Nullable @SerializedName("score") public Integer score;
        @Nullable @SerializedName("confidence") public String confidence;
        @Nullable @SerializedName("reason") public String reason;
        @Nullable @SerializedName("transaction") public ExistingTransaction transaction;
    }

    public static class ExistingTransaction {
        @Nullable @SerializedName("id") public String id;
        @Nullable @SerializedName("description") public String description;
        @Nullable @SerializedName("amount") public String amount;
        @Nullable @SerializedName("date") public String date;
        @Nullable @SerializedName("categoryId") public String categoryId;
    }

    public static class Suggestion {
        @Nullable @SerializedName("categoryId") public String categoryId;
        @Nullable @SerializedName("score") public Integer score;
        @Nullable @SerializedName("categoryName") public String categoryName;
        @Nullable @SerializedName("categoryColor") public String categoryColor;
    }

    public static class Summary {
        @Nullable @SerializedName("high") public Integer high;
        @Nullable @SerializedName("medium") public Integer medium;
        @Nullable @SerializedName("low") public Integer low;
        @Nullable @SerializedName("unmatchedStatement") public Integer unmatchedStatement;
        @Nullable @SerializedName("unmatchedSystem") public Integer unmatchedSystem;
        @Nullable @SerializedName("alreadyImported") public Integer alreadyImported;
    }
}
