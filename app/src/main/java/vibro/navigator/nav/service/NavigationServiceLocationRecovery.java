package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationRecoveryAlarm;
import vibro.navigator.nav.location.NavigationLocationStallMonitor;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class NavigationServiceLocationRecovery {
    private static final String TAG = "NavLocationRecovery";
    private static final long DEFAULT_UPDATE_INTERVAL_MS = NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS;

    @NonNull
    private final NavigationLocationController locationController;
    @NonNull
    private final NavigationLocationStallMonitor stallMonitor;
    @NonNull
    private final NavigationLocationRecoveryScheduler recoveryScheduler;
    @NonNull
    private final ElapsedRealtimeClock clock;

    NavigationServiceLocationRecovery(
            @NonNull NavigationLocationController locationController,
            @NonNull NavigationLocationStallMonitor stallMonitor,
            @NonNull NavigationLocationRecoveryAlarm alarm,
            @NonNull ElapsedRealtimeClock clock
    ) {
        this.locationController = locationController;
        this.stallMonitor = stallMonitor;
        recoveryScheduler = new NavigationLocationRecoveryScheduler(alarm);
        this.clock = clock;
    }

    void start() {
        stallMonitor.start(clock.elapsedRealtimeMs());
        scheduleNextCheck();
    }

    void onAcceptedFix() {
        stallMonitor.recordAcceptedFix(clock.elapsedRealtimeMs());
        scheduleNextCheck();
    }

    void onUpdateIntervalChanged() {
        scheduleNextCheck();
    }

    void recoverIfStalled() {
        recoveryScheduler.onAlarmTriggered();
        long nowMs = clock.elapsedRealtimeMs();
        long expectedIntervalMs = expectedIntervalMs();
        if (stallMonitor.shouldRecover(nowMs, expectedIntervalMs)) {
            AppLogger.w(TAG, "No location callback within requested interval; recovering location provider"
                    + " expectedIntervalMs=" + expectedIntervalMs);
            locationController.restartActiveLocationUpdates(DEFAULT_UPDATE_INTERVAL_MS);
            stallMonitor.recordRecoveryAttempt(nowMs);
        }
        scheduleNextCheck();
    }

    void stop() {
        recoveryScheduler.cancel();
        stallMonitor.reset();
    }

    private void scheduleNextCheck() {
        recoveryScheduler.schedule(stallMonitor.recoveryDeadlineMs(expectedIntervalMs()));
    }

    private long expectedIntervalMs() {
        return locationController.getLastRequestedLocationMinTimeMsOrDefault(DEFAULT_UPDATE_INTERVAL_MS);
    }
}
