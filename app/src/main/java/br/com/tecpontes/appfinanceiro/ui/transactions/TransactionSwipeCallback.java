package br.com.tecpontes.appfinanceiro.ui.transactions;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;

/** Arrastar para a direita concilia (ou desfaz); arrastar para a esquerda clona. */
public class TransactionSwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface Listener {
        boolean canReconcile(@NonNull TransactionListItemDto item);
        boolean canClone(@NonNull TransactionListItemDto item);
        void onReconcile(@NonNull TransactionListItemDto item);
        void onClone(@NonNull TransactionListItemDto item);
    }

    private final TransactionsAdapter adapter;
    private final Listener listener;
    private final Paint backgroundPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float padding;

    private TransactionSwipeCallback(@NonNull RecyclerView recyclerView,
                                     @NonNull TransactionsAdapter adapter,
                                     @NonNull Listener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.listener = listener;
        float density = recyclerView.getResources().getDisplayMetrics().density;
        this.padding = 20 * density;
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(14 * density);
        textPaint.setFakeBoldText(true);
    }

    public static void attach(@NonNull RecyclerView recyclerView,
                              @NonNull TransactionsAdapter adapter,
                              @NonNull Listener listener) {
        new ItemTouchHelper(new TransactionSwipeCallback(recyclerView, adapter, listener))
                .attachToRecyclerView(recyclerView);
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        TransactionListItemDto item = adapter.getItem(viewHolder.getAdapterPosition());
        if (item == null) return 0;
        int directions = 0;
        if (listener.canReconcile(item)) directions |= ItemTouchHelper.RIGHT;
        if (listener.canClone(item)) directions |= ItemTouchHelper.LEFT;
        return directions;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        TransactionListItemDto item = adapter.getItem(position);
        adapter.notifyItemChanged(position);
        if (item == null) return;
        if (direction == ItemTouchHelper.RIGHT) {
            listener.onReconcile(item);
        } else {
            listener.onClone(item);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0f) {
            View itemView = viewHolder.itemView;
            TransactionListItemDto item = adapter.getItem(viewHolder.getAdapterPosition());
            boolean reconcile = dX > 0;
            float left = reconcile ? itemView.getLeft() : itemView.getRight() + dX;
            float right = reconcile ? itemView.getLeft() + dX : itemView.getRight();

            backgroundPaint.setColor(ContextCompat.getColor(recyclerView.getContext(),
                    reconcile ? R.color.money_positive : R.color.brand_primary));
            canvas.drawRect(left, itemView.getTop(), right, itemView.getBottom(), backgroundPaint);

            String label = reconcile
                    ? (item != null && item.isReconciled() ? "Desfazer conciliação" : "Conciliar")
                    : "Clonar";
            float textWidth = textPaint.measureText(label);
            float x = reconcile ? left + padding : right - padding - textWidth;
            float y = itemView.getTop() + itemView.getHeight() / 2f
                    - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText(label, x, y, textPaint);
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
