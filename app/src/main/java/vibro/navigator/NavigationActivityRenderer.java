package vibro.navigator;


import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.orientation.NavigationCompassModeController;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.nav.model.NavState;
import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vibro.navigator.util.AppLogger;

final class NavigationActivityRenderer {

    interface Controls {
        void onBlockedRoad();

        void onStopNavigation();

        void onTogglePaused();
    }

    private static final String TAG = "NavigationActivity";
    private static final long COMPASS_TRANSITION_FRAME_DELAY_MS = 16L;
    private static final Pattern GPS_ACCURACY_HIGHLIGHT_PATTERN =
            Pattern.compile("• ([^ ]+ [^ ]+) ([^•]+) •");

    private final Activity activity;
    private final Handler uiHandler;
    private final NavigationCompassModeController compassModeController = new NavigationCompassModeController();
    private final TextView next;
    private final TextView afterNext;
    private final TextView destination;
    private final NavigationCompassView compass;
    private final TextView gpsStatus;
    private final ImageButton blocked;
    private final ImageButton pauseResume;
    private final ImageButton stop;
    private final Runnable compassTransitionTicker = this::renderCompassState;

    @Nullable
    private NavState currentState;
    private String lastRenderedStateKey = "";

    NavigationActivityRenderer(@NonNull Activity activity, @NonNull Handler uiHandler) {
        this.activity = activity;
        this.uiHandler = uiHandler;
        next = activity.findViewById(R.id.nextDirectionText);
        afterNext = activity.findViewById(R.id.afterNextDirectionText);
        destination = activity.findViewById(R.id.destinationText);
        compass = activity.findViewById(R.id.navigationCompassView);
        gpsStatus = activity.findViewById(R.id.gpsStatusText);
        blocked = activity.findViewById(R.id.blockedRoadButton);
        pauseResume = activity.findViewById(R.id.pauseResumeNavButton);
        stop = activity.findViewById(R.id.stopNavButton);
        configureTextScaling();
    }

    void configureControls(@NonNull Controls controls) {
        compass.setOnClickListener(v -> {
            compassModeController.onCompassTapped(currentState == null ? null : currentState.compassState);
            renderCompassState();
        });
        blocked.setOnClickListener(v -> controls.onBlockedRoad());
        stop.setOnClickListener(v -> controls.onStopNavigation());
        pauseResume.setOnClickListener(v -> controls.onTogglePaused());
    }

    void render(@NonNull NavState state, @Nullable NavigationServiceBinder navBinder) {
        currentState = state;
        next.setText(state.nextLine);
        afterNext.setText(state.afterNextLine);
        destination.setText(state.displayStatusBlock());
        renderCompassState();
        blocked.setEnabled(!state.paused);
        pauseResume.setEnabled(navBinder != null);
        pauseResume.setImageResource(state.paused ? R.drawable.ic_play : R.drawable.ic_pause);
        pauseResume.setContentDescription(activity.getString(
                state.paused ? R.string.action_resume_navigation : R.string.action_pause_navigation
        ));
        renderGpsStatus();
        logRenderedStateIfChanged(state);
    }

    void renderGpsStatus() {
        String statusText;
        if (currentState == null) {
            statusText = activity.getString(
                    R.string.format_nav_gps_status_with_countdown,
                    NavState.waiting(activity).gpsStatusLine,
                    activity.getString(R.string.nav_status_unavailable)
            );
            gpsStatus.setText(statusText);
            return;
        }
        String nextEvaluationValue = activity.getString(R.string.nav_status_unavailable);
        long remainingMs = Math.max(0L, currentState.nextEvaluationDeadlineElapsedMs - SystemClock.elapsedRealtime());
        if (currentState.nextEvaluationDeadlineElapsedMs != NavState.NO_DEADLINE && remainingMs > 0L) {
            long remainingSeconds = (long) Math.ceil(remainingMs / 1000.0);
            nextEvaluationValue = activity.getString(R.string.format_nav_next_position_check_value, remainingSeconds);
        }
        statusText = activity.getString(
                R.string.format_nav_gps_status_with_countdown,
                currentState.gpsStatusLine,
                nextEvaluationValue
        );
        gpsStatus.setText(styleGpsStatus(statusText));
    }

    void cancelPendingCompassTransition() {
        uiHandler.removeCallbacks(compassTransitionTicker);
    }

    private void configureTextScaling() {
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                next,
                10,
                22,
                1,
                TypedValue.COMPLEX_UNIT_SP
        );
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                afterNext,
                12,
                18,
                1,
                TypedValue.COMPLEX_UNIT_SP
        );
        gpsStatus.setMaxLines(1);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                gpsStatus,
                8,
                16,
                1,
                TypedValue.COMPLEX_UNIT_SP
        );
    }

    private void renderCompassState() {
        @Nullable NavCompassState compassState = currentState == null ? null : currentState.compassState;
        compass.setCompassState(compassModeController.resolve(compassState));
        cancelPendingCompassTransition();
        if (compassModeController.isTransitionInProgress()) {
            uiHandler.postDelayed(compassTransitionTicker, COMPASS_TRANSITION_FRAME_DELAY_MS);
        }
    }

    @NonNull
    private CharSequence styleGpsStatus(@NonNull String statusText) {
        SpannableString styledText = new SpannableString(statusText);
        Matcher matcher = GPS_ACCURACY_HIGHLIGHT_PATTERN.matcher(statusText);
        if (!matcher.find()) {
            return styledText;
        }
        String unavailable = activity.getString(R.string.nav_status_unavailable);
        int accentColor = ContextCompat.getColor(activity, R.color.compass_accent);
        if (!unavailable.equals(matcher.group(1).trim())) {
            styledText.setSpan(
                    new ForegroundColorSpan(accentColor),
                    matcher.start(1),
                    matcher.end(1),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        if (!unavailable.equals(matcher.group(2).trim())) {
            styledText.setSpan(
                    new ForegroundColorSpan(accentColor),
                    matcher.start(2),
                    matcher.end(2),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        return styledText;
    }

    private void logRenderedStateIfChanged(@NonNull NavState state) {
        String stateKey = state.nextLine + "|" + state.afterNextLine + "|" + state.gpsStatusLine
                + "|" + state.nextEvaluationDeadlineElapsedMs + "|" + state.destinationLine
                + "|" + state.stopProgressBlock
                + "|" + state.detailBlock
                + "|" + state.paused
                + "|" + (state.compassState == null ? "no-compass"
                : state.compassState.routePoints.size() + ":" + state.compassState.headingDegrees);
        if (stateKey.equals(lastRenderedStateKey)) {
            return;
        }
        lastRenderedStateKey = stateKey;
        AppLogger.d(TAG, "Rendered state next=" + state.nextLine
                + " afterNext=" + state.afterNextLine
                + " gpsStatus=" + state.gpsStatusLine
                + " nextEvalDeadline=" + state.nextEvaluationDeadlineElapsedMs
                + " destination=" + state.destinationLine
                + " stops=" + state.stopProgressBlock
                + " paused=" + state.paused
                + " compass=" + (state.compassState == null ? "none"
                : ("points=" + state.compassState.routePoints.size() + " heading=" + state.compassState.headingDegrees))
                + " detail=" + state.detailBlock);
    }
}
