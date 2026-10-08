package vibro.navigator.nav.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.settings.AppNavigationHintSettings;

/** A small startup guide, positioned independently of the compass layout. */
final class NavigationGestureHints {
    private static final String STATE_DISMISSED = "navigation_gesture_hints_dismissed";
    private static final String STATE_DISMISS_AT = "navigation_gesture_hints_dismiss_at";
    private static final long AUTO_DISMISS_DELAY_MS = 5000L;
    private static final long COUNTDOWN_TICK_MS = 1000L;
    private final TaskScheduler scheduler;
    private final ElapsedRealtimeClock clock;
    private final Runnable countdownTick = this::updateCountdown;
    private final TextView dismissText;
    private final FrameLayout content;
    private final FrameLayout overlay;
    private final View card;
    private final View compass;
    private final View fullscreenCompass;
    private final int[] contentLocation = new int[2];
    private final int[] compassLocation = new int[2];
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::position;
    private boolean dismissed = true;
    private long dismissAtElapsedMs;

    NavigationGestureHints(Activity activity, @Nullable Bundle savedState,
                           TaskScheduler scheduler, ElapsedRealtimeClock clock) {
        this.scheduler = scheduler;
        this.clock = clock;
        content = activity.findViewById(android.R.id.content);
        compass = activity.findViewById(R.id.navigationCompassView);
        fullscreenCompass = activity.findViewById(R.id.navigationFullscreenCompassView);
        overlay = new FrameLayout(activity);
        overlay.setId(R.id.navigationGestureHintsOverlay);
        overlay.setVisibility(View.GONE);
        card = activity.getLayoutInflater().inflate(R.layout.navigation_gesture_hints, overlay, false);
        overlay.addView(card);
        dismissText = card.findViewById(R.id.dismissGestureHints);
        dismissText.setOnClickListener(view -> dismiss());
        content.addView(overlay, new FrameLayout.LayoutParams(0, 0));
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
        position();
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

    void saveState(Bundle state) {
        state.putBoolean(STATE_DISMISSED, dismissed);
        state.putLong(STATE_DISMISS_AT, dismissAtElapsedMs);
    }

    private void position() {
        boolean fullscreen = fullscreenCompass.getVisibility() == View.VISIBLE;
        View anchor = fullscreen ? content : compass;
        content.getLocationInWindow(contentLocation);
        anchor.getLocationInWindow(compassLocation);
        int left = compassLocation[0] - contentLocation[0];
        int top = compassLocation[1] - contentLocation[1];
        int width = anchor.getWidth();
        int height = anchor.getHeight();
        int start = startMargin(left, width);
        FrameLayout.LayoutParams bounds = (FrameLayout.LayoutParams) overlay.getLayoutParams();
        if (bounds.width != width || bounds.height != height
                || bounds.getMarginStart() != start || bounds.topMargin != top) {
            bounds.width = width;
            bounds.height = height;
            bounds.setMarginStart(start);
            bounds.topMargin = top;
            bounds.gravity = Gravity.TOP | Gravity.START;
            overlay.setLayoutParams(bounds);
        }
        int cardWidth = Math.max(0, Math.min(dp(320), width - dp(24)));
        FrameLayout.LayoutParams cardBounds = (FrameLayout.LayoutParams) card.getLayoutParams();
        if (cardBounds.width != cardWidth) {
            cardBounds.width = cardWidth;
            card.setLayoutParams(cardBounds);
        }
    }

    private int startMargin(int left, int width) {
        return content.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                ? content.getWidth() - left - width : left;
    }

    private int dp(int value) {
        return Math.round(value * content.getResources().getDisplayMetrics().density);
    }
}
