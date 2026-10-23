package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Resumo global de conciliação exibido no topo da tela de contas. */
public class ReconciliationSummaryDto {
    @Nullable @SerializedName("total") public Integer total;
    @Nullable @SerializedName("conciliado") public Integer reconciled;
    @Nullable @SerializedName("pendente") public Integer pending;
    @Nullable @SerializedName("percentual") public Integer percent;
    @Nullable @SerializedName("totalPendingAmount") public String totalPendingAmount;
}
