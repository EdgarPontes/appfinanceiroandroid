package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Corpo do POST /api/category-goals. */
public class CategoryGoalRequestDto {

    @SerializedName("categoryId") public String categoryId;
    @SerializedName("referenceMonth") public int referenceMonth;
    @SerializedName("referenceYear") public int referenceYear;
    @SerializedName("goalAmount") public String goalAmount;

    public CategoryGoalRequestDto() {}

    public CategoryGoalRequestDto(
            String categoryId, int referenceMonth, int referenceYear, String goalAmount) {
        this.categoryId = categoryId;
        this.referenceMonth = referenceMonth;
        this.referenceYear = referenceYear;
        this.goalAmount = goalAmount;
    }
}
