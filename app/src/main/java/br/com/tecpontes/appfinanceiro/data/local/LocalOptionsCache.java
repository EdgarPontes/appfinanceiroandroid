package br.com.tecpontes.appfinanceiro.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.model.AccountDto;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.model.CreditCardDto;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Mantém uma cópia das opções dos formulários para uso sem conexão. */
@Singleton
public class LocalOptionsCache {

    private static final String PREF_NAME = "app_financeiro_options_cache";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_CARDS = "cards";

    private final SharedPreferences preferences;
    private final TokenManager tokenManager;
    private final Gson gson = new Gson();

    @Inject
    public LocalOptionsCache(
            @ApplicationContext Context context, TokenManager tokenManager) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.tokenManager = tokenManager;
    }

    @NonNull
    public List<AccountDto> getAccounts() {
        return readList(KEY_ACCOUNTS, new TypeToken<List<AccountDto>>() {}.getType());
    }

    public void saveAccounts(@Nullable List<AccountDto> items) {
        saveList(KEY_ACCOUNTS, items);
    }

    @NonNull
    public List<CategoryDto> getCategories() {
        return readList(KEY_CATEGORIES, new TypeToken<List<CategoryDto>>() {}.getType());
    }

    public void saveCategories(@Nullable List<CategoryDto> items) {
        saveList(KEY_CATEGORIES, items);
    }

    /**
     * A tela de categorias carrega despesas e receitas em chamadas separadas.
     * Mescla o resultado para uma chamada não apagar a outra.
     */
    public synchronized void saveCategoryType(
            @Nullable String type, @Nullable List<CategoryDto> items) {
        List<CategoryDto> merged = new ArrayList<>();
        for (CategoryDto cached : getCategories()) {
            if (cached == null || !matchesType(cached, type)) merged.add(cached);
        }
        if (items != null) {
            for (CategoryDto item : items) {
                if (item != null) merged.add(item);
            }
        }
        saveList(KEY_CATEGORIES, merged);
    }

    private static boolean matchesType(@NonNull CategoryDto category, @Nullable String type) {
        return type != null && category.getType() != null
                && type.equalsIgnoreCase(category.getType());
    }

    @NonNull
    public List<CreditCardDto> getCards() {
        return readList(KEY_CARDS, new TypeToken<List<CreditCardDto>>() {}.getType());
    }

    public void saveCards(@Nullable List<CreditCardDto> items) {
        saveList(KEY_CARDS, items);
    }

    private <T> List<T> readList(String key, Type type) {
        if (!matchesCurrentOwner()) return new ArrayList<>();
        String json = preferences.getString(key, null);
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            List<T> items = gson.fromJson(json, type);
            return items == null ? new ArrayList<>() : items;
        } catch (RuntimeException ignored) {
            return new ArrayList<>();
        }
    }

    private <T> void saveList(String key, @Nullable List<T> items) {
        String owner = tokenManager.getUserIdentity();
        if (owner == null || owner.trim().isEmpty()) return;
        String previousOwner = preferences.getString(KEY_OWNER, null);
        SharedPreferences.Editor editor = preferences.edit();
        if (!owner.equals(previousOwner)) {
            editor.clear();
            editor.putString(KEY_OWNER, owner);
        }
        editor.putString(key, gson.toJson(items == null ? new ArrayList<>() : items));
        // Garante que o cache esteja no disco antes de o usuário fechar o app.
        editor.commit();
    }

    private boolean matchesCurrentOwner() {
        String owner = tokenManager.getUserIdentity();
        return owner != null && owner.equals(preferences.getString(KEY_OWNER, null));
    }
}
