package br.com.tecpontes.appfinanceiro.data.local.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import org.junit.Test;

public class PendingTransactionTest {

    @Test
    public void preservesPayloadAndDisplayMetadata() {
        TransactionWriteDto request = new TransactionWriteDto();
        request.description = "Compra offline";
        request.amount = "45.90";
        request.date = "2026-10-29T12:00:00.000-03:00";
        request.type = "EXPENSE";
        request.categoryId = "category-1";
        request.localCategoryName = "Mercado";
        request.cardId = "card-1";
        request.localCardName = "Cartão Teste";
        request.localCardDebitAccountId = "account-1";
        request.note = "Observação";

        PendingTransaction pending = PendingTransaction.from("user@example.com", request);

        assertEquals("user@example.com", pending.ownerEmail);
        assertEquals(PendingTransaction.STATUS_PENDING, pending.status);

        TransactionWriteDto payload = pending.toWriteDto();
        assertEquals("Compra offline", payload.description);
        assertEquals("45.90", payload.amount);
        assertEquals("category-1", payload.categoryId);
        assertEquals("card-1", payload.cardId);
        assertEquals("Observação", payload.note);

        TransactionListItemDto item = pending.toListItemDto();
        assertTrue(item.isOfflinePending());
        assertFalse(item.offlineSyncError);
        assertEquals("Mercado", item.getCategoryName());
        assertEquals("Cartão Teste", item.getSourceName());
        assertEquals("account-1", item.card.debitAccountId);
    }

    @Test
    public void exposesPermanentSyncError() {
        PendingTransaction pending = new PendingTransaction();
        pending.status = PendingTransaction.STATUS_ERROR;

        assertTrue(pending.toListItemDto().offlineSyncError);
    }
}
