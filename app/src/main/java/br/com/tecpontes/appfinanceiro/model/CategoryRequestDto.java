package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Corpo de criação/edição de categoria (/api/categories). */
public class CategoryRequestDto {

    @SerializedName("name") public String name;
    @SerializedName("type") public String type;
    @SerializedName("color") public String color;
    @Nullable @SerializedName("parentId") public String parentId;

    public CategoryRequestDto() {}

    public CategoryRequestDto(
            String name, String type, String color, @Nullable String parentId) {
        this.name = name;
        this.type = type;
        this.color = color;
        this.parentId = parentId;
    }
}
