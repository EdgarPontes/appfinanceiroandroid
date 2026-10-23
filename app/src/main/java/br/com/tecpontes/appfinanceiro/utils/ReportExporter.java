package br.com.tecpontes.appfinanceiro.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Geração dos arquivos CSV e PDF do relatório financeiro. */
public final class ReportExporter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final float MARGIN = 36f;
    private static final float BOTTOM_LIMIT = PAGE_HEIGHT - 36f;

    private ReportExporter() {}

    public static class Aggregate {
        public final String name;
        public final double amount;
        public final String color;

        public Aggregate(String name, double amount, String color) {
            this.name = name;
            this.amount = amount;
            this.color = color;
        }
    }

    public static class Summary {
        public final double income;
        public final double expenses;
        public final double balance;
        public final int count;

        public Summary(double income, double expenses, int count) {
            this.income = income;
            this.expenses = expenses;
            this.balance = income - expenses;
            this.count = count;
        }
    }

    // ========== CSV ==========

    public static void writeCsv(
            @NonNull OutputStream output,
            @Nullable List<TransactionListItemDto> transactions,
            @NonNull Summary summary,
            @NonNull LocalDate from,
            @NonNull LocalDate to) throws IOException {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Data;Descrição;Categoria;Tipo;Valor\n");

        for (TransactionListItemDto transaction : sortedByDate(transactions)) {
            csv.append(quote(date(transaction))).append(';')
                    .append(quote(safe(transaction.description))).append(';')
                    .append(quote(safe(transaction.getCategoryName()))).append(';')
                    .append(quote(typeLabel(transaction.type))).append(';')
                    .append(quote(plainAmount(transaction.getAmountValue()))).append('\n');
        }

        csv.append('\n');
        csv.append("Período;").append(quote(from.format(DATE_FORMAT)))
                .append(';').append(quote(to.format(DATE_FORMAT))).append(";;\n");
        csv.append("Total de Receitas;;;;").append(plainAmount(summary.income)).append('\n');
        csv.append("Total de Despesas;;;;").append(plainAmount(summary.expenses)).append('\n');
        csv.append("Saldo do Período;;;;").append(plainAmount(summary.balance)).append('\n');
        csv.append("Número de Transações;;;;").append(summary.count).append('\n');

        output.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    // ========== PDF ==========

    public static void writePdf(
            @NonNull OutputStream output,
            @Nullable List<TransactionListItemDto> transactions,
            @Nullable List<Aggregate> expensesByCategory,
            @Nullable List<Aggregate> incomeByCategory,
            @NonNull Summary summary,
            @NonNull LocalDate from,
            @NonNull LocalDate to) throws IOException {
        PdfDocument document = new PdfDocument();
        PageWriter writer = new PageWriter(document);

        Paint titlePaint = paint(16f, Color.BLACK, true);
        Paint sectionPaint = paint(12f, Color.BLACK, true);
        Paint bodyPaint = paint(9f, Color.DKGRAY, false);
        Paint smallPaint = paint(8f, Color.GRAY, false);
        Paint incomePaint = paint(9f, Color.rgb(24, 153, 111), false);
        Paint expensePaint = paint(9f, Color.rgb(213, 72, 94), false);
        Paint amountRight = rightPaint(9f, Color.BLACK, false);

        try {
            writer.ensure(24);
            writer.canvas.drawText("Relatório Financeiro", MARGIN, writer.y, titlePaint);
            writer.y += 22;

            writer.line("Período: " + from.format(DATE_FORMAT) + " a "
                    + to.format(DATE_FORMAT), bodyPaint);
            writer.line("Total de Receitas: " + currency(summary.income), incomePaint);
            writer.line("Total de Despesas: " + currency(summary.expenses), expensePaint);
            writer.line("Saldo do Período: " + currency(summary.balance),
                    paint(9f, summary.balance >= 0 ? Color.rgb(24, 153, 111)
                            : Color.rgb(213, 72, 94), false));
            writer.line("Número de Transações: " + summary.count, bodyPaint);
            writer.y += 8;

            writer.section("Despesas por Categoria", sectionPaint);
            if (expensesByCategory == null || expensesByCategory.isEmpty()) {
                writer.line("Nenhuma despesa no período.", smallPaint);
            } else {
                for (Aggregate item : expensesByCategory) {
                    writer.categoryRow(item.name, currency(item.amount), expensePaint);
                }
            }
            writer.y += 8;

            writer.section("Receitas por Categoria", sectionPaint);
            if (incomeByCategory == null || incomeByCategory.isEmpty()) {
                writer.line("Nenhuma receita no período.", smallPaint);
            } else {
                for (Aggregate item : incomeByCategory) {
                    writer.categoryRow(item.name, currency(item.amount), incomePaint);
                }
            }
            writer.y += 8;

            writer.section("Lançamentos", sectionPaint);
            writer.transactionHeader(paint(8f, Color.BLACK, true));
            for (TransactionListItemDto transaction : sortedByDate(transactions)) {
                writer.transactionRow(
                        date(transaction),
                        safe(transaction.description),
                        safe(transaction.getCategoryName()),
                        currency(transaction.getAmountValue()),
                        bodyPaint,
                        smallPaint,
                        amountRight);
            }

            writer.finish();
            document.writeTo(output);
            output.flush();
        } finally {
            try {
                writer.finish();
            } catch (Exception ignored) {
                // Garante o fechamento da página mesmo em caso de erro.
            }
            document.close();
        }
    }

    private static final class PageWriter {
        private final PdfDocument document;
        private PdfDocument.Page page;
        private Canvas canvas;
        private float y;
        private int pageNumber;
        private boolean finished;

        PageWriter(PdfDocument document) {
            this.document = document;
            newPage();
        }

        private void newPage() {
            if (page != null) document.finishPage(page);
            pageNumber++;
            page = document.startPage(new PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
            canvas = page.getCanvas();
            y = MARGIN;
        }

        void ensure(float height) {
            if (y + height > BOTTOM_LIMIT) newPage();
        }

        void line(String text, Paint paint) {
            ensure(14);
            canvas.drawText(text, MARGIN, y, paint);
            y += 14;
        }

        void section(String title, Paint paint) {
            ensure(20);
            canvas.drawText(title, MARGIN, y, paint);
            y += 16;
        }

        void categoryRow(String name, String amount, Paint amountPaint) {
            ensure(14);
            Paint namePaint = paint(9f, Color.DKGRAY, false);
            canvas.drawText(fit(name, namePaint, 380f), MARGIN, y, namePaint);
            Paint right = rightPaint(9f, amountPaint.getColor(), false);
            canvas.drawText(amount, PAGE_WIDTH - MARGIN, y, right);
            y += 14;
        }

        void transactionHeader(Paint paint) {
            ensure(16);
            canvas.drawText("Data", MARGIN, y, paint);
            canvas.drawText("Descrição", 100f, y, paint);
            canvas.drawText("Categoria", 300f, y, paint);
            Paint right = rightPaint(8f, paint.getColor(), true);
            canvas.drawText("Valor", PAGE_WIDTH - MARGIN, y, right);
            y += 12;
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y,
                    paint(0.5f, Color.LTGRAY, false));
            y += 6;
        }

        void transactionRow(
                String date,
                String description,
                String category,
                String amount,
                Paint bodyPaint,
                Paint smallPaint,
                Paint amountPaint) {
            ensure(14);
            canvas.drawText(fit(date, bodyPaint, 60f), MARGIN, y, bodyPaint);
            canvas.drawText(fit(description, bodyPaint, 190f), 100f, y, bodyPaint);
            canvas.drawText(fit(category, smallPaint, 180f), 300f, y, smallPaint);
            canvas.drawText(amount, PAGE_WIDTH - MARGIN, y, amountPaint);
            y += 14;
        }

        void finish() {
            if (finished) return;
            if (page != null) {
                document.finishPage(page);
                page = null;
            }
            finished = true;
        }
    }

    // ========== HELPERS ==========

    @NonNull
    private static List<TransactionListItemDto> sortedByDate(
            @Nullable List<TransactionListItemDto> transactions) {
        List<TransactionListItemDto> sorted = new ArrayList<>();
        if (transactions != null) sorted.addAll(transactions);
        sorted.sort(Comparator.comparingLong(TransactionListItemDto::getDateMillis));
        return sorted;
    }

    @NonNull
    private static String date(@NonNull TransactionListItemDto transaction) {
        long millis = transaction.getDateMillis();
        if (millis <= 0) return "";
        return Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDate().format(DATE_FORMAT);
    }

    @NonNull
    private static String typeLabel(@Nullable String type) {
        if ("INCOME".equalsIgnoreCase(type)) return "Receita";
        if ("EXPENSE".equalsIgnoreCase(type)) return "Despesa";
        return safe(type);
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @NonNull
    private static String quote(@NonNull String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @NonNull
    private static String plainAmount(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    @NonNull
    private static String currency(double value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    @NonNull
    private static Paint paint(float size, int color, boolean bold) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTypeface(bold
                ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                : Typeface.DEFAULT);
        return paint;
    }

    @NonNull
    private static Paint rightPaint(float size, int color, boolean bold) {
        Paint paint = paint(size, color, bold);
        paint.setTextAlign(Paint.Align.RIGHT);
        return paint;
    }

    @NonNull
    private static String fit(@NonNull String text, @NonNull Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) return text;
        int end = text.length();
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--;
        }
        return text.substring(0, end) + "…";
    }
}
