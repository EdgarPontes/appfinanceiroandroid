package br.com.tecpontes.appfinanceiro.ui.help;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Uma dica exibida pelo {@link FeatureTour}. */
public final class HelpStep {

    @NonNull public final View target;
    @NonNull public final String title;
    @NonNull public final String message;
    @Nullable public final String nextLabel;
    public final boolean allowTargetClick;

    public HelpStep(
            @NonNull View target,
            @NonNull String title,
            @NonNull String message) {
        this(target, title, message, null, false);
    }

    private HelpStep(
            @NonNull View target,
            @NonNull String title,
            @NonNull String message,
            @Nullable String nextLabel,
            boolean allowTargetClick) {
        this.target = target;
        this.title = title;
        this.message = message;
        this.nextLabel = nextLabel;
        this.allowTargetClick = allowTargetClick;
    }

    /** Personaliza o rótulo do botão principal desta dica. */
    @NonNull
    public HelpStep nextLabel(@NonNull String label) {
        return new HelpStep(target, title, message, label, allowTargetClick);
    }

    /** Permite que o próprio elemento destacado seja tocado para concluir a dica. */
    @NonNull
    public HelpStep allowTargetClick() {
        return new HelpStep(target, title, message, nextLabel, true);
    }
}
