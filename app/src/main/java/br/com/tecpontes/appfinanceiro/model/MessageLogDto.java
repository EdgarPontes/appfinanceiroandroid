package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Notificação já disparada (/api/message-logs). */
public class MessageLogDto {

    @Nullable @SerializedName("id") public String id;
    @Nullable @SerializedName("configId") public String configId;
    @Nullable @SerializedName("status") public String status;
    @Nullable @SerializedName("sentAt") public String sentAt;
    @Nullable @SerializedName("readAt") public String readAt;
    @Nullable @SerializedName("triggeredBy") public String triggeredBy;
    @Nullable @SerializedName("targetDate") public String targetDate;
    @Nullable @SerializedName("priority") public String priority;
    @Nullable @SerializedName("type") public String type;
    @Nullable @SerializedName("content") public String content;
    @Nullable @SerializedName("invoiceId") public String invoiceId;
    @Nullable @SerializedName("invoiceStatus") public String invoiceStatus;
    @Nullable @SerializedName("archivedAt") public String archivedAt;

    public boolean isRead() {
        return readAt != null && !readAt.isEmpty();
    }
}
