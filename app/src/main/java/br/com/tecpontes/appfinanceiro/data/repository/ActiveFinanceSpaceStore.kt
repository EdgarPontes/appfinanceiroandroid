package br.com.tecpontes.appfinanceiro.data.repository

import android.content.Context
import android.content.SharedPreferences
import br.com.tecpontes.appfinanceiro.data.local.dao.AccountDao
import br.com.tecpontes.appfinanceiro.data.local.dao.TransactionDao
import br.com.tecpontes.appfinanceiro.utils.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/** Guarda o espaço ativo separado por usuário autenticado no aparelho. */
@Singleton
class ActiveFinanceSpaceStore @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenManager: TokenManager,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val cacheExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun getActiveSpaceId(): String? {
        val key = currentKey() ?: return null
        return preferences.getString(key, null)?.takeIf { it.isNotBlank() }
    }

    fun setActiveSpaceId(spaceId: String?) {
        val key = currentKey() ?: return
        val normalized = spaceId?.trim()?.takeIf { it.isNotEmpty() }
        val context = "$key|${normalized ?: PERSONAL_SPACE}"
        val previousContext = preferences.getString(KEY_LAST_ACTIVE_CONTEXT, null)
        if (previousContext == null || previousContext != context) {
            cacheExecutor.execute {
                transactionDao.deleteAll()
                accountDao.deleteAll()
            }
        }

        preferences.edit().apply {
            if (normalized == null) remove(key) else putString(key, normalized)
            putString(KEY_LAST_ACTIVE_CONTEXT, context)
        }.apply()
    }

    fun clearCurrentUserSelection() {
        currentKey()?.let { preferences.edit().remove(it).apply() }
    }

    private fun currentKey(): String? {
        val identity = tokenManager.userIdentity
        return identity?.takeIf { it.isNotBlank() }?.let { KEY_PREFIX + it }
    }

    private companion object {
        const val PREF_NAME = "active_finance_space_prefs"
        const val KEY_PREFIX = "active_space_"
        const val KEY_LAST_ACTIVE_CONTEXT = "last_active_context"
        const val PERSONAL_SPACE = "personal"
    }
}
