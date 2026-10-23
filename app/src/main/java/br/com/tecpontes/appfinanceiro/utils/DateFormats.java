package br.com.tecpontes.appfinanceiro.utils;

import androidx.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação das datas ISO devolvidas pela API. */
public final class DateFormats {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("pt", "BR"))
                    .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm", new Locale("pt", "BR"))
                    .withZone(ZoneId.systemDefault());

    private DateFormats() {}

    @Nullable
    public static String formatDateTime(@Nullable String iso) {
        Instant instant = parse(iso);
        return instant == null ? iso : DATE_TIME.format(instant);
    }

    @Nullable
    public static String formatTime(@Nullable String iso) {
        Instant instant = parse(iso);
        return instant == null ? iso : TIME.format(instant);
    }

    @Nullable
    private static Instant parse(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
