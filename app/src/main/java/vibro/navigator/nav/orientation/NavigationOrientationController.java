package vibro.navigator.nav.orientation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

public final class NavigationOrientationController {

    public interface HeadingMonitorFactory {
        @NonNull
        NavigationHeadingMonitor create(@NonNull GeomagneticOrientationMonitor.Callback callback);
    }

    public interface UiDispatcher {
        void post(@NonNull Runnable runnable);
    }

    public interface CompassUiState {
        boolean shouldDispatchCompassUi();

        boolean hasStateListeners();

        void requestStateRefresh();
    }

    private static final String TAG = "NavigationOrientation";
    private static final long MIN_COMPASS_UI_UPDATE_INTERVAL_MS = 100L;

    private final CompassUiState compassUiState;
    private final NavigationHeadingMonitor orientationMonitor;
    private final DisplayRotationProvider displayRotationProvider;
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    private final UiDispatcher uiDispatcher;
    private final StationaryOrientationNotifier stationaryOrientationNotifier =
            new StationaryOrientationNotifier(new StationaryOrientationAdvisor());

    private long lastCompassUiUpdateElapsedRealtimeMs;
    private boolean monitoringActive;
    @Nullable
    private NavigationSession latestNavigationSession;
    @Nullable
    private NavigationForegroundController latestForegroundController;

    public NavigationOrientationController(
            @NonNull HeadingMonitorFactory headingMonitorFactory,
            @NonNull DisplayRotationProvider displayRotationProvider,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock,
            @NonNull UiDispatcher uiDispatcher,
            @NonNull CompassUiState compassUiState
    ) {
        this.compassUiState = compassUiState;
        this.displayRotationProvider = displayRotationProvider;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        this.uiDispatcher = uiDispatcher;
        orientationMonitor = headingMonitorFactory.create(sample -> onGeomagneticSampleUpdated());
    }

    public void start() {
        stationaryOrientationNotifier.reset();
        latestNavigationSession = null;
        latestForegroundController = null;
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
        latestNavigationSession = null;
        latestForegroundController = null;
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
        latestNavigationSession = navigationSession;
        latestForegroundController = foregroundController;
        if (foregroundController == null) {
            return;
        }
        evaluateStationaryOrientation(navigationSession, foregroundController);
    }

    private void evaluateLatestStationaryOrientation() {
        NavigationSession navigationSession = latestNavigationSession;
        NavigationForegroundController foregroundController = latestForegroundController;
        if (navigationSession == null || foregroundController == null) {
            return;
        }
        evaluateStationaryOrientation(navigationSession, foregroundController);
    }

    private void evaluateStationaryOrientation(
            @NonNull NavigationSession navigationSession,
            @NonNull NavigationForegroundController foregroundController
    ) {
        boolean hasActiveRoute = navigationSession.hasActiveRoute();
        boolean routeCalculationInProgress = navigationSession.isRouteCalculationInProgress();
        if (!shouldEvaluateStationaryOrientation(hasActiveRoute, routeCalculationInProgress)) {
            stationaryOrientationNotifier.reset();
            return;
        }
        boolean likelyStationary = navigationSession.isLikelyStationaryForOrientation();
        if (!likelyStationary) {
            stationaryOrientationNotifier.reset();
            return;
        }
        stationaryOrientationNotifier.maybeNotify(
                hasActiveRoute,
                routeCalculationInProgress,
                likelyStationary,
                navigationSession.lastFilteredSpeedMps(),
                navigationSession.currentRouteBearingDegrees(),
                orientationMonitor.getLatestSample(),
                elapsedRealtimeClock.elapsedRealtimeMs(),
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
                elapsedRealtimeClock.elapsedRealtimeMs(),
                currentDisplayRotation()
        );
    }

    @Nullable
    public Float currentDisplayHeadingAccuracyDegrees() {
        return NavigationDisplayHeading.headingAccuracyDegrees(
                orientationMonitor.getLatestSample(),
                monitoringActive,
                elapsedRealtimeClock.elapsedRealtimeMs()
        );
    }

    public static boolean shouldDispatchCompassUi(
            boolean hasActiveRoute,
            boolean navigationDisplayActive
    ) {
        return hasActiveRoute && navigationDisplayActive;
    }

    public static boolean shouldDispatchCompassUi(
            boolean hasActiveRoute,
            boolean navigationUiVisible,
            boolean screenInteractive
    ) {
        return shouldDispatchCompassUi(hasActiveRoute, navigationUiVisible && screenInteractive);
    }

    public static boolean shouldEvaluateStationaryOrientation(
            boolean hasActiveRoute,
            boolean routeCalculationInProgress
    ) {
        return StationaryOrientationNotifier.shouldEvaluate(hasActiveRoute, routeCalculationInProgress);
    }

    private void onGeomagneticSampleUpdated() {
        evaluateLatestStationaryOrientation();
        if (!compassUiState.shouldDispatchCompassUi() || !compassUiState.hasStateListeners()) {
            return;
        }
        long nowElapsedRealtimeMs = elapsedRealtimeClock.elapsedRealtimeMs();
        if (nowElapsedRealtimeMs - lastCompassUiUpdateElapsedRealtimeMs < MIN_COMPASS_UI_UPDATE_INTERVAL_MS) {
            return;
        }
        lastCompassUiUpdateElapsedRealtimeMs = nowElapsedRealtimeMs;
        uiDispatcher.post(compassUiState::requestStateRefresh);
    }

    private int currentDisplayRotation() {
        return displayRotationProvider.currentDisplayRotation();
    }
}
