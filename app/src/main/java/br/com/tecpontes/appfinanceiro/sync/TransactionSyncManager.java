package br.com.tecpontes.appfinanceiro.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.data.local.dao.PendingTransactionDao;
import br.com.tecpontes.appfinanceiro.data.local.entity.PendingTransaction;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.utils.ApiErrorUtils;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.ResponseBody;
import retrofit2.Response;

/** Executa o envio bloqueante da fila para o WorkManager. */
@Singleton
public class TransactionSyncManager {

    private final PendingTransactionDao pendingDao;
    private final ApiService apiService;
    private final TokenManager tokenManager;

    @Inject
    public TransactionSyncManager(
            PendingTransactionDao pendingDao,
            ApiService apiService,
            TokenManager tokenManager) {
        this.pendingDao = pendingDao;
        this.apiService = apiService;
        this.tokenManager = tokenManager;
    }

    public void syncPendingBlocking() throws IOException {
        if (!tokenManager.isLoggedIn()) return;
        String owner = tokenManager.getUserIdentity();
        if (owner == null || owner.trim().isEmpty()) return;

        while (true) {
            PendingTransaction pending = pendingDao.getNextReady(owner);
            if (pending == null) return;

            final Response<ResponseBody> response;
            try {
                String targetSpace = pending.financeSpaceId == null
                        ? "personal" : pending.financeSpaceId;
                response = apiService.createPendingTransactionInSpace(
                        targetSpace, pending.toWriteDto()).execute();
            } catch (IOException error) {
                pendingDao.markAttempt(pending.id, messageOf(error));
                throw error;
            }

            if (response.isSuccessful()) {
                ApiErrorUtils.closeBody(response.body());
                pendingDao.deleteById(pending.id);
                continue;
            }

            String error = ApiErrorUtils.readError(
                    response, "Não foi possível sincronizar o lançamento");
            int code = response.code();
            if (code == 401) {
                // O login agenda novamente a fila após autenticar.
                return;
            }
            if (code == 408 || code == 429 || code >= 500) {
                pendingDao.markAttempt(pending.id, error);
                throw new IOException(error);
            }
            pendingDao.markError(pending.id, error);
        }
    }

    @NonNull
    private static String messageOf(@Nullable Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()) {
            return "Falha de conexão";
        }
        return error.getMessage();
    }
}
