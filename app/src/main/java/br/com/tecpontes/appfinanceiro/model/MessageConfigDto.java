package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Configuração/template de notificação (/api/message-configs). */
public class MessageConfigDto {

    public static final String TYPE_CUSTOM_REMINDER = "custom_reminder";
    public static final String TYPE_FINANCIAL_DUE_DATE = "financial_due_date";
    public static final String TYPE_PAYMENT_CONFIRMATION = "payment_confirmation";
    public static final String TYPE_DELAY_NOTICE = "delay_notice";

    @Nullable @SerializedName("id") public String id;
    @Nullable @SerializedName("title") public String title;
    @Nullable @SerializedName("defaultText") public String defaultText;
    @Nullable @SerializedName("type") public String type;
    @Nullable @SerializedName("priority") public String priority;
    @Nullable @SerializedName("isActive") public Boolean isActive;
    @Nullable @SerializedName("order") public Integer order;
    @Nullable @SerializedName("triggerOffsetDays") public Integer triggerOffsetDays;
    @Nullable @SerializedName("recurrenceType") public String recurrenceType;
    @Nullable @SerializedName("recurrenceValue") public Integer recurrenceValue;
    @Nullable @SerializedName("specificDate") public String specificDate;
    @Nullable @SerializedName("relatedEntityType") public String relatedEntityType;
    @Nullable @SerializedName("relatedEntityId") public String relatedEntityId;

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    public boolean isCustomReminder() {
        return TYPE_CUSTOM_REMINDER.equals(type);
    }
}
