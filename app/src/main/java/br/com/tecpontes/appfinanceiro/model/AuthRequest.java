package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
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

    public AuthRequest(@NonNull String email, @NonNull String password) {
        this.email = email;
        this.password = password;
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

    @Override
    public String toString() {
        return "AuthRequest{" +
                "email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}
