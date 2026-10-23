package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/** Corpo do POST /api/category-goals/import. */
public class ImportCategoryGoalsRequestDto {

    @SerializedName("sourceMonth") public int sourceMonth;
    @SerializedName("sourceYear") public int sourceYear;
    @SerializedName("targetMonth") public int targetMonth;
    @SerializedName("targetYear") public int targetYear;

    public ImportCategoryGoalsRequestDto() {}

    public ImportCategoryGoalsRequestDto(
            int sourceMonth, int sourceYear, int targetMonth, int targetYear) {
        this.sourceMonth = sourceMonth;
        this.sourceYear = sourceYear;
        this.targetMonth = targetMonth;
        this.targetYear = targetYear;
    }
}
