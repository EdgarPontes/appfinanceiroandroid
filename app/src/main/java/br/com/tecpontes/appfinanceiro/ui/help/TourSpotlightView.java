package br.com.tecpontes.appfinanceiro.ui.help;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import br.com.tecpontes.appfinanceiro.R;

/**
 * Camada escurecida do guia com um recorte arredondado ao redor do elemento em destaque.
 */
public class TourSpotlightView extends View {

    private final Paint scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outsidePath = new Path();
    private final Path highlightPath = new Path();
    private final RectF highlightRect = new RectF();
    private float cornerRadius;
    private boolean hasHighlight;

    public TourSpotlightView(@NonNull Context context) {
        super(context);
        init();
    }

    public TourSpotlightView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TourSpotlightView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        cornerRadius = dp(16);

        scrimPaint.setStyle(Paint.Style.FILL);
        scrimPaint.setColor(ContextCompat.getColor(getContext(), R.color.help_scrim));

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2));
        borderPaint.setColor(ContextCompat.getColor(getContext(), R.color.help_highlight));
    }

    public void setHighlight(@Nullable RectF rect) {
        hasHighlight = rect != null && !rect.isEmpty();
        if (hasHighlight) {
            highlightRect.set(rect);
        } else {
            highlightRect.setEmpty();
        }
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (!hasHighlight) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);
            return;
        }

        outsidePath.reset();
        outsidePath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        highlightPath.reset();
        highlightPath.addRoundRect(
                highlightRect, cornerRadius, cornerRadius, Path.Direction.CW);
        outsidePath.op(highlightPath, Path.Op.DIFFERENCE);
        canvas.drawPath(outsidePath, scrimPaint);
        canvas.drawRoundRect(highlightRect, cornerRadius, cornerRadius, borderPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
