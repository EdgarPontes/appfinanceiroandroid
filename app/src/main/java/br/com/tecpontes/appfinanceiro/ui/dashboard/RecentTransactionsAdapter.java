package br.com.tecpontes.appfinanceiro.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para RecyclerView de transações recentes no dashboard.
 * Consome o mesmo payload de GET /api/transactions usado pelo app web.
 */
public class RecentTransactionsAdapter extends RecyclerView.Adapter<RecentTransactionsAdapter.ViewHolder> {

    private List<TransactionListItemDto> transactions = new ArrayList<>();

    public RecentTransactionsAdapter(List<TransactionListItemDto> transactions) {
        if (transactions != null) {
            this.transactions = transactions;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionListItemDto transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    /**
     * Atualiza lista de transações
     */
    public void updateTransactions(List<TransactionListItemDto> newTransactions) {
        this.transactions = newTransactions != null ? newTransactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder para itens de transação
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final NumberFormat currencyFormat =
                NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

        private final TextView categoryTextView;
        private final TextView amountTextView;
        private final TextView dateTextView;
        private final TextView noteTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            noteTextView = itemView.findViewById(R.id.noteTextView);
        }

        public void bind(TransactionListItemDto transaction) {
            if (transaction == null) return;

            // Descrição principal (equivalente ao "description" usado no app web)
            String description = transaction.description;
            if (description == null || description.trim().isEmpty()) {
                description = transaction.getCategoryName();
            }
            categoryTextView.setText(description);

            // Linha secundária: categoria • conta/cartão
            noteTextView.setText(transaction.getCategoryName() + " · " + transaction.getSourceName());
            noteTextView.setVisibility(View.VISIBLE);

            // Valor formatado (receita verde, despesa vermelha)
            double amount = transaction.getAmountValue();
            if (transaction.isIncome()) {
                amountTextView.setTextColor(itemView.getContext().getColor(R.color.money_positive));
                amountTextView.setText("+" + currencyFormat.format(Math.abs(amount)));
            } else {
                amountTextView.setTextColor(itemView.getContext().getColor(R.color.money_negative));
                amountTextView.setText("-" + currencyFormat.format(Math.abs(amount)));
            }

            // Data formatada
            long dateMillis = transaction.getDateMillis();
            dateTextView.setText(dateMillis > 0
                    ? dateFormat.format(new Date(dateMillis))
                    : "-");
        }
    }
}
