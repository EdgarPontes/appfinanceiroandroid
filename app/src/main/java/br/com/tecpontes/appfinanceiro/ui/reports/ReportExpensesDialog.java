package br.com.tecpontes.appfinanceiro.ui.reports;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.DialogReportExpensesBinding;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Diálogo com a lista detalhada de despesas do período. */
public class ReportExpensesDialog extends Dialog {

    private DialogReportExpensesBinding binding;
    private final List<TransactionListItemDto> expenses;
    private final double total;
    private final ReportExpenseAdapter adapter = new ReportExpenseAdapter();

    public ReportExpensesDialog(
            @NonNull Context context,
            @NonNull List<TransactionListItemDto> expenses,
            double total) {
        super(context);
        this.expenses = expenses;
        this.total = total;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        binding = DialogReportExpensesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        binding.reportExpensesSubtitleTextView.setText(
                getContext().getString(R.string.reports_expenses_dialog_subtitle,
                        expenses.size(), currency.format(total)));

        binding.reportExpensesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.reportExpensesRecyclerView.setAdapter(adapter);
        adapter.setData(expenses);

        binding.closeReportExpensesButton.setOnClickListener(v -> dismiss());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window == null) return;
        window.setBackgroundDrawableResource(android.R.color.transparent);

        int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.94f);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}
