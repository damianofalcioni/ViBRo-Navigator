package vibro.navigator.nav.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationRecoveryAlarm;
import vibro.navigator.nav.location.NavigationLocationStallMonitor;
import vibro.navigator.nav.location.NavigationMotionTrigger;
import vibro.navigator.nav.location.NavigationStationarySleep;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class NavigationServiceLocationRecovery {
    interface StationarySleepListener {
        void onSleepEnded(long nowMs);
    }

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
    @Nullable private NavigationStationarySleep stationarySleep;
    private TaskScheduler scheduler = runnable -> { };
    private StationarySleepListener stationarySleepEnded = nowMs -> { };
    private final Runnable sleepCheck = () -> recoverIfStalled("awake_timer");

    NavigationServiceLocationRecovery(
            @NonNull NavigationLocationController locationController,
            @NonNull NavigationLocationStallMonitor stallMonitor,
            @NonNull NavigationLocationRecoveryAlarm alarm,
            @NonNull ElapsedRealtimeClock clock
    ) {
        this(locationController, stallMonitor, alarm, clock,
                message -> AppLogger.d(TAG, message));
    }

    NavigationServiceLocationRecovery(NavigationLocationController locationController,
            NavigationLocationStallMonitor stallMonitor, NavigationLocationRecoveryAlarm alarm,
            ElapsedRealtimeClock clock, NavigationLocationRecoveryScheduler.Diagnostics diagnostics) {
        this.locationController = locationController;
        this.stallMonitor = stallMonitor;
        recoveryScheduler = new NavigationLocationRecoveryScheduler(alarm, diagnostics);
        this.clock = clock;
        locationController.acquisition().setStateListener(this::scheduleNextCheck);
    }

    void enableStationarySleep(NavigationMotionTrigger trigger, TaskScheduler scheduler,
            StationarySleepListener stationarySleepEnded) {
        stationarySleep = new NavigationStationarySleep(trigger, this::onMotion);
        this.scheduler = scheduler;
        this.stationarySleepEnded = stationarySleepEnded;
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

    void onAcceptedFix(NavigationLocation raw, NavigationLocation filtered, boolean eligible) {
        onAcceptedFix();
        NavigationStationarySleep sleep = stationarySleep;
        if (running && sleep != null && sleep.observe(raw, filtered, eligible, clock.elapsedRealtimeMs())) {
            enterStationarySleep(sleep);
        }
    }

    private void enterStationarySleep(NavigationStationarySleep sleep) {
        checking = true;
        try {
            sleep.beginSleep(clock.elapsedRealtimeMs());
            if (recoveryScheduler.schedule(sleep.deadlineMs())) {
                locationController.suspendForStationarySleep(sleep.deadlineMs());
                scheduler.removeCallbacks(sleepCheck);
                scheduler.postDelayed(sleepCheck, NavigationStationarySleep.CHECK_INTERVAL_MS);
                AppLogger.i(TAG, "Stationary sleep armed; location suspended until motion or fallback check");
            } else {
                AppLogger.w(TAG, "Stationary sleep blocked reason=fallback_alarm_unavailable");
                sleep.reset();
            }
        } finally {
            checking = false;
            scheduleNextCheck();
        }
    }

    private void onMotion() {
        if (!running) {
            return;
        }
        recoveryScheduler.onCheckDispatched("motion_trigger", clock.elapsedRealtimeMs(), clock.uptimeMs());
        wakeForNavigationChange();
        locationController.requestFastLocationUpdates();
        AppLogger.i(TAG, "Motion detected; resuming rapid departure acquisition source=motion_trigger");
    }

    void wakeForNavigationChange() {
        cancelSleepCheck();
        if (stationarySleep != null) {
            stationarySleep.reset();
        }
        if (running && locationController.isStationarySuspended()) {
            stationarySleepEnded.onSleepEnded(clock.elapsedRealtimeMs());
            stallMonitor.start(clock.elapsedRealtimeMs());
            locationController.resumeAfterStationarySleep();
        }
        scheduleNextCheck();
    }

    void onUpdateIntervalChanged() {
        scheduleNextCheck();
    }

    void recoverIfStalled() {
        recoverIfStalled("alarm");
    }

    private void recoverIfStalled(String source) {
        if (!running) {
            return;
        }
        recoveryScheduler.onCheckDispatched(source, clock.elapsedRealtimeMs(), clock.uptimeMs());
        checking = true;
        try {
            if (stationarySleep != null && stationarySleep.isSleeping()) {
                checkStationarySleep(stationarySleep, source);
            } else {
                locationController.acquisition().checkHealth();
                recoverStreamIfStalled(source);
            }
        } finally {
            checking = false;
            scheduleNextCheck();
        }
    }

    private void checkStationarySleep(NavigationStationarySleep sleep, String source) {
        if (clock.elapsedRealtimeMs() < sleep.deadlineMs()) {
            return;
        }
        cancelSleepCheck();
        sleep.beginCheck();
        stationarySleepEnded.onSleepEnded(clock.elapsedRealtimeMs());
        stallMonitor.start(clock.elapsedRealtimeMs());
        locationController.resumeAfterStationarySleep();
        AppLogger.i(TAG, "Stationary fallback check; verifying fresh fixes before sleeping again source=" + source);
    }

    private void recoverStreamIfStalled(String source) {
        long nowMs = clock.elapsedRealtimeMs();
        long expectedIntervalMs = expectedIntervalMs();
        if (stallMonitor.shouldRecover(nowMs, expectedIntervalMs)) {
            AppLogger.w(TAG, "No location callback within requested interval; recovering location provider"
                    + " expectedIntervalMs=" + expectedIntervalMs + " source=" + source);
            locationController.restartActiveLocationUpdates(DEFAULT_UPDATE_INTERVAL_MS);
            stallMonitor.recordRecoveryAttempt(nowMs);
        }
    }

    void stop() {
        if (running && locationController.isStationarySuspended()) {
            stationarySleepEnded.onSleepEnded(clock.elapsedRealtimeMs());
        }
        running = false;
        cancelSleepCheck();
        if (stationarySleep != null) {
            stationarySleep.reset();
        }
        recoveryScheduler.cancel();
        stallMonitor.reset();
    }

    private void cancelSleepCheck() {
        scheduler.removeCallbacks(sleepCheck);
    }

    private void scheduleNextCheck() {
        if (!running || checking) {
            return;
        }
        if (stationarySleep != null && stationarySleep.isSleeping()) {
            recoveryScheduler.schedule(stationarySleep.deadlineMs());
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
