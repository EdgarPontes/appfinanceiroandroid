package br.com.tecpontes.appfinanceiro.ui.transactions;

import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.InvoicePendingDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Agrupa os lançamentos de cartão em uma única linha por fatura, como na tela web.
 * A data exibida é o vencimento da fatura e o valor é o saldo pendente quando a
 * API já o informou; caso contrário, usa a soma dos lançamentos do grupo.
 */
public final class CreditCardInvoiceConsolidator {

    private CreditCardInvoiceConsolidator() {
    }

    public static List<TransactionListItemDto> consolidate(
            @Nullable List<TransactionListItemDto> transactions,
            @Nullable Map<String, InvoicePendingDto> pendingByInvoice,
            int referenceYear,
            int referenceMonth) {
        List<TransactionListItemDto> result = new ArrayList<>();
        if (transactions == null || transactions.isEmpty()) return result;

        Map<String, List<TransactionListItemDto>> groups = new LinkedHashMap<>();
        for (TransactionListItemDto item : transactions) {
            if (item == null) continue;
            if (item.cardId != null && item.invoiceId != null) {
                String key = item.cardId + "-" + item.invoiceId;
                List<TransactionListItemDto> group = groups.get(key);
                if (group == null) {
                    group = new ArrayList<>();
                    groups.put(key, group);
                }
                group.add(item);
            } else {
                result.add(item);
            }
        }

        for (List<TransactionListItemDto> group : groups.values()) {
            TransactionListItemDto invoiceRow = buildInvoiceRow(
                    group, pendingByInvoice, referenceYear, referenceMonth);
            if (invoiceRow != null) result.add(invoiceRow);
        }
        return result;
    }

    @Nullable
    private static TransactionListItemDto buildInvoiceRow(
            List<TransactionListItemDto> group,
            @Nullable Map<String, InvoicePendingDto> pendingByInvoice,
            int referenceYear,
            int referenceMonth) {
        TransactionListItemDto first = group.get(0);
        TransactionListItemDto invoiceItem = null;
        for (TransactionListItemDto item : group) {
            if (item.invoice != null && item.invoice.dueDate != null) {
                invoiceItem = item;
                break;
            }
        }
        if (invoiceItem == null) return null;
        TransactionListItemDto.InvoiceInfo invoice = invoiceItem.invoice;

        long dueMillis = invoiceItem.getInvoiceDueDateMillis();
        if (dueMillis <= 0) return null;

        Calendar due = Calendar.getInstance();
        due.setTimeInMillis(dueMillis);
        if (due.get(Calendar.YEAR) != referenceYear
                || due.get(Calendar.MONTH) != referenceMonth) {
            return null;
        }

        InvoicePendingDto pending = pendingByInvoice == null || first.invoiceId == null
                ? null : pendingByInvoice.get(first.invoiceId);

        double componentsTotal = 0;
        boolean allReconciled = true;
        for (TransactionListItemDto item : group) {
            componentsTotal += item.isIncome() ? -item.getAmountValue() : item.getAmountValue();
            if (!item.isReconciled()) allReconciled = false;
        }

        double displayAmount;
        if (pending != null) {
            displayAmount = pending.pendingValue();
            if (displayAmount <= 0) return null;
        } else {
            displayAmount = componentsTotal;
            if (displayAmount <= 0) return null;
        }

        TransactionListItemDto cardItem = first;
        for (TransactionListItemDto item : group) {
            if (item.card != null) {
                cardItem = item;
                break;
            }
        }
        String debitAccountId = cardItem.card == null ? null : cardItem.card.debitAccountId;
        if (debitAccountId == null) {
            for (TransactionListItemDto item : group) {
                if (item.accountId != null) {
                    debitAccountId = item.accountId;
                    break;
                }
            }
        }

        TransactionListItemDto row = new TransactionListItemDto();
        row.id = "invoice-" + first.cardId + "-" + first.invoiceId;
        row.description = invoiceDescription(cardItem, dueMillis);
        row.amount = String.format(Locale.US, "%.2f", displayAmount);
        row.date = invoice.dueDate;
        row.type = "EXPENSE";
        row.accountId = debitAccountId;
        row.cardId = first.cardId;
        row.invoiceId = first.invoiceId;
        row.category = new TransactionListItemDto.CategoryInfo();
        row.category.name = "Fatura Cartão";
        row.category.color = "#ef4444";
        row.card = cardItem.card;
        row.invoice = invoice;
        row.reconciled = allReconciled;
        row.isInstallment = false;
        row.isFixedExpense = false;
        row.invoiceTransactions = new ArrayList<>(group);
        row.invoicePending = pending;
        row.partialInvoice = pending != null && pending.totalPaidValue() > 0;
        return row;
    }

    private static String invoiceDescription(TransactionListItemDto first, long dueMillis) {
        String cardName = first.card != null && first.card.name != null
                && !first.card.name.trim().isEmpty() ? first.card.name : "Cartão";
        String month = new SimpleDateFormat("MMMM 'de' yyyy", new Locale("pt", "BR"))
                .format(new Date(dueMillis));
        if (!month.isEmpty()) {
            month = month.substring(0, 1).toUpperCase(new Locale("pt", "BR"))
                    + month.substring(1);
        }
        return "Fatura " + cardName + " - " + month;
    }
}
