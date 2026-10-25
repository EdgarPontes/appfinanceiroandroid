package br.com.tecpontes.appfinanceiro.model;

import com.google.gson.annotations.SerializedName;

public class PushTokenRegistrationRequest {
    @SerializedName("token") public String token;
    @SerializedName("deviceId") public String deviceId;
    @SerializedName("platform") public String platform = "android";
    @SerializedName("appVersion") public String appVersion;
}
