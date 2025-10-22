package br.com.tecpontes.appfinanceiro.ui.transactions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
 * Adapter para RecyclerView de transações na tela de transações
 */
public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

    private List<Transaction> transactions;
    private final OnTransactionEditListener editListener;
    private final OnTransactionDeleteListener deleteListener;

    public TransactionsAdapter(List<Transaction> transactions,
                              OnTransactionEditListener editListener,
                              OnTransactionDeleteListener deleteListener) {
        this.transactions = transactions;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction, editListener, deleteListener);
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
        private final ImageButton editButton;
        private final ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            noteTextView = itemView.findViewById(R.id.noteTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Transaction transaction,
                        OnTransactionEditListener editListener,
                        OnTransactionDeleteListener deleteListener) {
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

            // Listeners dos botões
            editButton.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onTransactionEdit(transaction);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onTransactionDelete(transaction.getId());
                }
            });
        }
    }

    /**
     * Interfaces para callbacks
     */
    public interface OnTransactionEditListener {
        void onTransactionEdit(Transaction transaction);
    }

    public interface OnTransactionDeleteListener {
        void onTransactionDelete(String transactionId);
    }
}
