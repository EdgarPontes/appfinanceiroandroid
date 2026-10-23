package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

public class CategoryAmountDto {
    @SerializedName("categoryId")
    private String categoryId;
    
    @SerializedName("category")
    private String category;
    
    @SerializedName("amount")
    private double amount;
    
    @SerializedName("color")
    private String color;

    public String getCategoryId() { return categoryId; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getColor() { return color; }
}
