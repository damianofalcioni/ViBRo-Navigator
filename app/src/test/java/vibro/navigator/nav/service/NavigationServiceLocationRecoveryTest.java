package vibro.navigator.nav.service;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.nav.location.NavigationGnssTracker;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationProvider;
import vibro.navigator.nav.location.NavigationLocationRecoveryAlarm;
import vibro.navigator.nav.location.NavigationLocationStallMonitor;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

import static org.junit.Assert.assertEquals;

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

    private static final class Fixture {
        private final MutableClock clock = new MutableClock();
        private final Provider provider = new Provider();
        private final Alarm alarm = new Alarm();
        private final NavigationLocationController controller;
        private final NavigationServiceLocationRecovery recovery;

        Fixture(boolean fused) {
            controller = new NavigationLocationController(provider, new Gnss(), new Fused(), () -> fused, clock);
            recovery = new NavigationServiceLocationRecovery(controller, new NavigationLocationStallMonitor(), alarm, clock);
            controller.requestLocationUpdates(3_000L);
            recovery.start();
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
        @Override public boolean schedule(long time) { deadlineMs = time; scheduleCalls++; return true; }
        @Override public void cancel() { deadlineMs = -1L; }
    }

    private static final class Provider implements NavigationLocationProvider {
        private long networkIntervalMs;
        private int requests;
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
        @Override public void requestCurrentLocationSeeds(boolean fine, boolean coarse) { }
        @Override public void requestSeedForEnabledProvider(String provider) { }
        @Override public void cancelPendingCurrentLocationRequests() { }
        @Override public void removeUpdates() { }
        @Override public String describeAvailability() { return "test"; }
    }

    private static final class Fused implements FusedLocationUpdateClient {
        @Override public boolean isAvailable() { return true; }
        @Override public void setUpdateFailureListener(Runnable listener) { }
        @Override public boolean requestUpdates(long interval, boolean fine, boolean coarse) { return true; }
        @Override public void requestCurrentLocationSeed(boolean fine, boolean coarse) { }
        @Override public void cancelCurrentLocationSeed() { }
        @Override public void removeUpdates() { }
        @Override public String describeAvailability() { return "test"; }
    }

    private static final class Gnss implements NavigationGnssTracker {
        @Override public Integer getFixedSatelliteCount() { return null; }
        @Override public void updateForRequestedProviders(List<String> providers) { }
        @Override public void setTrackingAllowed(boolean allowed) { }
        @Override public void reset() { }
    }
}
