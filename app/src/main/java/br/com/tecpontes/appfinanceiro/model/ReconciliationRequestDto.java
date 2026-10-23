package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Corpo aceito por PUT /api/transactions/{id}/reconcile. */
public class ReconciliationRequestDto {
    @SerializedName("conciliado") public boolean reconciled;

    public ReconciliationRequestDto(boolean reconciled) {
        this.reconciled = reconciled;
    }
}
