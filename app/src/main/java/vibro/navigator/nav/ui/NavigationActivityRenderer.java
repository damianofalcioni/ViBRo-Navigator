package vibro.navigator.nav.ui;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.compass.ui.NavigationCompassView;


import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.format.NavigationSpeedLimitFormatter;
import vibro.navigator.nav.orientation.NavigationCompassModeController;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.route.RouteSpeedLimit;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import android.app.Activity;
import android.graphics.PorterDuff;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;

final class NavigationActivityRenderer {

    interface Controls {
        void onBlockedRoad();

        void onStopNavigation();

        void onTogglePaused();

        void onExportRoute();
    }

    private static final String TAG = "NavigationActivity";
    private static final long COMPASS_TRANSITION_FRAME_DELAY_MS = 16L;
    private static final float BUTTON_ENABLED_ALPHA = 1f;
    private static final float BLOCKED_ROAD_DISABLED_ALPHA = 0.45f;
    private static final Pattern GPS_ACCURACY_HIGHLIGHT_PATTERN =
            Pattern.compile("• ([^ ]+ [^ ]+) ([^•]+) •");

    private final Activity activity;
    private final TaskScheduler uiScheduler;
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    private final NavigationCompassModeController compassModeController;
    private final TextView next;
    private final TextView afterNext;
    private final TextView destination;
    private final NavigationCompassView compass;
    private final TextView gpsStatus;
    private final TextView speedLimit;
    private final ImageButton blocked;
    private final ImageButton export;
    private final ImageButton pauseResume;
    private final ImageButton stop;
    private final Runnable compassTransitionTicker = this::renderCompassState;

    @Nullable
    private NavState currentState;
    private String lastRenderedStateKey = "";

    NavigationActivityRenderer(
            @NonNull Activity activity,
            @NonNull TaskScheduler uiScheduler,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.activity = activity;
        this.uiScheduler = uiScheduler;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        compassModeController = new NavigationCompassModeController(elapsedRealtimeClock);
        next = activity.findViewById(R.id.nextDirectionText);
        afterNext = activity.findViewById(R.id.afterNextDirectionText);
        destination = activity.findViewById(R.id.destinationText);
        compass = activity.findViewById(R.id.navigationCompassView);
        gpsStatus = activity.findViewById(R.id.gpsStatusText);
        speedLimit = activity.findViewById(R.id.speedLimitText);
        blocked = activity.findViewById(R.id.blockedRoadButton);
        export = activity.findViewById(R.id.exportRouteButton);
        pauseResume = activity.findViewById(R.id.pauseResumeNavButton);
        stop = activity.findViewById(R.id.stopNavButton);
        configureTextScaling();
    }

    void configureControls(@NonNull Controls controls) {
        compass.setOnClickListener(v -> {
            compassModeController.onCompassTapped(currentState == null ? null : currentState.routeStatus.compassState);
            renderCompassState();
        });
        blocked.setOnClickListener(v -> controls.onBlockedRoad());
        export.setOnClickListener(v -> controls.onExportRoute());
        stop.setOnClickListener(v -> controls.onStopNavigation());
        pauseResume.setOnClickListener(v -> controls.onTogglePaused());
    }

    void render(@NonNull NavState state, @Nullable NavigationServiceBinder navBinder) {
        currentState = state;
        next.setText(state.routeStatus.guidance.nextLine);
        afterNext.setText(state.routeStatus.guidance.afterNextLine);
        destination.setText(state.routeStatus.displayStatusBlock());
        renderCompassState();
        renderSpeedLimit(state.routeStatus.speedLimit);
        renderBlockedRoadButton(state, navBinder);
        export.setEnabled(navBinder != null);
        pauseResume.setEnabled(navBinder != null);
        pauseResume.setImageResource(state.pauseStatus.paused ? R.drawable.ic_play : R.drawable.ic_pause);
        pauseResume.setContentDescription(activity.getString(
                state.pauseStatus.paused ? R.string.action_resume_navigation : R.string.action_pause_navigation
        ));
        renderGpsStatus();
        logRenderedStateIfChanged(state);
    }

    private void renderBlockedRoadButton(@NonNull NavState state, @Nullable NavigationServiceBinder navBinder) {
        boolean enabled = navBinder != null
                && state.routeStatus.blockedRoadActionAvailable
                && !state.pauseStatus.paused;
        blocked.setEnabled(enabled);
        blocked.setAlpha(enabled ? BUTTON_ENABLED_ALPHA : BLOCKED_ROAD_DISABLED_ALPHA);
        if (enabled) {
            blocked.clearColorFilter();
            return;
        }
        blocked.setColorFilter(
                AndroidAppTheme.color(activity, R.attr.vibroTextSecondaryColor),
                PorterDuff.Mode.SRC_IN
        );
    }

    void renderGpsStatus() {
        String statusText;
        if (currentState == null) {
            statusText = activity.getString(
                    R.string.format_nav_gps_status_with_countdown,
                    NavStateComposer.waiting(activity).gpsStatus.statusLine,
                    activity.getString(R.string.nav_status_unavailable)
            );
            gpsStatus.setText(statusText);
            return;
        }
        String nextEvaluationValue = activity.getString(R.string.nav_status_unavailable);
        long nextEvaluationDeadlineElapsedMs = currentState.gpsStatus.nextEvaluationDeadlineElapsedMs;
        long remainingMs = Math.max(0L, nextEvaluationDeadlineElapsedMs - elapsedRealtimeClock.elapsedRealtimeMs());
        if (nextEvaluationDeadlineElapsedMs != NavState.NO_DEADLINE && remainingMs > 0L) {
            long remainingSeconds = (long) Math.ceil(remainingMs / 1000.0);
            nextEvaluationValue = activity.getString(R.string.format_nav_next_position_check_value, remainingSeconds);
        }
        statusText = activity.getString(
                R.string.format_nav_gps_status_with_countdown,
                currentState.gpsStatus.statusLine,
                nextEvaluationValue
        );
        gpsStatus.setText(styleGpsStatus(statusText));
    }

    void cancelPendingCompassTransition() {
        uiScheduler.removeCallbacks(compassTransitionTicker);
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
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                speedLimit,
                10,
                16,
                1,
                TypedValue.COMPLEX_UNIT_SP
        );
    }

    private void renderCompassState() {
        @Nullable NavCompassState compassState = currentState == null ? null : currentState.routeStatus.compassState;
        compass.setNavigationPaused(currentState != null && currentState.pauseStatus.paused);
        compass.setCompassState(compassModeController.resolve(compassState));
        cancelPendingCompassTransition();
        if (compassModeController.isTransitionInProgress()) {
            uiScheduler.postDelayed(compassTransitionTicker, COMPASS_TRANSITION_FRAME_DELAY_MS);
        }
    }

    private void renderSpeedLimit(@Nullable RouteSpeedLimit routeSpeedLimit) {
        if (routeSpeedLimit == null) {
            speedLimit.setVisibility(View.GONE);
            speedLimit.setText("");
            speedLimit.setContentDescription(null);
            return;
        }
        speedLimit.setText(NavigationSpeedLimitFormatter.formatBadge(routeSpeedLimit));
        speedLimit.setContentDescription(
                NavigationSpeedLimitFormatter.formatContentDescription(activity, routeSpeedLimit)
        );
        speedLimit.setVisibility(View.VISIBLE);
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
        String stateKey = state.routeStatus.guidance.nextLine + "|" + state.routeStatus.guidance.afterNextLine
                + "|" + state.gpsStatus.statusLine
                + "|" + state.gpsStatus.nextEvaluationDeadlineElapsedMs
                + "|" + state.routeStatus.progress.destinationLine
                + "|" + state.routeStatus.progress.stopProgressBlock
                + "|" + state.routeStatus.progress.detailBlock
                + "|" + state.pauseStatus.paused
                + "|" + formatLogSpeedLimit(state.routeStatus.speedLimit)
                + "|" + (state.routeStatus.compassState == null ? "no-compass"
                : state.routeStatus.compassState.routePoints.size());
        if (stateKey.equals(lastRenderedStateKey)) {
            return;
        }
        lastRenderedStateKey = stateKey;
        AppLogger.d(TAG, "Rendered state next=" + state.routeStatus.guidance.nextLine
                + " afterNext=" + state.routeStatus.guidance.afterNextLine
                + " gpsStatus=" + state.gpsStatus.statusLine
                + " nextEvalDeadline=" + state.gpsStatus.nextEvaluationDeadlineElapsedMs
                + " destination=" + state.routeStatus.progress.destinationLine
                + " stops=" + state.routeStatus.progress.stopProgressBlock
                + " paused=" + state.pauseStatus.paused
                + " speedLimit=" + formatLogSpeedLimit(state.routeStatus.speedLimit)
                + " compass=" + (state.routeStatus.compassState == null ? "none"
                : ("points=" + state.routeStatus.compassState.routePoints.size()
                + " heading=" + state.routeStatus.compassState.displayMode.headingDegrees))
                + " detail=" + state.routeStatus.progress.detailBlock);
    }

    @NonNull
    private static String formatLogSpeedLimit(@Nullable RouteSpeedLimit routeSpeedLimit) {
        return routeSpeedLimit == null ? "none" : routeSpeedLimit.value + " " + routeSpeedLimit.unit;
    }
}

