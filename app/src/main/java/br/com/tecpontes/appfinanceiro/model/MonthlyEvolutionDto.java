package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

public class MonthlyEvolutionDto {
    @SerializedName("month")
    private String month;
    
    @SerializedName("income")
    private double income;
    
    @SerializedName("expenses")
    private double expenses;

    public String getMonth() { return month; }
    public double getIncome() { return income; }
    public double getExpenses() { return expenses; }
}
