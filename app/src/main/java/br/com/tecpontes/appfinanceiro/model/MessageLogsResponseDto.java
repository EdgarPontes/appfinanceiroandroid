package br.com.tecpontes.appfinanceiro.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Resposta paginada de GET /api/message-logs. */
public class MessageLogsResponseDto {

    @Nullable @SerializedName("logs") public List<MessageLogDto> logs;
    @Nullable @SerializedName("stats") public MessageStatsDto stats;

    public List<MessageLogDto> logsOrEmpty() {
        return logs == null ? new ArrayList<>() : logs;
    }
}
