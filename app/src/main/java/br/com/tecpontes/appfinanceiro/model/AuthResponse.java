package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Modelo para resposta de autenticação
 */
public class AuthResponse {

    @NonNull
    @SerializedName("token")
    private String token;

    @NonNull
    @SerializedName("user")
    private UserDto user;

    public AuthResponse(@NonNull String token, @NonNull UserDto user) {
        this.token = token;
        this.user = user;
    }

    @NonNull
    public String getToken() {
        return token;
    }

    public void setToken(@NonNull String token) {
        this.token = token;
    }

    @NonNull
    public UserDto getUser() {
        return user;
    }

    public void setUser(@NonNull UserDto user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "token='[PROTECTED]'" +
                ", user=" + user +
                '}';
    }

    /**
     * Modelo interno para dados do usuário
     */
    public static class UserDto {

        @NonNull
        @SerializedName("id")
        private String id;

        @NonNull
        @SerializedName("email")
        private String email;

        @Nullable
        @SerializedName("name")
        private String name;

        public UserDto(@NonNull String id, @NonNull String email, @Nullable String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        public void setId(@NonNull String id) {
            this.id = id;
        }

        @NonNull
        public String getEmail() {
            return email;
        }

        public void setEmail(@NonNull String email) {
            this.email = email;
        }

        @Nullable
        public String getName() {
            return name;
        }

        public void setName(@Nullable String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "UserDto{" +
                    "id='" + id + '\'' +
                    ", email='" + email + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
