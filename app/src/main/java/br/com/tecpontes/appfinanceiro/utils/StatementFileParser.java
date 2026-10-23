package br.com.tecpontes.appfinanceiro.utils;

import androidx.annotation.NonNull;
import br.com.tecpontes.appfinanceiro.model.StatementEntryDto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parser simples dos formatos de extrato aceitos pelas telas web de conciliação. */
public final class StatementFileParser {
    private StatementFileParser() { }

    @NonNull public static List<StatementEntryDto> parseOfx(@NonNull String content) {
        List<StatementEntryDto> entries = new ArrayList<>();
        String normalized = content.replace("\r", "");
        String[] blocks = normalized.split("(?i)<STMTTRN>");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            String dateRaw = tag(block, "DTPOSTED");
            String amountRaw = tag(block, "TRNAMT");
            String fitId = tag(block, "FITID");
            String typeRaw = tag(block, "TRNTYPE");
            if (dateRaw == null || amountRaw == null) continue;
            try {
                double value = parseAmount(amountRaw);
                String type = value < 0 || "DEBIT".equalsIgnoreCase(typeRaw) ? "DEBIT" : "CREDIT";
                String date = ofxDate(dateRaw);
                String description = firstNonBlank(tag(block, "MEMO"), tag(block, "NAME"),
                        tag(block, "CHECKNUM"), tag(block, "REFNUM"), "Transação sem descrição");
                if (fitId == null || fitId.trim().isEmpty()) fitId = "ofx-" + i + "-" + date;
                entries.add(new StatementEntryDto(fitId.trim(), type, date, Math.abs(value), description.trim()));
            } catch (RuntimeException ignored) { }
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("O arquivo OFX não contém transações válidas.");
        return entries;
    }

    @NonNull public static List<StatementEntryDto> parseCsv(@NonNull String content) {
        String[] lines = content.replace("\uFEFF", "").split("\\r?\\n");
        int headerLine = -1;
        char delimiter = ',';
        int dateIndex = -1, amountIndex = -1, descriptionIndex = -1, fitIdIndex = -1;
        List<String> header = null;
        for (int i = 0; i < lines.length; i++) {
            char candidate = countOutsideQuotes(lines[i], ';') > countOutsideQuotes(lines[i], ',') ? ';' : ',';
            List<String> fields = splitCsvLine(lines[i], candidate);
            List<String> normalized = new ArrayList<>();
            for (String field : fields) normalized.add(normalizeHeader(field));
            int d = findHeader(normalized, "data", "date", "release_date");
            int a = findHeader(normalized, "valor", "amount", "transaction_net_amount");
            if (d >= 0 && a >= 0) {
                headerLine = i;
                delimiter = candidate;
                header = fields;
                dateIndex = d;
                amountIndex = a;
                descriptionIndex = findHeader(normalized, "desc", "memo", "hist", "text", "transaction_type");
                fitIdIndex = findHeader(normalized, "reference_id", "fitid", "fit_id");
                break;
            }
        }
        if (headerLine < 0 || header == null) {
            throw new IllegalArgumentException("Cabeçalho do CSV precisa ter colunas de Data e Valor.");
        }
        List<StatementEntryDto> entries = new ArrayList<>();
        for (int i = headerLine + 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            List<String> fields = splitCsvLine(lines[i], delimiter);
            if (fields.size() <= Math.max(dateIndex, amountIndex)) continue;
            String rawDate = fields.get(dateIndex).trim();
            String rawAmount = fields.get(amountIndex).trim();
            if (rawDate.isEmpty() || rawAmount.isEmpty()) continue;
            try {
                double value = parseAmount(rawAmount);
                String date = csvDate(rawDate);
                String description = descriptionIndex >= 0 && descriptionIndex < fields.size()
                        ? fields.get(descriptionIndex).trim() : "Transação sem descrição";
                if (description.isEmpty()) description = "Transação sem descrição";
                String fitId = fitIdIndex >= 0 && fitIdIndex < fields.size()
                        ? fields.get(fitIdIndex).trim() : "";
                if (fitId.isEmpty()) fitId = "csv-" + date.substring(0, 10) + "-" + i + "-" + description.hashCode();
                entries.add(new StatementEntryDto(fitId,
                        value < 0 ? "DEBIT" : "CREDIT", date, Math.abs(value), description));
            } catch (RuntimeException ignored) { }
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("O CSV não contém transações válidas.");
        return entries;
    }

    private static String tag(String block, String name) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "(?i)<" + name + ">\\s*([^<\\r\\n]+)").matcher(block);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String ofxDate(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 8) throw new IllegalArgumentException("Data OFX inválida");
        return LocalDate.of(Integer.parseInt(digits.substring(0, 4)),
                Integer.parseInt(digits.substring(4, 6)), Integer.parseInt(digits.substring(6, 8)))
                .atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toString();
    }

    private static String csvDate(String value) {
        String date = value.trim();
        if (date.matches("\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4}.*")) {
            String[] parts = date.split("[/.-]");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2].substring(0, 4));
            return LocalDate.of(year, month, day).atTime(12, 0)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toString();
        }
        if (date.matches("\\d{4}-\\d{1,2}-\\d{1,2}.*")) {
            String[] parts = date.substring(0, 10).split("-");
            return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
                    .atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toString();
        }
        try { return OffsetDateTime.parse(date).toInstant().toString(); }
        catch (DateTimeParseException ignored) { return java.time.Instant.parse(date).toString(); }
    }

    private static double parseAmount(String value) {
        String clean = value.replace("R$", "").replaceAll("\\s", "").replaceAll("[^0-9,.-]", "");
        if (clean.contains(",") && clean.contains(".")) {
            if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) clean = clean.replace(".", "").replace(',', '.');
            else clean = clean.replace(",", "");
        } else if (clean.contains(",")) clean = clean.replace(',', '.');
        double amount = Double.parseDouble(clean);
        if (!Double.isFinite(amount)) throw new NumberFormatException("Valor inválido");
        return amount;
    }

    private static int findHeader(List<String> headers, String... possibilities) {
        for (int i = 0; i < headers.size(); i++) {
            for (String possibility : possibilities) {
                if (headers.get(i).contains(possibility)) return i;
            }
        }
        return -1;
    }

    private static String normalizeHeader(String header) {
        return java.text.Normalizer.normalize(header.replace("\"", "").trim().toLowerCase(Locale.ROOT),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static int countOutsideQuotes(String value, char delimiter) {
        boolean quoted = false;
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < value.length() && value.charAt(i + 1) == '"') i++;
                else quoted = !quoted;
            } else if (!quoted && c == delimiter) count++;
        }
        return count;
    }

    private static List<String> splitCsvLine(String line, char delimiter) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else quoted = !quoted;
            } else if (c == delimiter && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else field.append(c);
        }
        fields.add(field.toString());
        return fields;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value;
        return "";
    }
}
