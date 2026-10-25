package br.com.tecpontes.appfinanceiro.ui.notifications;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.ActivityNotificationHistoryBinding;
import br.com.tecpontes.appfinanceiro.model.MessageLogDto;
import br.com.tecpontes.appfinanceiro.notifications.LocalNotificationPublisher;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.NotificationHistoryViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Histórico com busca, filtros e ações de leitura/arquivamento. */
@AndroidEntryPoint
public class NotificationHistoryActivity extends AppCompatActivity {

    private static final long SEARCH_DEBOUNCE_MS = 350;

    private ActivityNotificationHistoryBinding binding;
    private NotificationHistoryViewModel viewModel;
    private NotificationHistoryAdapter adapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private boolean suppressSearch;

    @Inject TokenManager tokenManager;

    private final Runnable searchRunnable = () -> {
        if (binding == null) return;
        viewModel.setSearchTerm(binding.searchEditText.getText().toString());
        updateFilterSummary();
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityNotificationHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        BottomNavigationHelper.setup(this, R.id.navigation_menu);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NotificationHistoryViewModel.class);
        adapter = new NotificationHistoryAdapter(new NotificationHistoryAdapter.Listener() {
            @Override public void onMarkRead(@NonNull MessageLogDto log) {
                viewModel.markAsRead(log);
            }

            @Override public void onArchive(@NonNull MessageLogDto log) {
                viewModel.archive(log);
            }

            @Override public void onDelete(@NonNull MessageLogDto log) {
                confirmDelete(log);
            }
        });

        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.historyRecyclerView.setAdapter(adapter);

        binding.searchEditText.setText(viewModel.getSearchTerm());
        updateFilterSummary();

        setupListeners();
        setupObservers();
        SessionExpiredNavigation.observe(this, tokenManager);
        viewModel.load();
    }

    private void setupListeners() {
        binding.refreshButton.setOnClickListener(v -> viewModel.load());
        binding.markAllReadButton.setOnClickListener(v -> viewModel.markAllAsRead());
        binding.filterButton.setOnClickListener(v -> showFilterMenu());
        binding.clearFiltersButton.setOnClickListener(v -> clearFilters());

        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable editable) {
                if (suppressSearch) return;
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    private void setupObservers() {
        viewModel.logsData.observe(this, logs -> {
            adapter.updateData(logs, viewModel.configTitlesData.getValue());
            updateEmptyState();
            checkAndHighlightTargetNotification(logs);
        });

        viewModel.configTitlesData.observe(this,
                titles -> adapter.updateTitles(titles));

        viewModel.isLoading.observe(this, loading -> {
            binding.historyProgressBar.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            binding.refreshButton.setEnabled(!Boolean.TRUE.equals(loading));
            updateEmptyState();
        });

        viewModel.isProcessing.observe(this, processing -> {
            boolean enabled = !Boolean.TRUE.equals(processing);
            binding.refreshButton.setEnabled(enabled);
            binding.markAllReadButton.setEnabled(enabled
                    && currentUnreadCount() > 0);
            binding.filterButton.setEnabled(enabled);
        });

        viewModel.unreadCountData.observe(this, count -> {
            int unread = count == null ? 0 : count;
            binding.markAllReadButton.setEnabled(unread > 0
                    && !Boolean.TRUE.equals(viewModel.isProcessing.getValue()));
        });

        viewModel.errorMessage.observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                binding.historyErrorTextView.setVisibility(View.GONE);
            } else {
                binding.historyErrorTextView.setText(message);
                binding.historyErrorTextView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.successMessage.observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int currentUnreadCount() {
        Integer count = viewModel.unreadCountData.getValue();
        return count == null ? 0 : count;
    }

    private void updateEmptyState() {
        if (binding == null) return;
        List<MessageLogDto> logs = viewModel.logsData.getValue();
        boolean empty = (logs == null || logs.isEmpty())
                && !Boolean.TRUE.equals(viewModel.isLoading.getValue());
        binding.historyEmptyLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.historyRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void checkAndHighlightTargetNotification(@Nullable List<MessageLogDto> logs) {
        if (logs == null || logs.isEmpty()) return;
        String targetId = getIntent().getStringExtra(LocalNotificationPublisher.EXTRA_LOG_ID);
        if (targetId == null) return;

        getIntent().removeExtra(LocalNotificationPublisher.EXTRA_LOG_ID);

        for (MessageLogDto log : logs) {
            if (targetId.equals(log.id)) {
                if (!log.isRead()) {
                    viewModel.markAsRead(log);
                }
                break;
            }
        }
    }

    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(this, binding.filterButton);
        String selectedType = viewModel.getTypeCategory();

        popup.getMenu().add(0, 101, 0, R.string.history_type_all)
                .setCheckable(true)
                .setChecked(NotificationHistoryViewModel.FILTER_ALL.equals(selectedType));
        popup.getMenu().add(0, 102, 1, R.string.history_type_success)
                .setCheckable(true)
                .setChecked(NotificationHistoryViewModel.FILTER_SUCCESS.equals(selectedType));
        popup.getMenu().add(0, 103, 2, R.string.history_type_info)
                .setCheckable(true)
                .setChecked(NotificationHistoryViewModel.FILTER_INFO.equals(selectedType));
        popup.getMenu().add(0, 104, 3, R.string.history_type_alert)
                .setCheckable(true)
                .setChecked(NotificationHistoryViewModel.FILTER_ALERT.equals(selectedType));

        popup.getMenu().add(1, 200, 4, R.string.history_show_archived)
                .setCheckable(true)
                .setChecked(viewModel.isShowArchived());
        popup.getMenu().add(2, 300, 5, R.string.history_clear_filters);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 101) viewModel.setTypeCategory(NotificationHistoryViewModel.FILTER_ALL);
            else if (id == 102) viewModel.setTypeCategory(NotificationHistoryViewModel.FILTER_SUCCESS);
            else if (id == 103) viewModel.setTypeCategory(NotificationHistoryViewModel.FILTER_INFO);
            else if (id == 104) viewModel.setTypeCategory(NotificationHistoryViewModel.FILTER_ALERT);
            else if (id == 200) viewModel.setShowArchived(!viewModel.isShowArchived());
            else if (id == 300) {
                clearFilters();
                return true;
            } else {
                return false;
            }
            updateFilterSummary();
            return true;
        });
        popup.show();
    }

    private void clearFilters() {
        suppressSearch = true;
        binding.searchEditText.setText("");
        suppressSearch = false;
        searchHandler.removeCallbacks(searchRunnable);
        viewModel.clearFilters();
        updateFilterSummary();
    }

    private void updateFilterSummary() {
        if (binding == null) return;
        String type = viewModel.getTypeCategory();
        boolean hasType = !NotificationHistoryViewModel.FILTER_ALL.equals(type);
        boolean hideArchived = !viewModel.isShowArchived();
        String term = binding.searchEditText.getText().toString().trim();

        List<String> parts = new ArrayList<>();
        if (hasType) parts.add(typeLabel(type));
        if (hideArchived) parts.add("sem arquivadas");
        if (!term.isEmpty()) parts.add("\"" + term + "\"");

        if (parts.isEmpty()) {
            binding.filterSummaryTextView.setVisibility(View.GONE);
        } else {
            binding.filterSummaryTextView.setText("Filtros ativos: " + String.join(" · ", parts));
            binding.filterSummaryTextView.setVisibility(View.VISIBLE);
        }
    }

    private String typeLabel(String type) {
        if (NotificationHistoryViewModel.FILTER_SUCCESS.equals(type)) {
            return getString(R.string.history_type_success);
        }
        if (NotificationHistoryViewModel.FILTER_INFO.equals(type)) {
            return getString(R.string.history_type_info);
        }
        if (NotificationHistoryViewModel.FILTER_ALERT.equals(type)) {
            return getString(R.string.history_type_alert);
        }
        return getString(R.string.history_type_all);
    }

    private void confirmDelete(@NonNull MessageLogDto log) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.history_delete_title)
                .setMessage(R.string.history_delete_message)
                .setNegativeButton(R.string.history_cancel, null)
                .setPositiveButton(R.string.history_delete_confirm,
                        (dialog, which) -> viewModel.delete(log))
                .show();
    }

    @Override
    protected void onDestroy() {
        searchHandler.removeCallbacks(searchRunnable);
        super.onDestroy();
        binding = null;
    }

    @Override
    protected void onNewIntent(@NonNull android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (viewModel != null) {
            viewModel.load();
        }
    }
}
