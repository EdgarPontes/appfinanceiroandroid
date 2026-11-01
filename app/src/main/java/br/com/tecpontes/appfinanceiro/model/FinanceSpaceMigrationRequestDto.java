package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Solicitação de migração dos dados pessoais de um membro para o espaço compartilhado. */
public class FinanceSpaceMigrationRequestDto {
    @SerializedName("id") public String id;
    @SerializedName("targetSpaceId") public String targetSpaceId;
    @SerializedName("sourceSpaceId") public String sourceSpaceId;
    @SerializedName("sourceSpaceName") @Nullable public String sourceSpaceName;
    @SerializedName("targetSpaceName") @Nullable public String targetSpaceName;
    @SerializedName("status") public String status;
    @SerializedName("rejectionReason") @Nullable public String rejectionReason;
    @SerializedName("summary") @Nullable public Summary summary;
    @SerializedName("createdAt") @Nullable public String createdAt;
    @SerializedName("reviewedAt") @Nullable public String reviewedAt;
    @SerializedName("completedAt") @Nullable public String completedAt;

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public static class Summary {
        @SerializedName("accounts") public int accounts;
        @SerializedName("cards") public int cards;
        @SerializedName("categories") public int categories;
        @SerializedName("transactions") public int transactions;
        @SerializedName("categoryGoals") public int categoryGoals;

        public int total() {
            return accounts + cards + categories + transactions + categoryGoals;
        }
    }
}
