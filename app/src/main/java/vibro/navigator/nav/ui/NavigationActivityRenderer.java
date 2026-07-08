package vibro.navigator.nav.ui;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.compass.ui.NavigationCompassView;


import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.format.NavigationSpeedLimitFormatter;
import vibro.navigator.nav.orientation.NavigationCompassModeController;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.PorterDuff;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

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

    private final Activity activity;
    private final TaskScheduler uiScheduler;
    private final NavigationCompassModeController compassModeController;
    private final View directionsBlock;
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
    private final NavigationDetailsDialogs detailsDialogs;
    private final Runnable compassTransitionTicker = this::renderCompassState;

    @Nullable
    private NavState currentState;
    @Nullable
    private NavigationServiceBinder currentBinder;
    private String lastRenderedStateKey = "";

    NavigationActivityRenderer(
            @NonNull Activity activity,
            @NonNull TaskScheduler uiScheduler,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.activity = activity;
        this.uiScheduler = uiScheduler;
        compassModeController = new NavigationCompassModeController(elapsedRealtimeClock);
        directionsBlock = activity.findViewById(R.id.turnInstructionRow);
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
        detailsDialogs = new NavigationDetailsDialogs(activity, elapsedRealtimeClock);
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
        gpsStatus.setClickable(true);
        gpsStatus.setFocusable(true);
        gpsStatus.setOnClickListener(v -> detailsDialogs.showGps(currentState));
        View.OnClickListener directionsClickListener = v -> detailsDialogs.showDirections(currentBinder);
        directionsBlock.setClickable(true);
        directionsBlock.setFocusable(true);
        directionsBlock.setOnClickListener(directionsClickListener);
        next.setClickable(true);
        next.setFocusable(true);
        next.setOnClickListener(directionsClickListener);
        afterNext.setClickable(true);
        afterNext.setFocusable(true);
        afterNext.setOnClickListener(directionsClickListener);
        destination.setClickable(true);
        destination.setFocusable(true);
        destination.setOnClickListener(v -> detailsDialogs.showTripStats(currentState));
    }

    void render(@NonNull NavState state, @Nullable NavigationServiceBinder navBinder) {
        currentState = state;
        currentBinder = navBinder;
        next.setText(state.routeStatus.guidance.nextLine);
        afterNext.setText(state.routeStatus.guidance.afterNextLine);
        destination.setText(state.routeStatus.displayStatusBlock());
        renderCompassState();
        renderSpeedLimit(state);
        renderBlockedRoadButton(state, navBinder);
        export.setEnabled(navBinder != null);
        pauseResume.setEnabled(navBinder != null);
        pauseResume.setImageResource(state.pauseStatus.paused ? R.drawable.ic_play : R.drawable.ic_pause);
        pauseResume.setContentDescription(activity.getString(
                state.pauseStatus.paused ? R.string.action_resume_navigation : R.string.action_pause_navigation
        ));
        renderLiveDetails();
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

    void renderLiveDetails() {
        renderGpsStatus();
        detailsDialogs.updateLiveDetails(currentState, currentBinder);
    }

    private void renderGpsStatus() {
        String nextEvaluationValue = detailsDialogs.nextGpsEvaluationValue(currentState);
        String statusText = activity.getString(
                R.string.format_nav_gps_status_with_countdown,
                detailsDialogs.gpsStatusLine(currentState),
                nextEvaluationValue
        );
        gpsStatus.setText(styleGpsStatus(statusText, currentState));
    }

    void cancelPendingCompassTransition() {
        uiScheduler.removeCallbacks(compassTransitionTicker);
    }

    void dismissDetailsDialogs() {
        detailsDialogs.dismissAll();
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
                10,
                18,
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
        NavCompassState displayedCompassState = compassModeController.resolve(compassState);
        compass.setCompassState(displayedCompassState);
        if (currentBinder != null) {
            currentBinder.setCompassStreetViewport(displayedCompassState);
        }
        cancelPendingCompassTransition();
        if (compassModeController.isTransitionInProgress()) {
            uiScheduler.postDelayed(compassTransitionTicker, COMPASS_TRANSITION_FRAME_DELAY_MS);
        }
    }

    private void renderSpeedLimit(@NonNull NavState state) {
        if (state.routeStatus.speedLimit == null) {
            speedLimit.setVisibility(View.GONE);
            speedLimit.setText("");
            speedLimit.setContentDescription(null);
            return;
        }
        speedLimit.setText(NavigationSpeedLimitFormatter.formatBadge(state.routeStatus.speedLimit));
        speedLimit.setContentDescription(
                NavigationSpeedLimitFormatter.formatContentDescription(activity, state.routeStatus.speedLimit)
        );
        speedLimit.setVisibility(View.VISIBLE);
    }

    @NonNull
    private CharSequence styleGpsStatus(@NonNull String statusText, @Nullable NavState state) {
        SpannableString styledText = new SpannableString(statusText);
        if (state == null) {
            return styledText;
        }
        styleAccuracy(styledText, statusText, state);
        if (!NavigationSpeedLimitFormatter.isOverLimit(
                state.gpsStatus.telemetry.speedMps,
                state.routeStatus.speedLimit
        )) {
            return styledText;
        }
        int speedStart = statusText.indexOf(state.gpsStatus.telemetry.speedText);
        if (speedStart < 0) {
            return styledText;
        }
        int speedEnd = speedStart + state.gpsStatus.telemetry.speedText.length();
        styledText.setSpan(
                new StyleSpan(Typeface.BOLD),
                speedStart,
                speedEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return styledText;
    }

    private void styleAccuracy(
            @NonNull SpannableString styledText,
            @NonNull String statusText,
            @NonNull NavState state
    ) {
        int accuracyStart = accuracyStart(statusText, state);
        if (accuracyStart < 0) {
            return;
        }
        int accuracyEnd = accuracyStart + state.gpsStatus.telemetry.accuracyText.length();
        styledText.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(activity, R.color.compass_accent)),
                accuracyStart,
                accuracyEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private int accuracyStart(@NonNull String statusText, @NonNull NavState state) {
        int compactStart = statusText.indexOf(state.gpsStatus.statusLine);
        if (compactStart < 0) {
            return -1;
        }
        int elevationStart = state.gpsStatus.statusLine.indexOf(state.gpsStatus.telemetry.elevationText);
        if (elevationStart < 0) {
            return -1;
        }
        int accuracyStart = state.gpsStatus.statusLine.indexOf(
                state.gpsStatus.telemetry.accuracyText,
                elevationStart + state.gpsStatus.telemetry.elevationText.length()
        );
        return accuracyStart < 0 ? -1 : compactStart + accuracyStart;
    }

    private void logRenderedStateIfChanged(@NonNull NavState state) {
        String stateKey = state.routeStatus.guidance.nextLine + "|" + state.routeStatus.guidance.afterNextLine
                + "|" + state.gpsStatus.statusLine
                + "|" + state.gpsStatus.nextEvaluationDeadlineElapsedMs
                + "|" + state.routeStatus.progress.destinationLine
                + "|" + state.routeStatus.progress.stopProgressBlock
                + "|" + state.routeStatus.progress.detailBlock
                + "|" + state.pauseStatus.paused
                + "|" + formatLogSpeedLimit(state)
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
                + " speedLimit=" + formatLogSpeedLimit(state)
                + " compass=" + (state.routeStatus.compassState == null ? "none"
                : ("points=" + state.routeStatus.compassState.routePoints.size()
                + " heading=" + state.routeStatus.compassState.displayMode.headingDegrees))
                + " detail=" + state.routeStatus.progress.detailBlock);
    }

    @NonNull
    private static String formatLogSpeedLimit(@NonNull NavState state) {
        return state.routeStatus.speedLimit == null
                ? "none"
                : state.routeStatus.speedLimit.value + " " + state.routeStatus.speedLimit.unit;
    }
}

