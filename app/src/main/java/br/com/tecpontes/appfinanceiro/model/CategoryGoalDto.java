package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Meta mensal de uma categoria (/api/category-goals). */
public class CategoryGoalDto {

    @Nullable @SerializedName("id") public String id;
    @Nullable @SerializedName("categoryId") public String categoryId;
    @Nullable @SerializedName("referenceMonth") public Integer referenceMonth;
    @Nullable @SerializedName("referenceYear") public Integer referenceYear;
    @Nullable @SerializedName("goalAmount") public String goalAmount;
}
