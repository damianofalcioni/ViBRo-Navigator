package vibro.navigator.nav.service;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.nav.location.NavigationGnssTracker;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationProvider;
import vibro.navigator.nav.location.NavigationLocationRecoveryAlarm;
import vibro.navigator.nav.location.NavigationLocationStallMonitor;
import vibro.navigator.nav.location.NavigationMotionTrigger;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationServiceLocationRecoveryTest {
    private static final String GPS_PROVIDER = "gps";
    private static final String NETWORK_PROVIDER = "network";
    @Test
    public void networkAcceptanceCannotPostponeGpsSilenceAlarm() {
        Fixture fixture = new Fixture(false);
        fixture.stableGps();
        assertEquals(30_000L, fixture.provider.networkIntervalMs);
        fixture.clock.nowMs = 20_000L;
        fixture.accept(NETWORK_PROVIDER);
        assertEquals(26_000L, fixture.alarm.deadlineMs);
        fixture.clock.nowMs = 26_000L;
        fixture.recovery.recoverIfStalled();
        assertEquals(3_000L, fixture.provider.networkIntervalMs);
        assertEquals(50_000L, fixture.alarm.deadlineMs);
    }

    @Test
    public void silenceCheckRestoresNetworkWithoutAnyCallbackAndStopsCleanly() {
        Fixture fixture = new Fixture(false);
        fixture.stableGps();
        fixture.clock.nowMs = 26_000L;
        fixture.recovery.recoverIfStalled();
        assertEquals(3_000L, fixture.provider.networkIntervalMs);
        assertEquals(41_000L, fixture.alarm.deadlineMs);
        fixture.recovery.stop();
        fixture.controller.stopTracking();
        fixture.clock.nowMs = 60_000L;
        fixture.recovery.recoverIfStalled();
        assertEquals(-1L, fixture.alarm.deadlineMs);
    }

    @Test
    public void fusedProgressUsesStreamStallDeadlineWithoutGpsHealthAlarm() {
        Fixture fixture = new Fixture(true);
        fixture.clock.nowMs = 11_000L;
        fixture.accept("fused");
        assertEquals(41_000L, fixture.alarm.deadlineMs);
        assertEquals(0, fixture.provider.requests);
        int scheduledBefore = fixture.alarm.scheduleCalls;
        fixture.clock.nowMs = 41_000L;
        fixture.recovery.recoverIfStalled();
        assertEquals(1, fixture.provider.requests);
        assertEquals(3_000L, fixture.provider.networkIntervalMs);
        assertEquals(scheduledBefore + 1, fixture.alarm.scheduleCalls);
    }

    @Test
    public void legacySleepRemovesRequestsAndDoesNotTriggerStallRecovery() {
        Fixture fixture = new Fixture(false);
        fixture.enableSleep();
        fixture.stationaryStop();
        assertTrue(fixture.controller.isStationarySuspended());
        assertEquals("none", fixture.controller.activeProviderSummary());
        assertEquals(52_000L, fixture.alarm.deadlineMs);
        int requests = fixture.provider.requests;
        fixture.clock.nowMs = 40_000L;
        fixture.recovery.recoverIfStalled();
        assertEquals(requests, fixture.provider.requests);
        assertEquals(52_000L, fixture.alarm.deadlineMs);
        fixture.controller.requestLocationUpdates(3_000L);
        fixture.controller.requestCurrentLocationSeeds();
        assertEquals(requests, fixture.provider.requests);
        assertEquals(0, fixture.provider.seeds);
    }

    @Test
    public void fusedSleepCancelsRequestsAndMotionImmediatelyRestoresFused() {
        Fixture fixture = new Fixture(true);
        fixture.enableSleep();
        fixture.stationaryStop();
        assertTrue(fixture.controller.isStationarySuspended());
        assertFalse(fixture.fused.active);
        assertTrue(fixture.fused.seedCancellations > 0);
        fixture.clock.nowMs = 25_000L;
        fixture.motion.fire();
        assertFalse(fixture.controller.isStationarySuspended());
        assertTrue(fixture.fused.active);
        assertEquals(1_000L, fixture.controller.getLastRequestedLocationMinTimeMsOrDefault(-1L));
        assertEquals(1, fixture.departureResets);
        assertEquals(25_000L, fixture.sleepEndedAtMs);
        assertEquals(55_000L, fixture.alarm.deadlineMs);
    }

    @Test
    public void fallbackCheckReacquiresAndVerifiesBeforeSleepingAgain() {
        Fixture fixture = new Fixture(false);
        fixture.enableSleep();
        fixture.stationaryStop();
        fixture.clock.nowMs = 52_000L;
        fixture.recovery.recoverIfStalled();
        assertFalse(fixture.controller.isStationarySuspended());
        assertEquals(52_000L, fixture.sleepEndedAtMs);
        assertEquals(1_000L, fixture.provider.networkIntervalMs);
        fixture.acceptStationary(52_000L);
        fixture.acceptStationary(54_000L);
        assertFalse(fixture.controller.isStationarySuspended());
        fixture.acceptStationary(56_000L);
        assertTrue(fixture.controller.isStationarySuspended());
        assertEquals(86_000L, fixture.alarm.deadlineMs);
    }

    @Test
    public void alarmFailureOrMissingSensorKeepsAcquisitionRunning() {
        Fixture fixture = new Fixture(false);
        fixture.enableSleep();
        fixture.alarm.available = false;
        fixture.stationaryStop();
        assertFalse(fixture.controller.isStationarySuspended());
        fixture.alarm.available = true;
        fixture.motion.available = false;
        fixture.stationaryStop();
        assertFalse(fixture.controller.isStationarySuspended());
        assertEquals(GPS_PROVIDER + "+" + NETWORK_PROVIDER, fixture.controller.activeProviderSummary());
    }

    @Test
    public void pauseCancelsTriggerAndFallbackAndCannotRestartTracking() {
        Fixture fixture = new Fixture(false);
        fixture.enableSleep();
        fixture.stationaryStop();
        Runnable delayedCheck = fixture.scheduler.pending;
        fixture.recovery.stop();
        fixture.controller.stopTracking();
        assertEquals(null, fixture.motion.callback);
        assertEquals(null, fixture.scheduler.pending);
        int requests = fixture.provider.requests;
        fixture.clock.nowMs = 90_000L;
        delayedCheck.run();
        assertEquals(requests, fixture.provider.requests);
        assertEquals(-1L, fixture.alarm.deadlineMs);
    }

    @Test
    public void settingsOrRouteChangeWakesSleepAndRestartsDepartureMonitoring() {
        Fixture fixture = new Fixture(false);
        fixture.enableSleep();
        fixture.stationaryStop();
        fixture.recovery.wakeForNavigationChange();
        assertFalse(fixture.controller.isStationarySuspended());
        assertEquals(1, fixture.departureResets);
        assertEquals(null, fixture.scheduler.pending);
        assertEquals(null, fixture.motion.callback);
    }

    @Test
    public void stoppingSleepReportsItsEndOnceWithoutRestartingAcquisition() {
        Fixture fixture = new Fixture(false);
        fixture.enableSleep();
        fixture.stationaryStop();
        int requests = fixture.provider.requests;
        fixture.clock.nowMs = 40_000L;
        fixture.recovery.stop();
        fixture.recovery.stop();
        assertEquals(40_000L, fixture.sleepEndedAtMs);
        assertEquals(1, fixture.departureResets);
        assertEquals(requests, fixture.provider.requests);
    }

    @Test
    public void tenSecondFusedFixesWithoutNativeSpeedCanSleepVerifyAndWakeOnMotion() {
        Fixture fixture = new Fixture(true);
        fixture.enableSleep();
        for (long time : new long[] {1_000L, 11_100L, 21_200L, 31_300L}) {
            fixture.acceptSpeedlessFused(time);
        }
        assertTrue(fixture.controller.isStationarySuspended());
        assertFalse(fixture.fused.active);
        assertEquals(61_300L, fixture.alarm.deadlineMs);
        fixture.clock.nowMs = 61_300L;
        fixture.recovery.recoverIfStalled();
        assertTrue(fixture.fused.active);
        fixture.acceptSpeedlessFused(61_300L);
        fixture.acceptSpeedlessFused(71_400L);
        assertFalse(fixture.controller.isStationarySuspended());
        fixture.acceptSpeedlessFused(81_500L);
        assertTrue(fixture.controller.isStationarySuspended());
        assertEquals(111_500L, fixture.alarm.deadlineMs);
        fixture.clock.nowMs = 82_000L;
        fixture.motion.fire();
        assertFalse(fixture.controller.isStationarySuspended());
        assertTrue(fixture.fused.active);
        assertEquals(1_000L, fixture.controller.getLastRequestedLocationMinTimeMsOrDefault(-1L));
        assertEquals(2, fixture.departureResets);
    }

    private static final class Fixture {
        private final MutableClock clock = new MutableClock();
        private final Provider provider = new Provider();
        private final Alarm alarm = new Alarm();
        private final Fused fused = new Fused();
        private final Motion motion = new Motion();
        private final Scheduler scheduler = new Scheduler();
        private int departureResets;
        private long sleepEndedAtMs = -1L;
        private final NavigationLocationController controller;
        private final NavigationServiceLocationRecovery recovery;

        Fixture(boolean fused) {
            controller = new NavigationLocationController(provider, new Gnss(), this.fused, () -> fused, clock);
            recovery = new NavigationServiceLocationRecovery(controller, new NavigationLocationStallMonitor(), alarm, clock);
            controller.requestLocationUpdates(3_000L);
            recovery.start();
        }

        void enableSleep() {
            recovery.enableStationarySleep(motion, scheduler, nowMs -> {
                departureResets++;
                sleepEndedAtMs = nowMs;
            });
        }

        void stationaryStop() {
            for (long time = 1_000L; time <= 22_000L; time += 3_000L) {
                acceptStationary(time);
            }
        }

        void acceptStationary(long time) {
            clock.nowMs = time;
            NavigationLocation location = new NavigationLocation(GPS_PROVIDER);
            location.setTime(time, time);
            location.setAccuracy(5f);
            location.setSpeed(0f);
            recovery.onAcceptedFix(location, location, true);
        }

        void acceptSpeedlessFused(long time) {
            clock.nowMs = time;
            NavigationLocation location = new NavigationLocation("fused");
            location.setTime(time, time);
            location.setAccuracy(10f);
            recovery.onAcceptedFix(location, location, true);
        }

        void stableGps() {
            for (long time : new long[] {1_000L, 6_000L, 11_000L}) {
                clock.nowMs = time;
                accept(GPS_PROVIDER);
            }
        }

        void accept(String providerName) {
            NavigationLocation location = new NavigationLocation(providerName);
            location.setTime(clock.nowMs, clock.nowMs);
            location.setAccuracy(20f);
            controller.acquisition().recordRawLocation(location);
            recovery.onAcceptedFix();
        }
    }

    private static final class MutableClock implements ElapsedRealtimeClock {
        private long nowMs = 1_000L;
        @Override public long elapsedRealtimeMs() { return nowMs; }
    }

    private static final class Alarm implements NavigationLocationRecoveryAlarm {
        private long deadlineMs = -1L;
        private int scheduleCalls;
        private boolean available = true;
        @Override public boolean schedule(long time) {
            scheduleCalls++;
            if (available) {
                deadlineMs = time;
            }
            return available;
        }
        @Override public void cancel() { deadlineMs = -1L; }
    }

    private static final class Provider implements NavigationLocationProvider {
        private long networkIntervalMs;
        private int requests;
        private int seeds;
        @Override public boolean hasFineLocationPermission() { return true; }
        @Override public boolean hasCoarseLocationPermission() { return true; }
        @Override public List<String> enabledPermittedProviders(boolean fine, boolean coarse) {
            return Arrays.asList(GPS_PROVIDER, NETWORK_PROVIDER);
        }
        @Override public List<String> requestProviderUpdates(List<String> providers, long interval) {
            return requestProviderUpdates(providers, interval, interval);
        }
        @Override public List<String> requestProviderUpdates(List<String> providers, long interval, long networkInterval) {
            networkIntervalMs = networkInterval;
            requests++;
            return providers;
        }
        @Override public NavigationLocation getLastKnownLocationQuietly(String provider) { return null; }
        @Override public void requestCurrentLocationSeeds(boolean fine, boolean coarse) { seeds++; }
        @Override public void requestSeedForEnabledProvider(String provider) { }
        @Override public void cancelPendingCurrentLocationRequests() { }
        @Override public void removeUpdates() { }
        @Override public String describeAvailability() { return "test"; }
    }

    private static final class Fused implements FusedLocationUpdateClient {
        private boolean active;
        private int seedCancellations;
        @Override public boolean isAvailable() { return true; }
        @Override public void setUpdateFailureListener(Runnable listener) { }
        @Override public boolean requestUpdates(long interval, boolean fine, boolean coarse) { active = true; return true; }
        @Override public void requestCurrentLocationSeed(boolean fine, boolean coarse) { }
        @Override public void cancelCurrentLocationSeed() { seedCancellations++; }
        @Override public void removeUpdates() { active = false; }
        @Override public String describeAvailability() { return "test"; }
    }

    private static final class Motion implements NavigationMotionTrigger {
        private boolean available = true;
        private Runnable callback;
        @Override public boolean arm(Runnable onMotion) { callback = onMotion; return available; }
        @Override public void cancel() { callback = null; }
        void fire() { callback.run(); }
    }

    private static final class Scheduler implements TaskScheduler {
        private Runnable pending;
        @Override public void post(Runnable runnable) { runnable.run(); }
        @Override public void postDelayed(Runnable runnable, long delayMs) { pending = runnable; }
        @Override public void removeCallbacks(Runnable runnable) { if (runnable.equals(pending)) { pending = null; } }
    }

    private static final class Gnss implements NavigationGnssTracker {
        @Override public Integer getFixedSatelliteCount() { return null; }
        @Override public void updateForRequestedProviders(List<String> providers) { }
        @Override public void setTrackingAllowed(boolean allowed) { }
        @Override public void reset() { }
    }
}
