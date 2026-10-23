package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para representar uma categoria de transação
 */
public class CategoryDto {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("type")
    private String type; // INCOME ou EXPENSE
    
    @SerializedName("color")
    private String color;
    
    @SerializedName("parentId")
    private String parentId;

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    @Override
    public String toString() {
        return name; // Necessário para o ArrayAdapter do AutoCompleteTextView
    }
}
