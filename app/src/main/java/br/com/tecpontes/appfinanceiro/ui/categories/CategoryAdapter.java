package br.com.tecpontes.appfinanceiro.ui.categories;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import java.util.ArrayList;
import java.util.List;

/** Lista de categorias com suas subcategorias indentadas. */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface Listener {
        void onEdit(@NonNull CategoryDto category);
        void onDelete(@NonNull CategoryDto category);
    }

    private final Listener listener;
    private final List<Row> rows = new ArrayList<>();

    public CategoryAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setData(@Nullable List<CategoryDto> categories) {
        rows.clear();
        if (categories != null) {
            for (CategoryDto parent : categories) {
                rows.add(new Row(parent, false));
                for (CategoryDto child : parent.getSubCategories()) {
                    rows.add(new Row(child, true));
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static class Row {
        final CategoryDto category;
        final boolean subcategory;

        Row(CategoryDto category, boolean subcategory) {
            this.category = category;
            this.subcategory = subcategory;
        }
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final View colorView;
        private final TextView name;
        private final ImageButton edit;
        private final ImageButton delete;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.categoryColorView);
            name = itemView.findViewById(R.id.categoryNameTextView);
            edit = itemView.findViewById(R.id.editCategoryButton);
            delete = itemView.findViewById(R.id.deleteCategoryButton);
        }

        void bind(@NonNull Row row) {
            CategoryDto category = row.category;

            name.setText(category.getName() == null ? "" : category.getName());
            name.setTextSize(row.subcategory ? 13f : 14f);

            itemView.setPaddingRelative(
                    dp(row.subcategory ? 30 : 12),
                    dp(4),
                    dp(4),
                    dp(4));
            itemView.setAlpha(row.subcategory ? 0.92f : 1f);

            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(parseColor(category.getColor()));
            colorView.setBackground(background);

            int size = dp(row.subcategory ? 10 : 12);
            ViewGroup.LayoutParams params = colorView.getLayoutParams();
            params.width = size;
            params.height = size;
            colorView.setLayoutParams(params);

            itemView.setOnClickListener(v -> listener.onEdit(category));
            edit.setOnClickListener(v -> listener.onEdit(category));
            delete.setOnClickListener(v -> listener.onDelete(category));
        }

        private int parseColor(@Nullable String color) {
            int fallback = itemView.getContext().getColor(R.color.money_neutral);
            try {
                return color == null ? fallback : Color.parseColor(color);
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }
    }
}
