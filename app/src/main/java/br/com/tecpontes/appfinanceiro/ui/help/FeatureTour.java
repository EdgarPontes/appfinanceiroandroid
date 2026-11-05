package br.com.tecpontes.appfinanceiro.ui.help;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import br.com.tecpontes.appfinanceiro.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

/**
 * Exibe um guia contextual sobre a tela atual.
 *
 * <p>O overlay adiciona uma camada escurecida, recorta o elemento em destaque e posiciona
 * um balão acima ou abaixo dele. Funciona tanto em Activities quanto em Dialogs.</p>
 */
public final class FeatureTour {

    public interface Listener {
        void onCompleted();
        void onSkipped();
        void onTargetClicked();
    }

    private static final int RESULT_COMPLETED = 1;
    private static final int RESULT_SKIPPED = 2;
    private static final int RESULT_TARGET = 3;

    @NonNull private final ViewGroup host;
    @NonNull private final View overlay;
    @NonNull private final TourSpotlightView spotlight;
    @NonNull private final MaterialCardView bubble;
    @NonNull private final TextView progressTextView;
    @NonNull private final TextView titleTextView;
    @NonNull private final TextView messageTextView;
    @NonNull private final MaterialButton skipButton;
    @NonNull private final MaterialButton backButton;
    @NonNull private final MaterialButton nextButton;
    @NonNull private final List<HelpStep> steps;
    @NonNull private final RectF currentHighlight = new RectF();
    @NonNull private final ViewTreeObserver.OnScrollChangedListener treeScrollListener =
            this::handleTreeScrollChanged;
    @Nullable private final Listener listener;

    private int index;
    private int attachAttempts;
    private boolean targetPressed;
    private boolean positionPosted;
    private boolean adjustingTarget;
    private boolean ended;
    @Nullable private HelpStep currentStep;

    private FeatureTour(
            @NonNull ViewGroup host,
            @NonNull List<HelpStep> steps,
            @Nullable Listener listener) {
        this.host = host;
        this.steps = steps;
        this.listener = listener;

        Context context = host.getContext();
        overlay = LayoutInflater.from(context).inflate(R.layout.view_feature_tour, host, false);
        spotlight = overlay.findViewById(R.id.helpTourSpotlight);
        bubble = overlay.findViewById(R.id.helpTourBubble);
        progressTextView = overlay.findViewById(R.id.helpTourProgress);
        titleTextView = overlay.findViewById(R.id.helpTourTitle);
        messageTextView = overlay.findViewById(R.id.helpTourMessage);
        skipButton = overlay.findViewById(R.id.helpTourSkipButton);
        backButton = overlay.findViewById(R.id.helpTourBackButton);
        nextButton = overlay.findViewById(R.id.helpTourNextButton);

        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setElevation(dp(context, 1000));
        overlay.setOnTouchListener(this::handleOverlayTouch);

        skipButton.setOnClickListener(view -> finish(RESULT_SKIPPED));
        backButton.setOnClickListener(view -> {
            if (index > 0) {
                index--;
                renderStep();
            }
        });
        nextButton.setOnClickListener(view -> {
            if (index >= steps.size() - 1) {
                finish(RESULT_COMPLETED);
            } else {
                index++;
                renderStep();
            }
        });
    }

    /** Abre o guia sobre o conteúdo de uma Activity. */
    @Nullable
    public static FeatureTour show(
            @NonNull Activity activity,
            @NonNull List<HelpStep> steps,
            @Nullable Listener listener) {
        if (activity.isFinishing() || activity.isDestroyed()) return null;
        ViewGroup host = activity.findViewById(android.R.id.content);
        return show(host, steps, listener);
    }

    /** Abre o guia sobre o conteúdo de um Dialog já exibido. */
    @Nullable
    public static FeatureTour show(
            @NonNull Dialog dialog,
            @NonNull List<HelpStep> steps,
            @Nullable Listener listener) {
        Window window = dialog.getWindow();
        if (window == null) return null;
        View decorView = window.getDecorView();
        ViewGroup host = decorView.findViewById(android.R.id.content);
        if (host == null && decorView instanceof ViewGroup) host = (ViewGroup) decorView;
        return show(host, steps, listener);
    }

    @Nullable
    private static FeatureTour show(
            @Nullable ViewGroup host,
            @NonNull List<HelpStep> steps,
            @Nullable Listener listener) {
        if (host == null) return null;

        List<HelpStep> visibleSteps = new ArrayList<>();
        for (HelpStep step : steps) {
            if (step != null && step.target.getVisibility() == View.VISIBLE) {
                visibleSteps.add(step);
            }
        }
        if (visibleSteps.isEmpty()) return null;

        FeatureTour tour = new FeatureTour(host, visibleSteps, listener);
        tour.attach();
        return tour;
    }

    private void attach() {
        if (ended) return;
        if (host.getWidth() <= 0 || host.getHeight() <= 0) {
            if (attachAttempts++ < 20) host.postDelayed(this::attach, 50);
            return;
        }
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        host.addView(overlay, layoutParams);
        overlay.bringToFront();
        overlay.getViewTreeObserver().addOnScrollChangedListener(treeScrollListener);
        renderStep();
    }

    private void renderStep() {
        if (ended || index < 0 || index >= steps.size()) return;
        currentStep = steps.get(index);

        progressTextView.setText(host.getContext().getString(
                R.string.help_step_progress, index + 1, steps.size()));
        titleTextView.setText(currentStep.title);
        messageTextView.setText(currentStep.message);

        boolean last = index == steps.size() - 1;
        if (currentStep.nextLabel != null && !currentStep.nextLabel.trim().isEmpty()) {
            nextButton.setText(currentStep.nextLabel);
        } else {
            nextButton.setText(last ? R.string.help_done : R.string.help_next);
        }
        nextButton.setContentDescription(nextButton.getText());
        backButton.setVisibility(index > 0 ? View.VISIBLE : View.GONE);
        adjustingTarget = false;
        spotlight.setHighlight(null);

        if (currentStep.target.getWidth() > 0) {
            currentStep.target.requestRectangleOnScreen(
                    new Rect(0, 0, currentStep.target.getWidth(),
                            currentStep.target.getHeight()),
                    true);
        }
        int renderedIndex = index;
        overlay.post(() -> ensureTargetInSafeArea(renderedIndex));
        overlay.postDelayed(() -> ensureTargetInSafeArea(renderedIndex), 180);
        overlay.postDelayed(() -> ensureTargetInSafeArea(renderedIndex), 420);
    }

    /**
     * Garante que o elemento fique acima da barra inferior e de botões flutuantes.
     * Sem isso, a rolagem pode posicionar o destaque atrás dos atalhos da tela.
     */
    private void ensureTargetInSafeArea(int renderedIndex) {
        if (ended || currentStep == null || renderedIndex != index) return;
        View target = currentStep.target;
        if (target.getWidth() <= 0 || !target.isShown()) return;

        int[] overlayLocation = new int[2];
        int[] targetLocation = new int[2];
        overlay.getLocationOnScreen(overlayLocation);
        target.getLocationOnScreen(targetLocation);
        RectF targetRect = new RectF(
                targetLocation[0] - overlayLocation[0],
                targetLocation[1] - overlayLocation[1],
                targetLocation[0] - overlayLocation[0] + target.getWidth(),
                targetLocation[1] - overlayLocation[1] + target.getHeight());

        int safeBottom = safeBottomFor(targetRect);
        int safeTop = dp(host.getContext(), 8);
        int delta = 0;
        if (targetRect.bottom > safeBottom) {
            delta = Math.round(targetRect.bottom - safeBottom);
        } else if (targetRect.top < safeTop) {
            delta = Math.round(targetRect.top - safeTop);
        }

        if (delta != 0) {
            View scrollable = findScrollableAncestor(target, delta > 0 ? 1 : -1);
            if (scrollable != null) {
                adjustingTarget = true;
                scrollable.scrollBy(0, delta);
                overlay.postDelayed(() -> {
                    if (ended || renderedIndex != index) return;
                    adjustingTarget = false;
                    positionBubble();
                }, 60);
                return;
            }
        }
        adjustingTarget = false;
        positionBubble();
    }

    /** Limite inferior visível, considerando a navegação e os botões flutuantes. */
    private int safeBottomFor(@NonNull RectF targetRect) {
        int margin = dp(host.getContext(), 8);
        int safeBottom = overlay.getHeight() - margin;
        int[] overlayLocation = new int[2];
        overlay.getLocationOnScreen(overlayLocation);

        int[] obstructionIds = {
                R.id.bottomNavigation,
                R.id.addTransactionFab,
                R.id.scrollToTopFab,
                R.id.addCategoryButton
        };
        for (int obstructionId : obstructionIds) {
            View obstruction = host.findViewById(obstructionId);
            if (obstruction == null || !obstruction.isShown()) continue;

            int[] obstructionLocation = new int[2];
            obstruction.getLocationOnScreen(obstructionLocation);
            int top = obstructionLocation[1] - overlayLocation[1];
            if (top <= 0 || top >= overlay.getHeight()) continue;

            int left = obstructionLocation[0] - overlayLocation[0];
            int right = left + obstruction.getWidth();
            boolean spansWidth = obstructionId == R.id.bottomNavigation;
            boolean horizontalOverlap = targetRect.right > left && targetRect.left < right;
            if (spansWidth || horizontalOverlap) {
                safeBottom = Math.min(safeBottom, top - margin);
            }
        }
        return Math.max(0, safeBottom);
    }

    @Nullable
    private View findScrollableAncestor(@NonNull View target, int direction) {
        ViewParent parent = target.getParent();
        while (parent != null) {
            if (parent instanceof View) {
                View view = (View) parent;
                if (view.canScrollVertically(direction)) return view;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void handleTreeScrollChanged() {
        if (ended || currentStep == null || adjustingTarget || positionPosted) return;
        positionPosted = true;
        overlay.post(() -> {
            positionPosted = false;
            if (!ended) positionBubble();
        });
    }

    private void positionBubble() {
        if (ended || currentStep == null || overlay.getParent() == null) return;
        int overlayWidth = overlay.getWidth();
        int overlayHeight = overlay.getHeight();
        if (overlayWidth <= 0 || overlayHeight <= 0) return;

        int[] overlayLocation = new int[2];
        int[] targetLocation = new int[2];
        overlay.getLocationOnScreen(overlayLocation);
        currentStep.target.getLocationOnScreen(targetLocation);

        float targetLeft = targetLocation[0] - overlayLocation[0];
        float targetTop = targetLocation[1] - overlayLocation[1];
        float targetRight = targetLeft + currentStep.target.getWidth();
        float targetBottom = targetTop + currentStep.target.getHeight();

        RectF highlight = new RectF(targetLeft, targetTop, targetRight, targetBottom);
        float padding = dp(host.getContext(), 8);
        highlight.inset(-padding, -padding);
        highlight.left = Math.max(0, highlight.left);
        highlight.top = Math.max(0, highlight.top);
        highlight.right = Math.min(overlayWidth, highlight.right);
        highlight.bottom = Math.min(overlayHeight, highlight.bottom);
        currentHighlight.set(highlight);
        spotlight.setHighlight(highlight);

        int margin = dp(host.getContext(), 14);
        int gap = dp(host.getContext(), 14);
        int maxBubbleWidth = Math.max(dp(host.getContext(), 220),
                Math.min(overlayWidth - margin * 2, dp(host.getContext(), 420)));

        bubble.measure(
                View.MeasureSpec.makeMeasureSpec(maxBubbleWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int bubbleWidth = bubble.getMeasuredWidth();
        int bubbleHeight = bubble.getMeasuredHeight();

        int below = Math.round(highlight.bottom) + gap;
        int above = Math.round(highlight.top) - gap - bubbleHeight;
        int bubbleTop;
        if (below + bubbleHeight <= overlayHeight - margin) {
            bubbleTop = below;
        } else if (above >= margin) {
            bubbleTop = above;
        } else {
            bubbleTop = clamp(below, margin, overlayHeight - margin - bubbleHeight);
        }
        int bubbleLeft = clamp(
                Math.round(highlight.centerX()) - bubbleWidth / 2,
                margin,
                overlayWidth - margin - bubbleWidth);

        ViewGroup.LayoutParams currentParams = bubble.getLayoutParams();
        if (currentParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) currentParams;
            params.width = bubbleWidth;
            params.height = bubbleHeight;
            params.leftMargin = bubbleLeft;
            params.topMargin = bubbleTop;
            bubble.setLayoutParams(params);
        } else {
            bubble.setX(bubbleLeft);
            bubble.setY(bubbleTop);
        }
    }

    private boolean handleOverlayTouch(View view, MotionEvent event) {
        if (currentStep == null) return true;
        float x = event.getX();
        float y = event.getY();

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            targetPressed = currentStep.allowTargetClick && currentHighlight.contains(x, y);
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            boolean shouldActivateTarget = targetPressed && currentHighlight.contains(x, y);
            targetPressed = false;
            if (shouldActivateTarget) {
                View target = currentStep.target;
                target.setPressed(true);
                target.postDelayed(() -> target.setPressed(false), 140);
                finish(RESULT_TARGET);
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            targetPressed = false;
        }
        return true;
    }

    private void finish(int result) {
        if (ended) return;
        ended = true;
        removeOverlay();
        if (listener == null) return;
        if (result == RESULT_COMPLETED) listener.onCompleted();
        else if (result == RESULT_SKIPPED) listener.onSkipped();
        else if (result == RESULT_TARGET) listener.onTargetClicked();
    }

    /** Indica se o overlay ainda está visível. */
    public boolean isShowing() {
        return !ended && overlay.getParent() != null;
    }

    /** Remove o guia sem disparar callbacks (usado ao fechar a tela). */
    public void dismiss() {
        if (ended) return;
        ended = true;
        removeOverlay();
    }

    private void removeOverlay() {
        ViewTreeObserver observer = overlay.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnScrollChangedListener(treeScrollListener);
        }
        ViewParent parent = overlay.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(overlay);
        }
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
