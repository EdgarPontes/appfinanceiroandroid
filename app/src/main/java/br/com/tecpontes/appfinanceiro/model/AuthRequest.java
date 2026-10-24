package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Modelo para requisição de autenticação
 */
public class AuthRequest {

    @NonNull
    @SerializedName("email")
    private String email;

    @NonNull
    @SerializedName("password")
    private String password;

    @Nullable
    @SerializedName("firstName")
    private String firstName;

    @Nullable
    @SerializedName("cellphone")
    private String cellphone;

    public AuthRequest(@NonNull String email, @NonNull String password) {
        this(email, password, null, null);
    }

    public AuthRequest(@NonNull String email, @NonNull String password,
                       @Nullable String firstName, @Nullable String cellphone) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.cellphone = cellphone;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    public void setPassword(@NonNull String password) {
        this.password = password;
    }

    @Nullable
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(@Nullable String firstName) {
        this.firstName = firstName;
    }

    @Nullable
    public String getCellphone() {
        return cellphone;
    }

    public void setCellphone(@Nullable String cellphone) {
        this.cellphone = cellphone;
    }

    @Override
    public String toString() {
        return "AuthRequest{" +
                "email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                ", firstName='" + firstName + '\'' +
                ", cellphone='" + cellphone + '\'' +
                '}';
    }
}
