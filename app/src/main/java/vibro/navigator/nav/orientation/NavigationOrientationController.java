package vibro.navigator.nav.orientation;


import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.session.NavigationSession;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassOrientationCue;

public final class NavigationOrientationController {

    public interface CompassUiState {
        boolean shouldDispatchCompassUi();

        boolean hasStateListeners();

        void requestStateRefresh();
    }

    private static final String TAG = "NavigationOrientation";
    private static final long MIN_COMPASS_UI_UPDATE_INTERVAL_MS = 100L;

    private final Context context;
    private final Handler uiHandler;
    private final CompassUiState compassUiState;
    private final GeomagneticOrientationMonitor orientationMonitor;
    private final StationaryOrientationNotifier stationaryOrientationNotifier =
            new StationaryOrientationNotifier(new StationaryOrientationAdvisor());

    private long lastCompassUiUpdateElapsedRealtimeMs;
    private boolean monitoringActive;

    public NavigationOrientationController(
            @NonNull Context context,
            @NonNull Handler uiHandler,
            @NonNull CompassUiState compassUiState
    ) {
        this.context = context;
        this.uiHandler = uiHandler;
        this.compassUiState = compassUiState;
        orientationMonitor = new GeomagneticOrientationMonitor(context, sample -> onGeomagneticSampleUpdated());
    }

    public void start() {
        stationaryOrientationNotifier.reset();
        if (monitoringActive) {
            return;
        }
        if (!orientationMonitor.start()) {
            AppLogger.w(TAG, "Stationary orientation monitor unavailable, skipping stationary orientation notifications");
            return;
        }
        monitoringActive = true;
    }

    public void stop() {
        stationaryOrientationNotifier.reset();
        lastCompassUiUpdateElapsedRealtimeMs = 0L;
        if (!monitoringActive) {
            return;
        }
        monitoringActive = false;
        orientationMonitor.stop();
    }

    public void maybeSendStationaryOrientationNotification(
            @NonNull NavigationSession navigationSession,
            @Nullable NavigationForegroundController foregroundController
    ) {
        if (foregroundController == null) {
            return;
        }
        stationaryOrientationNotifier.maybeNotify(
                navigationSession.hasActiveRoute(),
                navigationSession.isRouteCalculationInProgress(),
                navigationSession.isLikelyStationaryForOrientation(),
                navigationSession.lastFilteredSpeedMps(),
                navigationSession.currentRouteBearingDegrees(),
                orientationMonitor.getLatestSample(),
                android.os.SystemClock.elapsedRealtime(),
                foregroundController::sendStationaryOrientationNotification
        );
    }

    @Nullable
    public CompassOrientationCue activeOrientationCue() {
        return stationaryOrientationNotifier.activeOrientationCue();
    }

    @Nullable
    public Double currentDisplayHeadingDegrees() {
        return NavigationDisplayHeading.headingDegrees(
                orientationMonitor.getLatestSample(),
                monitoringActive,
                android.os.SystemClock.elapsedRealtime(),
                currentDisplayRotation()
        );
    }

    @Nullable
    public Float currentDisplayHeadingAccuracyDegrees() {
        return NavigationDisplayHeading.headingAccuracyDegrees(
                orientationMonitor.getLatestSample(),
                monitoringActive,
                android.os.SystemClock.elapsedRealtime()
        );
    }

    public static boolean shouldDispatchCompassUi(
            boolean hasActiveRoute,
            boolean navigationUiVisible,
            boolean screenInteractive
    ) {
        return hasActiveRoute && navigationUiVisible && screenInteractive;
    }

    public static boolean shouldEvaluateStationaryOrientation(
            boolean hasActiveRoute,
            boolean routeCalculationInProgress
    ) {
        return StationaryOrientationNotifier.shouldEvaluate(hasActiveRoute, routeCalculationInProgress);
    }

    private void onGeomagneticSampleUpdated() {
        if (!compassUiState.shouldDispatchCompassUi() || !compassUiState.hasStateListeners()) {
            return;
        }
        long nowElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime();
        if (nowElapsedRealtimeMs - lastCompassUiUpdateElapsedRealtimeMs < MIN_COMPASS_UI_UPDATE_INTERVAL_MS) {
            return;
        }
        lastCompassUiUpdateElapsedRealtimeMs = nowElapsedRealtimeMs;
        uiHandler.post(compassUiState::requestStateRefresh);
    }

    private int currentDisplayRotation() {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null) {
            return Surface.ROTATION_0;
        }
        Display defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (defaultDisplay == null) {
            return Surface.ROTATION_0;
        }
        return defaultDisplay.getRotation();
    }
}
