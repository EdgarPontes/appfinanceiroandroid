package br.com.tecpontes.appfinanceiro.ui.notifications;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.model.MessageConfigDto;
import br.com.tecpontes.appfinanceiro.model.MessageLogDto;
import br.com.tecpontes.appfinanceiro.utils.DateFormats;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lista do histórico de notificações. */
public class NotificationHistoryAdapter
        extends RecyclerView.Adapter<NotificationHistoryAdapter.LogViewHolder> {

    public interface Listener {
        void onMarkRead(@NonNull MessageLogDto log);
        void onArchive(@NonNull MessageLogDto log);
        void onDelete(@NonNull MessageLogDto log);
    }

    private final Listener listener;
    private List<MessageLogDto> logs = new ArrayList<>();
    private Map<String, String> titles = new HashMap<>();

    public NotificationHistoryAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void updateData(
            @Nullable List<MessageLogDto> newLogs,
            @Nullable Map<String, String> newTitles) {
        logs = newLogs == null ? new ArrayList<>() : new ArrayList<>(newLogs);
        if (newTitles != null) titles = new HashMap<>(newTitles);
        notifyDataSetChanged();
    }

    public void updateTitles(@Nullable Map<String, String> newTitles) {
        titles = newTitles == null ? new HashMap<>() : new HashMap<>(newTitles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        holder.bind(logs.get(position));
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    class LogViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final ImageView icon;
        private final TextView title;
        private final TextView description;
        private final TextView date;
        private final View unreadDot;
        private final ImageButton more;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.logCard);
            icon = itemView.findViewById(R.id.logIconImageView);
            title = itemView.findViewById(R.id.logTitleTextView);
            description = itemView.findViewById(R.id.logDescriptionTextView);
            date = itemView.findViewById(R.id.logDateTextView);
            unreadDot = itemView.findViewById(R.id.unreadDotView);
            more = itemView.findViewById(R.id.logMoreButton);
        }

        void bind(@NonNull MessageLogDto log) {
            String configTitle = log.configId == null ? null : titles.get(log.configId);
            title.setText(configTitle == null
                    ? itemView.getContext().getString(R.string.history_notification_fallback)
                    : configTitle);

            String content = log.content;
            description.setText(content == null || content.trim().isEmpty()
                    ? itemView.getContext().getString(R.string.history_no_description)
                    : content);

            String formattedDate = DateFormats.formatDateTime(log.sentAt);
            date.setText(formattedDate == null ? "" : formattedDate);

            boolean unread = !log.isRead();
            unreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
            title.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    unread ? android.R.color.black : android.R.color.darker_gray));

            applyTypeStyle(log);
            applyPriorityStyle(log);

            itemView.setOnClickListener(v -> {
                if (!log.isRead()) listener.onMarkRead(log);
            });
            more.setOnClickListener(v -> showActions(log));
        }

        private void applyTypeStyle(@NonNull MessageLogDto log) {
            String type = log.type == null ? "" : log.type.toLowerCase(Locale.ROOT);
            int color;
            int iconResource;

            if (type.contains(MessageConfigDto.TYPE_PAYMENT_CONFIRMATION)
                    || "PAID".equalsIgnoreCase(log.invoiceStatus)) {
                iconResource = R.drawable.ic_check_circle;
                color = R.color.money_positive;
            } else if (type.contains(MessageConfigDto.TYPE_DELAY_NOTICE)
                    || type.contains("cancellation_notice")) {
                iconResource = R.drawable.ic_warning;
                color = R.color.money_negative;
            } else if (type.contains(MessageConfigDto.TYPE_FINANCIAL_DUE_DATE)) {
                iconResource = R.drawable.ic_clock;
                color = R.color.primary_purple;
            } else {
                iconResource = R.drawable.ic_notifications_bell;
                color = R.color.primary_purple;
            }

            icon.setImageResource(iconResource);
            icon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), color)));
        }

        private void applyPriorityStyle(@NonNull MessageLogDto log) {
            int strokeColor = 0x1A000000;
            int strokeWidth = 1;
            if ("critical".equalsIgnoreCase(log.priority)) {
                strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.money_negative);
                strokeWidth = 2;
            } else if ("high".equalsIgnoreCase(log.priority)) {
                strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.money_negative);
            } else if ("medium".equalsIgnoreCase(log.priority)) {
                strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.primary_purple);
            }
            card.setStrokeColor(strokeColor);
            card.setStrokeWidth(dp(strokeWidth));
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }

        private void showActions(@NonNull MessageLogDto log) {
            PopupMenu popup = new PopupMenu(itemView.getContext(), more);
            if (!log.isRead()) {
                popup.getMenu().add(0, 1, 0, R.string.history_action_read);
            }
            popup.getMenu().add(0, 2, 1, R.string.history_action_archive);
            popup.getMenu().add(0, 3, 2, R.string.history_action_delete);

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    listener.onMarkRead(log);
                    return true;
                }
                if (item.getItemId() == 2) {
                    listener.onArchive(log);
                    return true;
                }
                if (item.getItemId() == 3) {
                    listener.onDelete(log);
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }
}
