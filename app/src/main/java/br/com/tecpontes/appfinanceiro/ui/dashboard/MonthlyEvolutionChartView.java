package br.com.tecpontes.appfinanceiro.ui.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Gráfico mensal agrupado de receitas e despesas. */
public class MonthlyEvolutionChartView extends View {

    public static class Entry {
        public final String period;
        public final double income;
        public final double expenses;

        public Entry(String period, double income, double expenses) {
            this.period = period;
            this.income = Math.max(0, income);
            this.expenses = Math.max(0, expenses);
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Entry> entries = new ArrayList<>();
    private final int incomeColor = Color.rgb(28, 153, 111);
    private final int expenseColor = Color.rgb(213, 72, 94);
    private final int gridColor = Color.rgb(225, 229, 235);
    private final int labelColor = Color.rgb(100, 110, 125);

    public MonthlyEvolutionChartView(Context context) {
        super(context);
    }

    public MonthlyEvolutionChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MonthlyEvolutionChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setEntries(@Nullable List<Entry> values) {
        entries.clear();
        if (values != null) entries.addAll(values);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float left = dp(8, density);
        float right = getWidth() - dp(8, density);
        float top = dp(10, density);
        float bottom = getHeight() - dp(34, density);
        float chartHeight = Math.max(1, bottom - top);

        paint.setStrokeWidth(dp(1, density));
        paint.setColor(gridColor);
        for (int i = 0; i <= 3; i++) {
            float y = top + chartHeight * i / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }

        if (entries.isEmpty()) return;

        double max = 0;
        for (Entry entry : entries) max = Math.max(max, Math.max(entry.income, entry.expenses));
        if (max <= 0) max = 1;

        float groupWidth = (right - left) / entries.size();
        float gap = dp(3, density);
        float barWidth = Math.min(dp(16, density), Math.max(dp(3, density), (groupWidth - gap * 3) / 2f));
        paint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            float center = left + groupWidth * (i + 0.5f);
            float incomeHeight = (float) (entry.income / max * chartHeight);
            float expensesHeight = (float) (entry.expenses / max * chartHeight);

            paint.setColor(incomeColor);
            canvas.drawRect(center - barWidth - gap / 2f, bottom - incomeHeight,
                    center - gap / 2f, bottom, paint);
            paint.setColor(expenseColor);
            canvas.drawRect(center + gap / 2f, bottom - expensesHeight,
                    center + barWidth + gap / 2f, bottom, paint);

            paint.setColor(labelColor);
            paint.setTextSize(dp(entries.size() > 8 ? 9 : 10, density));
            paint.setTypeface(Typeface.DEFAULT);
            String label = monthLabel(entry.period);
            float textWidth = paint.measureText(label);
            canvas.drawText(label, center - textWidth / 2f, getHeight() - dp(10, density), paint);
        }
    }

    private String monthLabel(String value) {
        if (value == null || value.length() < 7) return value == null ? "" : value;
        try {
            int month = Integer.parseInt(value.substring(5, 7));
            String[] months = DateFormatSymbols.getInstance(new Locale("pt", "BR")).getShortMonths();
            if (month >= 1 && month <= 12) {
                String label = months[month - 1].replace(".", "");
                return label.length() > 3 ? label.substring(0, 3) : label;
            }
        } catch (NumberFormatException ignored) {
            // Usa o texto original para granularidades que não sejam mensais.
        }
        return value;
    }

    private float dp(float value, float density) {
        return value * density;
    }
}
