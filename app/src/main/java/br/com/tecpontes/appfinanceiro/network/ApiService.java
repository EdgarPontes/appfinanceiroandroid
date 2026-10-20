package br.com.tecpontes.appfinanceiro.network;

import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.AuthRequest;
import br.com.tecpontes.appfinanceiro.model.AuthResponse;
import br.com.tecpontes.appfinanceiro.model.DashboardDto;
import br.com.tecpontes.appfinanceiro.model.TransactionDto;
import java.util.List;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

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
    Call<List<TransactionDto>> getTransactions(
            @Query("accountId") @Nullable String accountId,
            @Query("from") @Nullable String fromDate,
            @Query("to") @Nullable String toDate
    );

    @POST("transactions")
    Call<TransactionDto> createTransaction(@Body TransactionDto transaction);

    // ========== DASHBOARD ==========

    @GET("dashboard")
    Call<DashboardDto> getDashboard();

    // ========== IMPORTAÇÃO OFX ==========

    @Multipart
    @POST("import/ofx")
    Call<List<TransactionDto>> importOfx(@Part MultipartBody.Part file);
}
