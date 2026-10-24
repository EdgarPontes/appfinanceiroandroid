package br.com.tecpontes.appfinanceiro.ui.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import br.com.tecpontes.appfinanceiro.R;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de pizza leve, desenhado nativamente para evitar dependência externa de charts. */
public class CategoryPieChartView extends View {

    public static class Entry {
        public final String label;
        public final double value;
        public final int color;

        public Entry(String label, double value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Entry> entries = new ArrayList<>();

    public CategoryPieChartView(Context context) {
        super(context);
        init();
    }

    public CategoryPieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryPieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setEntries(@Nullable List<Entry> values) {
        entries.clear();
        if (values != null) entries.addAll(values);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        double total = 0;
        for (Entry entry : entries) total += Math.max(0, entry.value);

        if (total <= 0 || entries.isEmpty()) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.outline));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(18));
            float radius = Math.min(getWidth(), getHeight()) * 0.34f;
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            return;
        }

        float diameter = Math.min(getWidth(), getHeight()) - dp(24);
        float left = (getWidth() - diameter) / 2f;
        float top = (getHeight() - diameter) / 2f;
        RectF bounds = new RectF(left, top, left + diameter, top + diameter);
        float startAngle = -90f;

        paint.setStyle(Paint.Style.FILL);
        for (Entry entry : entries) {
            if (entry.value <= 0) continue;
            float sweep = (float) (entry.value / total * 360f);
            paint.setColor(entry.color);
            canvas.drawArc(bounds, startAngle, sweep, true, paint);
            startAngle += sweep;
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
