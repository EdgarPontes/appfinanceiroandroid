package br.com.tecpontes.appfinanceiro.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista de despesas detalhadas do relatório. */
public class ReportExpenseAdapter
        extends RecyclerView.Adapter<ReportExpenseAdapter.ExpenseViewHolder> {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<TransactionListItemDto> items = new ArrayList<>();
    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public void setData(@Nullable List<TransactionListItemDto> transactions) {
        items.clear();
        if (transactions != null) items.addAll(transactions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {

        private final TextView date;
        private final TextView description;
        private final TextView category;
        private final TextView amount;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.reportExpenseDateTextView);
            description = itemView.findViewById(R.id.reportExpenseDescriptionTextView);
            category = itemView.findViewById(R.id.reportExpenseCategoryTextView);
            amount = itemView.findViewById(R.id.reportExpenseAmountTextView);
        }

        void bind(@NonNull TransactionListItemDto transaction) {
            long millis = transaction.getDateMillis();
            date.setText(millis <= 0 ? ""
                    : Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DATE_FORMAT));

            description.setText(transaction.description == null
                    || transaction.description.trim().isEmpty()
                    ? itemView.getContext().getString(R.string.reports_no_description)
                    : transaction.description);

            category.setText(transaction.getCategoryName());
            amount.setText(currency.format(transaction.getAmountValue()));
        }
    }
}
