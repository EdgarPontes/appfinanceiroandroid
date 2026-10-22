package br.com.tecpontes.appfinanceiro.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.AuthRequest;
import br.com.tecpontes.appfinanceiro.model.AuthResponse;
import br.com.tecpontes.appfinanceiro.model.BalanceHistoryDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.DashboardDto;
import br.com.tecpontes.appfinanceiro.model.TransactionDto;
import br.com.tecpontes.appfinanceiro.model.TransactionListItemDto;
import br.com.tecpontes.appfinanceiro.model.TransactionWriteDto;
import java.util.List;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
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

    // ========== CONTAS ==========

    @GET("accounts")
    Call<List<AccountDto>> getAccounts();

    @POST("accounts")
    Call<AccountDto> createAccount(@Body AccountDto account);

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

    @POST("transactions")
    Call<TransactionDto> createTransaction(@Body TransactionDto transaction);

    @POST("transactions")
    Call<ResponseBody> createTransactionFromList(@Body TransactionWriteDto transaction);

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

    // ========== CATEGORIAS ==========

    @GET("categories")
    Call<List<CategoryDto>> getCategories(@Query("type") @Nullable String type);

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
}
