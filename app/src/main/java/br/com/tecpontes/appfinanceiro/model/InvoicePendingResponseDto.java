package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resposta de POST /api/invoices/pending. */
public class InvoicePendingResponseDto {
    @Nullable @SerializedName("pending") public Map<String, InvoicePendingDto> pending;

    public Map<String, InvoicePendingDto> getPending() {
        return pending == null ? new LinkedHashMap<>() : pending;
    }
}
