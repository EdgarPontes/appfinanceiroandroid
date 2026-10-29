package br.com.tecpontes.appfinanceiro.data.repository;

import androidx.annotation.NonNull;
import br.com.tecpontes.appfinanceiro.data.local.LocalOptionsCache;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Atualiza silenciosamente as opções usadas pelos formulários offline. */
@Singleton
public class OptionsBootstrap {

    private final ApiService apiService;
    private final LocalOptionsCache cache;
    private final TokenManager tokenManager;

    @Inject
    public OptionsBootstrap(
            ApiService apiService, LocalOptionsCache cache, TokenManager tokenManager) {
        this.apiService = apiService;
        this.cache = cache;
        this.tokenManager = tokenManager;
    }

    public void refresh() {
        if (!tokenManager.isLoggedIn()) return;
        String owner = tokenManager.getUserIdentity();
        if (owner == null || owner.trim().isEmpty()) return;

        apiService.getAccounts().enqueue(new Callback<List<AccountDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<AccountDto>> ignored,
                    @NonNull Response<List<AccountDto>> response) {
                if (response.isSuccessful()) cache.saveAccounts(response.body());
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<AccountDto>> ignored,
                    @NonNull Throwable error) {
                // Mantém o cache anterior.
            }
        });

        apiService.getCategories(null).enqueue(new Callback<List<CategoryDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<CategoryDto>> ignored,
                    @NonNull Response<List<CategoryDto>> response) {
                if (response.isSuccessful()) cache.saveCategories(response.body());
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<CategoryDto>> ignored,
                    @NonNull Throwable error) {
                // Mantém o cache anterior.
            }
        });

        apiService.getCreditCards().enqueue(new Callback<List<CreditCardDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<CreditCardDto>> ignored,
                    @NonNull Response<List<CreditCardDto>> response) {
                if (response.isSuccessful()) cache.saveCards(response.body());
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<CreditCardDto>> ignored,
                    @NonNull Throwable error) {
                // Mantém o cache anterior.
            }
        });
    }
}
