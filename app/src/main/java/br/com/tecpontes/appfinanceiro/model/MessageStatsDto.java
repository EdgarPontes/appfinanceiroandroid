package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Estatísticas agregadas devolvidas junto da listagem de notificações. */
public class MessageStatsDto {

    @Nullable @SerializedName("totalSent") public Integer totalSent;
    @Nullable @SerializedName("failures") public Integer failures;
    @Nullable @SerializedName("successRate") public Double successRate;
}
