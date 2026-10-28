package br.com.tecpontes.appfinanceiro.ui.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import br.com.tecpontes.appfinanceiro.model.InvoicePendingDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class CreditCardInvoiceConsolidatorTest {

    @Test
    public void consolidatesCreditCardTransactionsByInvoice() {
        TransactionListItemDto purchase = cardTransaction("tx-1", "Compra", "100.00");
        TransactionListItemDto credit = cardTransaction("tx-2", "Estorno", "20.00");
        credit.type = "INCOME";
        TransactionListItemDto accountTransaction = new TransactionListItemDto();
        accountTransaction.id = "tx-account";
        accountTransaction.date = "2026-10-03";
        accountTransaction.amount = "25.00";
        accountTransaction.type = "EXPENSE";

        InvoicePendingDto pending = new InvoicePendingDto();
        pending.pending = "60.00";
        pending.totalPaid = "20.00";
        Map<String, InvoicePendingDto> pendingByInvoice = new LinkedHashMap<>();
        pendingByInvoice.put("invoice-1", pending);

        List<TransactionListItemDto> result = CreditCardInvoiceConsolidator.consolidate(
                Arrays.asList(purchase, credit, accountTransaction),
                pendingByInvoice,
                2026,
                Calendar.OCTOBER);

        assertEquals(2, result.size());
        TransactionListItemDto invoice = findConsolidated(result);
        assertNotNull(invoice);
        assertEquals("60.00", invoice.amount);
        assertTrue(invoice.description.startsWith("Fatura Cartão Teste"));
        assertEquals("account-1", invoice.accountId);
        assertEquals(2, invoice.invoiceTransactions.size());
        assertTrue(invoice.partialInvoice);
    }

    @Test
    public void usesComponentTotalWhenPendingIsNotLoaded() {
        TransactionListItemDto purchase = cardTransaction("tx-1", "Compra", "100.00");
        TransactionListItemDto credit = cardTransaction("tx-2", "Estorno", "20.00");
        credit.type = "INCOME";

        List<TransactionListItemDto> result = CreditCardInvoiceConsolidator.consolidate(
                Arrays.asList(purchase, credit),
                new LinkedHashMap<>(),
                2026,
                Calendar.OCTOBER);

        TransactionListItemDto invoice = findConsolidated(result);
        assertNotNull(invoice);
        assertEquals("80.00", invoice.amount);
    }

    @Test
    public void skipsFullyPaidInvoice() {
        InvoicePendingDto pending = new InvoicePendingDto();
        pending.pending = "0.00";
        pending.totalPaid = "100.00";
        Map<String, InvoicePendingDto> pendingByInvoice = new LinkedHashMap<>();
        pendingByInvoice.put("invoice-1", pending);

        List<TransactionListItemDto> result = CreditCardInvoiceConsolidator.consolidate(
                Arrays.asList(cardTransaction("tx-1", "Compra", "100.00")),
                pendingByInvoice,
                2026,
                Calendar.OCTOBER);

        assertTrue(result.isEmpty());
    }

    @Test
    public void skipsInvoiceFromAnotherMonth() {
        List<TransactionListItemDto> result = CreditCardInvoiceConsolidator.consolidate(
                Arrays.asList(cardTransaction("tx-1", "Compra", "100.00")),
                new LinkedHashMap<>(),
                2026,
                Calendar.NOVEMBER);

        assertTrue(result.isEmpty());
    }

    private TransactionListItemDto cardTransaction(
            String id, String description, String amount) {
        TransactionListItemDto transaction = new TransactionListItemDto();
        transaction.id = id;
        transaction.description = description;
        transaction.amount = amount;
        transaction.date = "2026-09-10";
        transaction.type = "EXPENSE";
        transaction.cardId = "card-1";
        transaction.invoiceId = "invoice-1";
        transaction.card = new TransactionListItemDto.CardInfo();
        transaction.card.name = "Cartão Teste";
        transaction.card.debitAccountId = "account-1";
        transaction.invoice = new TransactionListItemDto.InvoiceInfo();
        transaction.invoice.id = "invoice-1";
        transaction.invoice.dueDate = "2026-10-10";
        return transaction;
    }

    private TransactionListItemDto findConsolidated(List<TransactionListItemDto> items) {
        for (TransactionListItemDto item : items) {
            if (item.isConsolidatedInvoice()) return item;
        }
        return null;
    }
}
