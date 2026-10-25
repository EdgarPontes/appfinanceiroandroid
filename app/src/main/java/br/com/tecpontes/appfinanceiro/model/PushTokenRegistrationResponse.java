package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class PushTokenRegistrationResponse {
    @Nullable @SerializedName("id") public String id;
    @Nullable @SerializedName("bindingId") public String bindingId;
}
