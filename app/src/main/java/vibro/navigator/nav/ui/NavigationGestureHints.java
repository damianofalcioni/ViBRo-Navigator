package vibro.navigator.nav.ui;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.settings.AppNavigationHintSettings;

/** A centered startup guide with all tips and street colors visible together. */
final class NavigationGestureHints {
    private static final String STATE_DISMISSED = "navigation_gesture_hints_dismissed";
    private static final String STATE_DISMISS_AT = "navigation_gesture_hints_dismiss_at";
    private static final long AUTO_DISMISS_DELAY_MS = 10000L;
    private static final long COUNTDOWN_TICK_MS = 1000L;
    private final TaskScheduler scheduler;
    private final ElapsedRealtimeClock clock;
    private final Runnable countdownTick = this::updateCountdown;
    private final TextView dismissText;
    private final FrameLayout content;
    private final FrameLayout overlay;
    private final View card;
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::sizeCard;
    private final View dontShowAgain;
    private final Rect actionBounds = new Rect();
    private final int[] actionLocation = new int[2];
    private boolean dismissed = true;
    private long dismissAtElapsedMs;

    NavigationGestureHints(Activity activity, @Nullable Bundle savedState,
                           TaskScheduler scheduler, ElapsedRealtimeClock clock) {
        this.scheduler = scheduler;
        this.clock = clock;
        content = activity.findViewById(android.R.id.content);
        overlay = new FrameLayout(activity);
        overlay.setId(R.id.navigationGestureHintsOverlay);
        overlay.setBackgroundColor(ColorUtils.setAlphaComponent(
                AndroidAppTheme.color(activity, R.attr.vibroSurfaceColor), 220));
        overlay.setVisibility(View.GONE);
        card = activity.getLayoutInflater().inflate(R.layout.navigation_gesture_hints, overlay, false);
        overlay.addView(card);
        dismissText = card.findViewById(R.id.dismissGestureHints);
        dismissText.setOnClickListener(view -> dismiss());
        dontShowAgain = card.findViewById(R.id.dontShowGestureHintsAgain);
        dontShowAgain.setOnClickListener(view -> {
            AppNavigationHintSettings.setEnabled(activity, false);
            dismiss();
        });
        content.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        if (savedState == null || !savedState.getBoolean(STATE_DISMISSED)) {
            long freshDeadline = clock.elapsedRealtimeMs() + AUTO_DISMISS_DELAY_MS;
            showUntil(savedState == null ? freshDeadline : savedState.getLong(STATE_DISMISS_AT, freshDeadline));
        }
    }

    void show() {
        dismiss();
        showUntil(clock.elapsedRealtimeMs() + AUTO_DISMISS_DELAY_MS);
    }

    private void showUntil(long deadline) {
        if (!AppNavigationHintSettings.isEnabled(content.getContext())) {
            return;
        }
        long now = clock.elapsedRealtimeMs();
        long remainingMs = Math.min(AUTO_DISMISS_DELAY_MS, deadline - now);
        if (remainingMs <= 0L) {
            return;
        }
        dismissAtElapsedMs = now + remainingMs;
        dismissed = false;
        overlay.setVisibility(View.VISIBLE);
        content.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        sizeCard();
        updateCountdown();
    }

    private void updateCountdown() {
        long remainingMs = dismissAtElapsedMs - clock.elapsedRealtimeMs();
        if (remainingMs <= 0L) {
            dismiss();
            return;
        }
        int secondsRemaining = (int) ((remainingMs + COUNTDOWN_TICK_MS - 1L) / COUNTDOWN_TICK_MS);
        dismissText.setText(content.getResources().getQuantityString(
                R.plurals.nav_gesture_dismiss_countdown, secondsRemaining, secondsRemaining));
        scheduler.postDelayed(countdownTick,
                remainingMs - (secondsRemaining - 1L) * COUNTDOWN_TICK_MS);
    }

    void refreshCountdown() {
        if (!AppNavigationHintSettings.isEnabled(content.getContext())) {
            dismiss();
            return;
        }
        if (dismissed) {
            return;
        }
        scheduler.removeCallbacks(countdownTick);
        updateCountdown();
    }

    void dismiss() {
        if (dismissed) {
            return;
        }
        dismissed = true;
        dismissAtElapsedMs = 0L;
        scheduler.removeCallbacks(countdownTick);
        overlay.setVisibility(View.GONE);
        content.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
    }

    void dismissOnTouch(float x, float y) {
        if (dismissed) {
            return;
        }
        dontShowAgain.getLocationInWindow(actionLocation);
        actionBounds.set(actionLocation[0], actionLocation[1],
                actionLocation[0] + dontShowAgain.getWidth(), actionLocation[1] + dontShowAgain.getHeight());
        if (!actionBounds.contains((int) x, (int) y)) {
            dismiss();
        }
    }

    void saveState(Bundle state) {
        state.putBoolean(STATE_DISMISSED, dismissed);
        state.putLong(STATE_DISMISS_AT, dismissAtElapsedMs);
    }

    private void sizeCard() {
        float density = content.getResources().getDisplayMetrics().density;
        int maxWidth = Math.round((content.getWidth() > content.getHeight() ? 600 : 320) * density);
        int width = Math.max(0, Math.min(maxWidth, content.getWidth() - Math.round(24 * density)));
        FrameLayout.LayoutParams bounds = (FrameLayout.LayoutParams) card.getLayoutParams();
        if (bounds.width != width) {
            bounds.width = width;
            card.setLayoutParams(bounds);
        }
    }
}
