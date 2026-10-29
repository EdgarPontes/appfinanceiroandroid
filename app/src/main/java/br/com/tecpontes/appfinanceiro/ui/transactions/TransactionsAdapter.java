package br.com.tecpontes.appfinanceiro.ui.transactions;

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
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Apresenta a transação com os detalhes retornados pela API, semelhante à lista web. */
public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

    private final List<TransactionListItemDto> transactions = new ArrayList<>();
    private final OnTransactionClickListener clickListener;
    private final OnTransactionOptionsListener optionsListener;
    @Nullable private OnInvoiceAmountClickListener invoiceAmountClickListener;

    public TransactionsAdapter(
            OnTransactionClickListener clickListener,
            OnTransactionOptionsListener optionsListener) {
        this.clickListener = clickListener;
        this.optionsListener = optionsListener;
    }

    public void setInvoiceAmountClickListener(
            @Nullable OnInvoiceAmountClickListener invoiceAmountClickListener) {
        this.invoiceAmountClickListener = invoiceAmountClickListener;
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
        TransactionListItemDto transaction = transactions.get(position);
        holder.bind(transaction);

        if (transaction.isConsolidatedInvoice()) {
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
            holder.itemView.setOnClickListener(null);
            holder.moreOptions.setVisibility(View.GONE);
            holder.moreOptions.setOnClickListener(null);
            holder.amount.setClickable(true);
            holder.amount.setFocusable(true);
            holder.amount.setBackgroundResource(R.drawable.bg_text_action);
            holder.amount.setContentDescription(
                    "Ver os lançamentos que compõem esta fatura");
            holder.amount.setOnClickListener(v -> {
                if (invoiceAmountClickListener != null) {
                    invoiceAmountClickListener.onInvoiceAmountClick(transaction);
                }
            });
        } else {
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onTransactionClick(transaction);
            });
            holder.moreOptions.setVisibility(View.VISIBLE);
            holder.moreOptions.setOnClickListener(v -> {
                if (optionsListener != null) optionsListener.onTransactionOptions(v, transaction);
            });
            holder.amount.setClickable(false);
            holder.amount.setFocusable(false);
            holder.amount.setBackground(null);
            holder.amount.setContentDescription(null);
            holder.amount.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateTransactions(@Nullable List<TransactionListItemDto> items) {
        transactions.clear();
        if (items != null) transactions.addAll(items);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView description;
        private final TextView category;
        private final TextView amount;
        private final TextView date;
        private final TextView status;
        private final TextView badges;
        private final TextView runningBalance;
        private final ImageButton moreOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.descriptionTextView);
            category = itemView.findViewById(R.id.categoryTextView);
            amount = itemView.findViewById(R.id.amountTextView);
            date = itemView.findViewById(R.id.dateTextView);
            status = itemView.findViewById(R.id.statusTextView);
            badges = itemView.findViewById(R.id.badgesTextView);
            runningBalance = itemView.findViewById(R.id.runningBalanceTextView);
            moreOptions = itemView.findViewById(R.id.moreOptionsButton);
        }

        void bind(TransactionListItemDto transaction) {
            description.setText(transaction.description == null || transaction.description.trim().isEmpty()
                    ? transaction.getCategoryName() : transaction.description);
            category.setText(transaction.getCategoryName() + " · " + transaction.getSourceName());

            NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            double value = Math.abs(transaction.getAmountValue());
            amount.setText((transaction.isIncome() ? "+" : "−") + currency.format(value));
            amount.setTextColor(itemView.getContext().getColor(
                    transaction.isIncome() ? R.color.money_positive : R.color.money_negative));

            if (transaction.getDateMillis() > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
                date.setText(dateFormat.format(new Date(transaction.getDateMillis())));
            } else {
                date.setText("Data indisponível");
            }

            boolean syncError = transaction.isOfflinePending() && transaction.offlineSyncError;
            status.setText(transaction.isOfflinePending()
                    ? (syncError ? "Falha no envio" : "Aguardando envio")
                    : (transaction.isReconciled() ? "Conciliado" : "Pendente"));
            status.setTextColor(itemView.getContext().getColor(
                    syncError ? R.color.on_error_container
                            : transaction.isReconciled() ? R.color.on_success_container
                            : R.color.on_neutral_container));
            GradientDrawable statusBackground = new GradientDrawable();
            statusBackground.setColor(itemView.getContext().getColor(
                    syncError ? R.color.error_container
                            : transaction.isReconciled() ? R.color.success_container
                            : R.color.neutral_container));
            statusBackground.setCornerRadius(itemView.getResources().getDisplayMetrics().density * 12);
            status.setBackground(statusBackground);

            List<String> indicators = new ArrayList<>();
            if (transaction.partialInvoice) indicators.add("Parcial");
            if (transaction.isFixedExpenseTransaction()) indicators.add("Fixa");
            if (transaction.isInstallmentTransaction()) {
                if (transaction.installmentNumber != null && transaction.totalInstallments != null) {
                    indicators.add("Parcela " + transaction.installmentNumber + "/" + transaction.totalInstallments);
                } else {
                    indicators.add("Parcelada");
                }
            }
            badges.setText(android.text.TextUtils.join(" · ", indicators));
            badges.setVisibility(indicators.isEmpty() ? View.GONE : View.VISIBLE);

            if (transaction.hasRunningBalance()) {
                runningBalance.setText(currency.format(transaction.getRunningBalance()));
            } else {
                runningBalance.setText("—");
            }
            runningBalance.setVisibility(View.VISIBLE);
        }
    }

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionListItemDto transaction);
    }

    public interface OnTransactionOptionsListener {
        void onTransactionOptions(View anchor, TransactionListItemDto transaction);
    }

    public interface OnInvoiceAmountClickListener {
        void onInvoiceAmountClick(TransactionListItemDto invoice);
    }
}
