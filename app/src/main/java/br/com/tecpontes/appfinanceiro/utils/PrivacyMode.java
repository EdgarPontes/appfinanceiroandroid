package br.com.tecpontes.appfinanceiro.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import java.text.NumberFormat;
import java.util.Locale;

/** Preferência global para ocultar saldos e resumos financeiros na interface. */
public final class PrivacyMode {

    private static final String PREF_NAME = "app_financeiro_privacy";
    private static final String KEY_MASK_VALUES = "mask_values";
    private static final String MASKED_VALUE = "R$ ****";

    private PrivacyMode() {}

    public static boolean isMasked(@NonNull Context context) {
        return preferences(context).getBoolean(KEY_MASK_VALUES, false);
    }

    public static void setMasked(@NonNull Context context, boolean masked) {
        preferences(context).edit().putBoolean(KEY_MASK_VALUES, masked).apply();
    }

    public static boolean toggle(@NonNull Context context) {
        boolean masked = !isMasked(context);
        setMasked(context, masked);
        return masked;
    }

    @NonNull
    public static String format(@NonNull Context context, double value) {
        if (isMasked(context)) return MASKED_VALUE;
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    private static SharedPreferences preferences(@NonNull Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
