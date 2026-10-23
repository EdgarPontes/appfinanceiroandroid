package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class ReconciliationConfirmResultDto {
    @Nullable @SerializedName("reconciled") public Integer reconciled;
    @Nullable @SerializedName("created") public Integer created;
}
