package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Requisição de autenticação com Google: o ID token é validado pelo backend,
 * que devolve o JWT da aplicação.
 */
public class GoogleAuthRequestDto {

    @NonNull
    @SerializedName("idToken")
    private String idToken;

    @Nullable
    @SerializedName("remember")
    private Boolean remember;

    public GoogleAuthRequestDto(@NonNull String idToken, boolean remember) {
        this.idToken = idToken;
        this.remember = remember;
    }

    @NonNull
    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(@NonNull String idToken) {
        this.idToken = idToken;
    }

    @Nullable
    public Boolean getRemember() {
        return remember;
    }

    public void setRemember(@Nullable Boolean remember) {
        this.remember = remember;
    }

    @Override
    public String toString() {
        return "GoogleAuthRequestDto{" +
                "idToken='[PROTECTED]'" +
                ", remember=" + remember +
                '}';
    }
}
