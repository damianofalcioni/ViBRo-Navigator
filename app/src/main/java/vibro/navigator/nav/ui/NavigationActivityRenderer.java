package vibro.navigator.nav.ui;

import vibro.navigator.R;


import vibro.navigator.nav.orientation.NavigationCompassModeController;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private final Activity activity;
    private final TaskScheduler uiScheduler;
    private final NavigationCompassModeController compassModeController;
    private final View directionsBlock;
    private final TextView next;
    private final TextView afterNext;
    private final TextView destination;
    private final NavigationCompassSurfaces compassSurfaces;
    private final TextView gpsStatus;
    private final TextView speedLimit;
    private final NavigationBlockedRoadButton blockedRoadButton;
    private final NavigationCustomButtonUi customButtonUi;
    private final NavigationActionButtons actionButtons;
    private final NavigationDetailsDialogs detailsDialogs;
    private final Runnable compassTransitionTicker = this::renderCompassState;

    @Nullable
    private NavState currentState;
    @Nullable
    private NavigationServiceBinder currentBinder;
    private String lastRenderedStateKey = "";
    @Nullable
    private String lastGpsStatusText;
    @Nullable
    private Boolean lastGpsOverLimit;
    private long lastSpeedLimitKey;
    private boolean speedLimitRendered;
    private boolean customButtonRendered;

    NavigationActivityRenderer(
            @NonNull Activity activity,
            @NonNull TaskScheduler uiScheduler,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock,
            @NonNull Runnable afterSettingsLaunch
    ) {
        this.activity = activity;
        this.uiScheduler = uiScheduler;
        compassModeController = new NavigationCompassModeController(elapsedRealtimeClock);
        directionsBlock = activity.findViewById(R.id.turnInstructionRow);
        next = activity.findViewById(R.id.nextDirectionText);
        afterNext = activity.findViewById(R.id.afterNextDirectionText);
        destination = activity.findViewById(R.id.destinationText);
        gpsStatus = activity.findViewById(R.id.gpsStatusText);
        speedLimit = activity.findViewById(R.id.speedLimitText);
        blockedRoadButton = new NavigationBlockedRoadButton(activity);
        actionButtons = new NavigationActionButtons(activity, uiScheduler, afterSettingsLaunch);
        customButtonUi = new NavigationCustomButtonUi(activity, uiScheduler, new CustomButtonHost());
        compassSurfaces = new NavigationCompassSurfaces(activity, directionsBlock, destination);
        compassSurfaces.alignFullscreenCenterWith(actionButtons.settingsAnchor(), actionButtons.exportAnchor());
        detailsDialogs = new NavigationDetailsDialogs(activity, elapsedRealtimeClock);
        compassSurfaces.includeForegroundText(gpsStatus);
        NavigationActivityTextScaling.configure(next, afterNext, gpsStatus, speedLimit);
    }

    void configureControls(@NonNull Controls controls) {
        View.OnClickListener compassClickListener = v -> {
            compassModeController.onCompassTapped(
                    currentState == null ? null : currentState.routeStatus.compassState,
                    compassZoomAnimationEnabled()
            );
            renderCompassState();
        };
        compassSurfaces.setOnClickListener(compassClickListener);
        blockedRoadButton.setOnClickListener(v -> controls.onBlockedRoad());
        customButtonUi.configure();
        actionButtons.configure(new NavigationActionButtons.Listener() {
            @Override
            public void onStopNavigation() {
                controls.onStopNavigation();
            }

            @Override
            public void onTogglePaused() {
                controls.onTogglePaused();
            }

            @Override
            public void onExportRoute() {
                controls.onExportRoute();
            }
        });
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
        setTextIfChanged(next, state.routeStatus.guidance.nextLine);
        setTextIfChanged(afterNext, state.routeStatus.guidance.afterNextLine);
        setTextIfChanged(destination, state.routeStatus.displayStatusBlock());
        renderCompassState();
        renderSpeedLimitIfChanged(state);
        blockedRoadButton.render(state, navBinder);
        renderCustomButtonIfNeeded();
        actionButtons.render(state, navBinder);
        renderLiveDetails();
        logRenderedStateIfChanged(state);
    }

    void refreshSettings() {
        renderCompassState();
        customButtonUi.render();
        customButtonRendered = true;
    }

    boolean onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        return customButtonUi.onRequestPermissionsResult(requestCode, grantResults);
    }

    @NonNull
    NavigationCustomButtonUi customButtonUi() {
        return customButtonUi;
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
        boolean overLimit = NavigationActivityTextScaling.isOverSpeedLimit(currentState);
        if (TextUtils.equals(lastGpsStatusText, statusText)
                && lastGpsOverLimit != null
                && lastGpsOverLimit == overLimit) {
            return;
        }
        lastGpsStatusText = statusText;
        lastGpsOverLimit = overLimit;
        gpsStatus.setText(NavigationActivityTextScaling.styleGpsStatus(activity, statusText, currentState));
    }

    private void renderSpeedLimitIfChanged(@NonNull NavState state) {
        long speedLimitKey = NavigationActivityTextScaling.speedLimitRenderKey(state);
        if (speedLimitRendered && lastSpeedLimitKey == speedLimitKey) {
            return;
        }
        speedLimitRendered = true;
        lastSpeedLimitKey = speedLimitKey;
        NavigationActivityTextScaling.renderSpeedLimit(activity, speedLimit, state);
    }

    private void renderCustomButtonIfNeeded() {
        if (customButtonRendered) {
            return;
        }
        customButtonUi.render();
        customButtonRendered = true;
    }

    private static void setTextIfChanged(@NonNull TextView view, @NonNull CharSequence text) {
        if (!TextUtils.equals(view.getText(), text)) {
            view.setText(text);
        }
    }

    void cancelPendingCompassTransition() {
        uiScheduler.removeCallbacks(compassTransitionTicker);
    }

    void dismissDetailsDialogs() {
        detailsDialogs.dismissAll();
    }

    private void renderCompassState() {
        var compassState = currentState == null ? null : currentState.routeStatus.compassState;
        boolean fullscreenRouteMode = compassSurfaces.fullscreenRouteModeEnabled();
        boolean navigationPaused = currentState != null && currentState.pauseStatus.paused;
        var displayedCompassState = compassModeController.resolve(
                compassState,
                compassZoomAnimationEnabled()
        );
        compassSurfaces.render(fullscreenRouteMode, navigationPaused, displayedCompassState);
        if (currentBinder != null) {
            currentBinder.setCompassStreetViewport(displayedCompassState);
        }
        cancelPendingCompassTransition();
        if (compassModeController.isTransitionInProgress()) {
            uiScheduler.postDelayed(compassTransitionTicker, COMPASS_TRANSITION_FRAME_DELAY_MS);
        }
    }

    private boolean compassZoomAnimationEnabled() {
        return compassSurfaces.zoomAnimationEnabled();
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

    private final class CustomButtonHost implements NavigationCustomButtonController.Host {
        @Nullable
        @Override
        public NavigationServiceBinder currentBinder() {
            return currentBinder;
        }

        @Override
        public void refreshNavigationUiSettings() {
            refreshSettings();
        }
    }
}

