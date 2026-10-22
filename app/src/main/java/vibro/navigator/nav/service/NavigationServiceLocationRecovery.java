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
    private boolean running;
    private boolean checking;

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
        locationController.acquisition().setStateListener(this::scheduleNextCheck);
    }

    void start() {
        running = true;
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
        if (!running) {
            return;
        }
        recoveryScheduler.onAlarmTriggered();
        checking = true;
        try {
            locationController.acquisition().checkHealth();
            recoverStreamIfStalled();
        } finally {
            checking = false;
            scheduleNextCheck();
        }
    }

    private void recoverStreamIfStalled() {
        long nowMs = clock.elapsedRealtimeMs();
        long expectedIntervalMs = expectedIntervalMs();
        if (stallMonitor.shouldRecover(nowMs, expectedIntervalMs)) {
            AppLogger.w(TAG, "No location callback within requested interval; recovering location provider"
                    + " expectedIntervalMs=" + expectedIntervalMs);
            locationController.restartActiveLocationUpdates(DEFAULT_UPDATE_INTERVAL_MS);
            stallMonitor.recordRecoveryAttempt(nowMs);
        }
    }

    void stop() {
        running = false;
        recoveryScheduler.cancel();
        stallMonitor.reset();
    }

    private void scheduleNextCheck() {
        if (!running || checking) {
            return;
        }
        long stallDeadline = stallMonitor.recoveryDeadlineMs(expectedIntervalMs());
        long acquisitionDeadline = locationController.acquisition().recoveryDeadlineMs();
        recoveryScheduler.schedule(acquisitionDeadline < 0L ? stallDeadline : Math.min(stallDeadline, acquisitionDeadline));
    }

    private long expectedIntervalMs() {
        return locationController.getLastRequestedLocationMinTimeMsOrDefault(DEFAULT_UPDATE_INTERVAL_MS);
    }
}
