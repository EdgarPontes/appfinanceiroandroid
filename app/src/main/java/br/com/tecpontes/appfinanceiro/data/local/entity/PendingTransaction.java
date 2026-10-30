package br.com.tecpontes.appfinanceiro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import java.util.UUID;

/**
 * Fila local de lançamentos criados sem conexão. Cada item guarda o payload que
 * será enviado à API e os dados necessários para exibi-lo na lista enquanto isso.
 */
@Entity(
        tableName = "pending_transactions",
        indices = {@Index("ownerEmail"), @Index("status"), @Index("financeSpaceId")}
)
public class PendingTransaction {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ERROR = "ERROR";

    @PrimaryKey
    @NonNull
    public String id;

    @Nullable public String ownerEmail;
    /** Target workspace captured when this offline operation was created. */
    @Nullable public String financeSpaceId;
    @Nullable public String description;
    @Nullable public String amount;
    @Nullable public String date;
    @Nullable public String type;
    @Nullable public String categoryId;
    @Nullable public String categoryName;
    @Nullable public String accountId;
    @Nullable public String accountName;
    @Nullable public String cardId;
    @Nullable public String cardName;
    @Nullable public String cardDebitAccountId;
    @Nullable public String note;
    public boolean isInstallment;
    @Nullable public Integer totalInstallments;
    @Nullable public String installmentValueType;
    @Nullable public String totalValue;
    public boolean isFixedExpense;
    public long createdAt;
    public int attempts;
    @NonNull public String status = STATUS_PENDING;
    @Nullable public String lastError;

    public PendingTransaction() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.status = STATUS_PENDING;
    }

    @NonNull
    public static PendingTransaction from(
            @Nullable String ownerEmail, @NonNull TransactionWriteDto request) {
        return from(ownerEmail, null, request);
    }

    @NonNull
    public static PendingTransaction from(
            @Nullable String ownerEmail,
            @Nullable String financeSpaceId,
            @NonNull TransactionWriteDto request) {
        PendingTransaction pending = new PendingTransaction();
        pending.ownerEmail = ownerEmail;
        pending.financeSpaceId = financeSpaceId;
        pending.description = request.description;
        pending.amount = request.amount;
        pending.date = request.date;
        pending.type = request.type;
        pending.categoryId = request.categoryId;
        pending.categoryName = request.localCategoryName;
        pending.accountId = request.accountId;
        pending.accountName = request.localAccountName;
        pending.cardId = request.cardId;
        pending.cardName = request.localCardName;
        pending.cardDebitAccountId = request.localCardDebitAccountId;
        pending.note = request.note;
        pending.isInstallment = Boolean.TRUE.equals(request.isInstallment);
        pending.totalInstallments = request.totalInstallments;
        pending.installmentValueType = request.installmentValueType;
        pending.totalValue = request.totalValue;
        pending.isFixedExpense = Boolean.TRUE.equals(request.isFixedExpense);
        return pending;
    }

    @NonNull
    public TransactionWriteDto toWriteDto() {
        TransactionWriteDto request = new TransactionWriteDto();
        request.description = description;
        request.amount = amount;
        request.date = date;
        request.type = type;
        request.categoryId = categoryId;
        request.accountId = accountId;
        request.cardId = cardId;
        request.note = note;
        request.isInstallment = isInstallment;
        request.totalInstallments = totalInstallments;
        request.installmentValueType = installmentValueType;
        request.totalValue = totalValue;
        request.isFixedExpense = isFixedExpense;
        return request;
    }

    /** Converte a pendência em item exibível na lista de transações. */
    @NonNull
    public TransactionListItemDto toListItemDto() {
        TransactionListItemDto item = new TransactionListItemDto();
        item.id = id;
        item.description = description;
        item.amount = amount;
        item.date = date;
        item.type = type;
        item.categoryId = categoryId;
        item.accountId = accountId;
        item.cardId = cardId;
        item.note = note;
        item.isInstallment = isInstallment;
        item.installmentNumber = isInstallment ? 1 : null;
        item.totalInstallments = totalInstallments;
        item.isFixedExpense = isFixedExpense;
        item.reconciled = false;
        item.offlinePending = true;
        item.offlineSyncError = STATUS_ERROR.equals(status);

        item.category = new TransactionListItemDto.CategoryInfo();
        item.category.id = categoryId;
        item.category.name = categoryName == null || categoryName.trim().isEmpty()
                ? "Sem categoria" : categoryName;

        if (accountId != null || (accountName != null && !accountName.trim().isEmpty())) {
            item.account = new TransactionListItemDto.AccountInfo();
            item.account.id = accountId;
            item.account.name = accountName;
        }
        if (cardId != null || (cardName != null && !cardName.trim().isEmpty())) {
            item.card = new TransactionListItemDto.CardInfo();
            item.card.id = cardId;
            item.card.name = cardName;
            item.card.debitAccountId = cardDebitAccountId;
        }
        return item;
    }
}
