package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Linha normalizada de um extrato CSV ou OFX para a API de conciliação. */
public class StatementEntryDto {
    @SerializedName("fitId") public String fitId;
    @SerializedName("type") public String type;
    @SerializedName("date") public String date;
    @SerializedName("amount") public double amount;
    @SerializedName("description") public String description;

    public StatementEntryDto(String fitId, String type, String date, double amount, String description) {
        this.fitId = fitId;
        this.type = type;
        this.date = date;
        this.amount = amount;
        this.description = description;
    }
}
