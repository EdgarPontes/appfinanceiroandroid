package br.com.tecpontes.appfinanceiro.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.AccountRequestDto;
import br.com.tecpontes.appfinanceiro.model.AccountBalanceHistoryDto;
import br.com.tecpontes.appfinanceiro.model.AuthRequest;
import br.com.tecpontes.appfinanceiro.model.AuthResponse;
import br.com.tecpontes.appfinanceiro.model.BalanceHistoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryGoalDto;
import br.com.tecpontes.appfinanceiro.model.CategoryGoalRequestDto;
import br.com.tecpontes.appfinanceiro.model.CategoryRequestDto;
import br.com.tecpontes.appfinanceiro.model.DashboardDto;
import br.com.tecpontes.appfinanceiro.model.ForgotPasswordRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationPreviewDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationAcceptResponseDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceDeletionRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationTokenRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationVerificationRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceMemberDto;
import br.com.tecpontes.appfinanceiro.model.GoogleAuthRequestDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardRequestDto;
import br.com.tecpontes.appfinanceiro.model.ImportCategoryGoalsRequestDto;
import br.com.tecpontes.appfinanceiro.model.InvoiceDetailsDto;
import br.com.tecpontes.appfinanceiro.model.InvoicePaymentRequestDto;
import br.com.tecpontes.appfinanceiro.model.InvoicePendingRequestDto;
import br.com.tecpontes.appfinanceiro.model.InvoicePendingResponseDto;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;
import br.com.tecpontes.appfinanceiro.model.MessageLogDto;
import br.com.tecpontes.appfinanceiro.model.MessageLogsResponseDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationRequestDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationSummaryDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationPreviewRequestDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationPreviewDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationConfirmRequestDto;
import br.com.tecpontes.appfinanceiro.model.ReconciliationConfirmResultDto;
import br.com.tecpontes.appfinanceiro.model.DeactivateAccountRequestDto;
import br.com.tecpontes.appfinanceiro.model.PasswordUpdateRequestDto;
import br.com.tecpontes.appfinanceiro.model.ProfileDto;
import br.com.tecpontes.appfinanceiro.model.ProfileUpdateRequestDto;
import br.com.tecpontes.appfinanceiro.model.PushTokenRegistrationRequest;
import br.com.tecpontes.appfinanceiro.model.PushTokenRegistrationResponse;
import br.com.tecpontes.appfinanceiro.model.TransactionDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import br.com.tecpontes.appfinanceiro.model.TransactionTransferRequestDto;
import br.com.tecpontes.appfinanceiro.model.UserSettingsDto;
import br.com.tecpontes.appfinanceiro.model.UserSettingsUpdateRequestDto;
import java.util.List;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Header;
import okhttp3.ResponseBody;

/**
 * Interface para comunicação com a API App Financeiro
 */
public interface ApiService {

    // ========== AUTENTICAÇÃO ==========

    @POST("auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @POST("auth/register")
    Call<AuthResponse> register(@Body AuthRequest request);

    @POST("auth/forgot-password")
    Call<ResponseBody> forgotPassword(@Body ForgotPasswordRequestDto request);

    @POST("auth/google")
    Call<AuthResponse> loginWithGoogle(@Body GoogleAuthRequestDto request);

    // ========== COMPARTILHAMENTO FINANCEIRO ==========

    @GET("finance-spaces")
    Call<List<FinanceSpaceDto>> getFinanceSpaces();

    @GET("finance-spaces/{spaceId}/members")
    Call<List<FinanceSpaceMemberDto>> getFinanceSpaceMembers(@Path("spaceId") String spaceId);

    @GET("finance-spaces/{spaceId}/invitations")
    Call<List<FinanceSpaceInvitationDto>> getFinanceSpaceInvitations(@Path("spaceId") String spaceId);

    @POST("finance-spaces/{spaceId}/invitations")
    Call<FinanceSpaceInvitationDto> createFinanceSpaceInvitation(
            @Path("spaceId") String spaceId,
            @Body FinanceSpaceInvitationRequestDto request);

    @POST("finance-spaces/{spaceId}/invitations/{invitationId}/resend")
    Call<FinanceSpaceInvitationDto> resendFinanceSpaceInvitation(
            @Path("spaceId") String spaceId,
            @Path("invitationId") String invitationId);

    @DELETE("finance-spaces/{spaceId}/invitations/{invitationId}")
    Call<ResponseBody> revokeFinanceSpaceInvitation(
            @Path("spaceId") String spaceId,
            @Path("invitationId") String invitationId);

    @GET("finance-spaces/{spaceId}/deletion-requests")
    Call<List<FinanceSpaceDeletionRequestDto>> getFinanceSpaceDeletionRequests(
            @Path("spaceId") String spaceId);

    @POST("finance-spaces/{spaceId}/deletion-requests/{requestId}/approve")
    Call<ResponseBody> approveFinanceSpaceDeletionRequest(
            @Path("spaceId") String spaceId,
            @Path("requestId") String requestId);

    @POST("finance-spaces/{spaceId}/deletion-requests/{requestId}/reject")
    Call<ResponseBody> rejectFinanceSpaceDeletionRequest(
            @Path("spaceId") String spaceId,
            @Path("requestId") String requestId);

    @GET("finance-spaces/invitations/preview/{token}")
    Call<FinanceSpaceInvitationPreviewDto> previewFinanceSpaceInvitation(@Path("token") String token);

    @POST("finance-spaces/invitations/accept")
    Call<FinanceSpaceInvitationAcceptResponseDto> acceptFinanceSpaceInvitation(
            @Body FinanceSpaceInvitationTokenRequestDto request);

    @POST("finance-spaces/invitations/verify-email")
    Call<ResponseBody> verifyFinanceSpaceInvitationEmail(
            @Body FinanceSpaceInvitationVerificationRequestDto request);

    @POST("finance-spaces/invitations/decline")
    Call<ResponseBody> declineFinanceSpaceInvitation(@Body FinanceSpaceInvitationTokenRequestDto request);

    // ========== PERFIL ==========

    @GET("me")
    Call<ProfileDto> getMyProfile();

    @PUT("users/profile")
    Call<ResponseBody> updateMyProfile(@Body ProfileUpdateRequestDto request);

    @PUT("me/password")
    Call<ResponseBody> updateMyPassword(@Body PasswordUpdateRequestDto request);

    @Multipart
    @POST("me/avatar")
    Call<ResponseBody> uploadMyAvatar(@Part MultipartBody.Part avatar);

    @HTTP(method = "DELETE", path = "users/deactivate", hasBody = true)
    Call<ResponseBody> deactivateMyAccount(@Body DeactivateAccountRequestDto request);

    // ========== CONTAS ==========

    @GET("accounts")
    Call<List<AccountDto>> getAccounts();

    @GET("accounts")
    Call<List<AccountDto>> getAccountsWithInactive(@Query("includeInactive") String includeInactive);

    @POST("accounts")
    Call<AccountDto> createAccount(@Body AccountDto account);

    @POST("accounts")
    Call<ResponseBody> createManagedAccount(@Body AccountRequestDto account);

    @PUT("accounts/{id}")
    Call<ResponseBody> updateManagedAccount(@Path("id") String id, @Body AccountRequestDto account);

    @DELETE("accounts/{id}")
    Call<ResponseBody> deleteManagedAccount(@Path("id") String id);

    // ========== CARTÕES E FATURAS ==========

    @GET("cards/")
    Call<List<CreditCardDto>> getCreditCards();

    @GET("cards")
    Call<List<CreditCardDto>> getCreditCardsWithInactive(@Query("includeInactive") String includeInactive);

    @POST("cards")
    Call<ResponseBody> createCreditCard(@Body CreditCardRequestDto card);

    @PUT("cards/{id}")
    Call<ResponseBody> updateCreditCard(@Path("id") String id, @Body CreditCardRequestDto card);

    @DELETE("cards/{id}")
    Call<ResponseBody> deleteCreditCard(@Path("id") String id);

    @GET("invoices/{cardId}/{year}/{month}")
    Call<InvoiceDetailsDto> getInvoiceDetails(
            @Path("cardId") String cardId,
            @Path("year") int year,
            @Path("month") int month
    );

    @POST("invoices/{id}/close")
    Call<ResponseBody> closeInvoice(@Path("id") String invoiceId);

    @POST("invoices/{id}/reopen")
    Call<ResponseBody> reopenInvoice(@Path("id") String invoiceId);

    @POST("invoices/{id}/pay")
    Call<ResponseBody> payInvoice(
            @Path("id") String invoiceId,
            @Body InvoicePaymentRequestDto request
    );

    @POST("invoices/pending")
    Call<InvoicePendingResponseDto> getInvoicePending(@Body InvoicePendingRequestDto request);

    // ========== TRANSAÇÕES ==========

    @GET("transactions")
    Call<List<TransactionListItemDto>> getTransactions(
            @Query("accountIds") @Nullable String accountIds,
            @Query("startDate") @Nullable String startDate,
            @Query("endDate") @Nullable String endDate,
            @Query("type") @Nullable String type,
            @Query("categoryId") @Nullable String categoryId,
            @Query("conciliado") @Nullable String reconciled,
            @Query("q") @Nullable String query
    );

    @GET("balance-history/batch")
    Call<BalanceHistoryDto> getBalancesAtDate(
            @Query("accountIds") @NonNull String accountIds,
            @Query("date") @NonNull String date
    );

    @GET("balance-history")
    Call<AccountBalanceHistoryDto> getAccountBalanceHistory(@Query("accountId") String accountId);

    @GET("reconciliation/summary")
    Call<ReconciliationSummaryDto> getReconciliationSummary();

    @POST("maintenance/recalculate")
    Call<ResponseBody> recalculateBalances();

    @POST("reconciliation/preview")
    Call<ReconciliationPreviewDto> previewReconciliation(@Body ReconciliationPreviewRequestDto request);

    @POST("reconciliation/confirm")
    Call<ReconciliationConfirmResultDto> confirmReconciliation(@Body ReconciliationConfirmRequestDto request);

    @POST("transactions")
    Call<TransactionDto> createTransaction(@Body TransactionDto transaction);

    @POST("transactions")
    Call<ResponseBody> createTransactionFromList(@Body TransactionWriteDto transaction);

    @POST("transactions")
    Call<ResponseBody> createPendingTransactionInSpace(
            @Header("X-Finance-Space-Id") String financeSpaceId,
            @Body TransactionWriteDto transaction);

    @POST("transactions/transfer")
    Call<ResponseBody> createTransactionTransfer(@Body TransactionTransferRequestDto transfer);

    @PUT("transactions/{id}")
    Call<ResponseBody> updateTransaction(
            @Path("id") String id,
            @Body TransactionWriteDto transaction
    );

    @DELETE("transactions/{id}")
    Call<ResponseBody> deleteTransaction(
            @Path("id") String id,
            @Query("scope") @Nullable String scope
    );

    @PUT("transactions/{id}/reconcile")
    Call<ResponseBody> setTransactionReconciled(
            @Path("id") String transactionId,
            @Body ReconciliationRequestDto request
    );


    // ========== CATEGORIAS ==========

    @GET("categories")
    Call<List<CategoryDto>> getCategories(@Query("type") @Nullable String type);

    @POST("categories")
    Call<CategoryDto> createCategory(@Body CategoryRequestDto request);

    @PUT("categories/{id}")
    Call<CategoryDto> updateCategory(
            @Path("id") String id,
            @Body CategoryRequestDto request
    );

    @DELETE("categories/{id}")
    Call<ResponseBody> deleteCategory(@Path("id") String id);

    @POST("categories/defaults")
    Call<ResponseBody> createDefaultCategories();

    // ========== DASHBOARD ==========

    @GET("dashboard")
    Call<DashboardDto> getDashboard(
            @Query("startDate") @Nullable String startDate,
            @Query("endDate") @Nullable String endDate,
            @Query("accountIds") @Nullable String accountIds,
            @Query("evolutionGranularity") @Nullable String evolutionGranularity
    );

    // ========== IMPORTAÇÃO OFX ==========

    @Multipart
    @POST("import/ofx")
    Call<List<TransactionDto>> importOfx(@Part MultipartBody.Part file);

    // ========== CONFIGURAÇÕES DO USUÁRIO ==========

    @GET("user/settings")
    Call<UserSettingsDto> getUserSettings();

    @PUT("user/settings")
    Call<UserSettingsDto> updateUserSettings(@Body UserSettingsUpdateRequestDto request);

    // ========== NOTIFICAÇÕES ==========

    @POST("device-push-tokens")
    Call<PushTokenRegistrationResponse> registerDevicePushToken(
            @Body PushTokenRegistrationRequest request);

    @DELETE("device-push-tokens/{id}")
    Call<Void> deleteDevicePushToken(
            @Header("Authorization") String authorization,
            @Path("id") String registrationId);

    @GET("message-configs")
    Call<List<MessageConfigDto>> getMessageConfigs();

    @POST("message-configs")
    Call<MessageConfigDto> createMessageConfig(@Body MessageConfigDto request);

    @POST("message-configs/defaults")
    Call<ResponseBody> createDefaultMessageConfigs();

    @PUT("message-configs/{id}")
    Call<MessageConfigDto> updateMessageConfig(
            @Path("id") String id,
            @Body MessageConfigDto request
    );

    @PATCH("message-configs/{id}")
    Call<MessageConfigDto> patchMessageConfig(
            @Path("id") String id,
            @Body MessageConfigDto request
    );

    @DELETE("message-configs/{id}")
    Call<ResponseBody> deleteMessageConfig(@Path("id") String id);

    @GET("message-logs")
    Call<MessageLogsResponseDto> getMessageLogs(
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("searchTerm") @Nullable String searchTerm,
            @Query("type") @Nullable String type,
            @Query("status") @Nullable String status,
            @Query("priority") @Nullable String priority,
            @Query("showArchived") @Nullable String showArchived
    );

    @POST("message-logs/{id}/read")
    Call<MessageLogDto> markMessageLogAsRead(@Path("id") String id);

    @POST("message-logs/{id}/archive")
    Call<MessageLogDto> archiveMessageLog(@Path("id") String id);

    @DELETE("message-logs/{id}")
    Call<ResponseBody> deleteMessageLog(@Path("id") String id);

    // ========== METAS POR CATEGORIA ==========

    @GET("category-goals")
    Call<List<CategoryGoalDto>> getCategoryGoals(
            @Query("referenceMonth") @Nullable Integer referenceMonth,
            @Query("referenceYear") @Nullable Integer referenceYear
    );

    @POST("category-goals")
    Call<CategoryGoalDto> upsertCategoryGoal(@Body CategoryGoalRequestDto request);

    @POST("category-goals/import")
    Call<ResponseBody> importCategoryGoals(@Body ImportCategoryGoalsRequestDto request);
}
