package br.com.tecpontes.appfinanceiro.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.repository.NotificationsRepository;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;
import br.com.tecpontes.appfinanceiro.model.MessageLogDto;
import br.com.tecpontes.appfinanceiro.model.MessageLogsResponseDto;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/** Estado e ações do histórico de notificações. */
@HiltViewModel
public class NotificationHistoryViewModel extends ViewModel {

    public static final String FILTER_ALL = "all";
    public static final String FILTER_SUCCESS = "success";
    public static final String FILTER_INFO = "info";
    public static final String FILTER_ALERT = "alert";

    private final NotificationsRepository repository;

    private final MutableLiveData<List<MessageLogDto>> logs =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, String>> configTitles =
            new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> processing = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> success = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);

    public final LiveData<List<MessageLogDto>> logsData = logs;
    public final LiveData<Map<String, String>> configTitlesData = configTitles;
    public final LiveData<Boolean> isLoading = loading;
    public final LiveData<Boolean> isProcessing = processing;
    public final LiveData<String> errorMessage = error;
    public final LiveData<String> successMessage = success;
    public final LiveData<Integer> unreadCountData = unreadCount;

    private String searchTerm = "";
    private String typeCategory = FILTER_ALL;
    private boolean showArchived = true;
    private List<MessageLogDto> allLogs = new ArrayList<>();

    @Inject
    public NotificationHistoryViewModel(NotificationsRepository repository) {
        this.repository = repository;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(null);

        repository.getConfigs(new NotificationsRepository.ResultCallback<List<MessageConfigDto>>() {
            @Override public void onSuccess(@Nullable List<MessageConfigDto> result) {
                Map<String, String> titles = new HashMap<>();
                if (result != null) {
                    for (MessageConfigDto config : result) {
                        if (config.id != null) {
                            titles.put(config.id, config.title == null ? "Notificação" : config.title);
                        }
                    }
                }
                configTitles.postValue(titles);
            }

            @Override public void onError(@NonNull String message) {
                // O histórico continua funcional sem os títulos dos templates.
            }
        });

        repository.getLogs(searchTerm, null, null, showArchived,
                new NotificationsRepository.ResultCallback<MessageLogsResponseDto>() {
                    @Override public void onSuccess(@Nullable MessageLogsResponseDto result) {
                        loading.postValue(false);
                        allLogs = result == null ? new ArrayList<>() : result.logsOrEmpty();
                        applyFilter();
                    }

                    @Override public void onError(@NonNull String message) {
                        loading.postValue(false);
                        error.postValue(message);
                    }
                });
    }

    public void setSearchTerm(@Nullable String term) {
        searchTerm = term == null ? "" : term.trim();
        load();
    }

    @NonNull
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setTypeCategory(@NonNull String category) {
        typeCategory = category;
        applyFilter();
    }

    public void setShowArchived(boolean show) {
        showArchived = show;
        load();
    }

    public boolean isShowArchived() {
        return showArchived;
    }

    public String getTypeCategory() {
        return typeCategory;
    }

    public void clearFilters() {
        searchTerm = "";
        typeCategory = FILTER_ALL;
        showArchived = true;
        load();
    }

    public void markAsRead(@NonNull MessageLogDto log) {
        if (log.id == null) return;
        repository.markRead(log.id, new NotificationsRepository.ResultCallback<MessageLogDto>() {
            @Override public void onSuccess(@Nullable MessageLogDto result) {
                String readAt = result != null && result.readAt != null
                        ? result.readAt : Instant.now().toString();
                for (MessageLogDto item : allLogs) {
                    if (item.id != null && item.id.equals(log.id)) item.readAt = readAt;
                }
                applyFilter();
            }

            @Override public void onError(@NonNull String message) {
                error.postValue(message);
            }
        });
    }

    public void markAllAsRead() {
        List<String> ids = new ArrayList<>();
        for (MessageLogDto log : allLogs) {
            if (log.id != null && !log.isRead()) ids.add(log.id);
        }
        if (ids.isEmpty()) return;

        processing.setValue(true);
        repository.markAllAsRead(ids, new NotificationsRepository.ActionCallback() {
            @Override public void onSuccess() {
                processing.postValue(false);
                String now = Instant.now().toString();
                for (MessageLogDto log : allLogs) {
                    if (!log.isRead()) log.readAt = now;
                }
                applyFilter();
                success.postValue("Todas as notificações foram marcadas como lidas.");
            }

            @Override public void onError(@NonNull String message) {
                processing.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void archive(@NonNull MessageLogDto log) {
        if (log.id == null) return;
        processing.setValue(true);
        repository.archive(log.id, new NotificationsRepository.ResultCallback<MessageLogDto>() {
            @Override public void onSuccess(@Nullable MessageLogDto result) {
                processing.postValue(false);
                removeFromList(log.id);
                success.postValue("Notificação arquivada.");
            }

            @Override public void onError(@NonNull String message) {
                processing.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void delete(@NonNull MessageLogDto log) {
        if (log.id == null) return;
        processing.setValue(true);
        repository.deleteLog(log.id, new NotificationsRepository.ActionCallback() {
            @Override public void onSuccess() {
                processing.postValue(false);
                removeFromList(log.id);
                success.postValue("Notificação excluída.");
            }

            @Override public void onError(@NonNull String message) {
                processing.postValue(false);
                error.postValue(message);
            }
        });
    }

    private void removeFromList(@Nullable String id) {
        if (id == null) return;
        List<MessageLogDto> copy = new ArrayList<>();
        for (MessageLogDto item : allLogs) {
            if (item.id == null || !item.id.equals(id)) copy.add(item);
        }
        allLogs = copy;
        applyFilter();
    }

    private void applyFilter() {
        List<MessageLogDto> filtered = new ArrayList<>();
        for (MessageLogDto log : allLogs) {
            if (matchesCategory(log)) filtered.add(log);
        }
        logs.setValue(filtered);

        int unread = 0;
        for (MessageLogDto log : allLogs) {
            if (!log.isRead()) unread++;
        }
        unreadCount.setValue(unread);
    }

    private boolean matchesCategory(@NonNull MessageLogDto log) {
        if (FILTER_ALL.equals(typeCategory)) return true;
        String type = log.type == null ? "" : log.type.toLowerCase(java.util.Locale.ROOT);
        if (FILTER_SUCCESS.equals(typeCategory)) {
            return type.contains(MessageConfigDto.TYPE_PAYMENT_CONFIRMATION)
                    || "PAID".equalsIgnoreCase(log.invoiceStatus);
        }
        if (FILTER_ALERT.equals(typeCategory)) {
            return type.contains(MessageConfigDto.TYPE_DELAY_NOTICE);
        }
        // Informativas: vencimentos, lembretes e cancelamentos.
        return type.contains(MessageConfigDto.TYPE_FINANCIAL_DUE_DATE)
                || type.contains(MessageConfigDto.TYPE_CUSTOM_REMINDER)
                || type.contains("cancellation_notice");
    }
}
