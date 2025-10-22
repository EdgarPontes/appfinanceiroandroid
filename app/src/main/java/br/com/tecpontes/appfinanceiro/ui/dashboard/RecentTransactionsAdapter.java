package br.com.tecpontes.appfinanceiro.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.data.local.entity.Transaction;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para RecyclerView de transações recentes no dashboard
 */
public class RecentTransactionsAdapter extends RecyclerView.Adapter<RecentTransactionsAdapter.ViewHolder> {

    private List<Transaction> transactions;

    public RecentTransactionsAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
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
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    /**
     * Atualiza lista de transações
     */
    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder para itens de transação
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

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

        public void bind(Transaction transaction) {
            // Categoria
            categoryTextView.setText(transaction.getCategory());

            // Valor formatado
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            String formattedAmount = currencyFormat.format(transaction.getAmount());

            // Cor baseada no tipo (verde para receita, vermelho para despesa)
            if ("income".equals(transaction.getType())) {
                amountTextView.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
                amountTextView.setText("+" + formattedAmount);
            } else {
                amountTextView.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                amountTextView.setText("-" + formattedAmount);
            }

            // Data formatada
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
            String formattedDate = dateFormat.format(new java.util.Date(transaction.getDate()));
            dateTextView.setText(formattedDate);

            // Nota (se houver)
            if (transaction.getNote() != null && !transaction.getNote().trim().isEmpty()) {
                noteTextView.setText(transaction.getNote());
                noteTextView.setVisibility(View.VISIBLE);
            } else {
                noteTextView.setVisibility(View.GONE);
            }
        }
    }
}
