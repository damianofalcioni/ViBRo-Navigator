package vibro.navigator.nav.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.nav.time.ElapsedRealtimeClock;

public class NavigationLocationControllerProviderModeTest {
    private static final String GPS_PROVIDER = "gps";
    private static final String NETWORK_PROVIDER = "network";

    @Test
    public void legacyStableGpsThrottlesOnlyNetworkAndRecoversWithoutCallbacks() {
        Fixture fixture = new Fixture(false, true, GPS_PROVIDER, NETWORK_PROVIDER);
        fixture.controller.requestLocationUpdates(3_000L);
        recordGoodGps(fixture);
        assertEquals(3_000L, fixture.provider.requestedMinTimeMs);
        assertEquals(30_000L, fixture.provider.networkMinTimeMs);
        assertEquals(30_000L, fixture.controller.acquisition().recoveryDeadlineMs());
        fixture.clock.nowMs = 30_000L;
        fixture.controller.acquisition().checkHealth();
        assertEquals(3_000L, fixture.provider.networkMinTimeMs);
        assertEquals(-1L, fixture.controller.acquisition().recoveryDeadlineMs());
    }

    @Test
    public void healthyFusedStreamNeverActivatesLegacyHealthChecks() {
        Fixture fixture = new Fixture(true, GPS_PROVIDER, NETWORK_PROVIDER);
        fixture.controller.requestLocationUpdates(3_000L);
        recordGoodGps(fixture);
        fixture.clock.nowMs = 100_000L;
        fixture.controller.acquisition().checkHealth();
        assertTrue(fixture.provider.requestedProviders.isEmpty());
        assertEquals(-1L, fixture.controller.acquisition().recoveryDeadlineMs());
        assertEquals(1, fixture.fused.requestUpdatesCount);
    }

    @Test
    public void slowCadenceAndGpsOnlyModeDoNotAddNetworkRecoveryWakeups() {
        Fixture slow = new Fixture(false, true, GPS_PROVIDER, NETWORK_PROVIDER);
        slow.controller.requestLocationUpdates(60_000L);
        recordGoodGps(slow);
        assertEquals(60_000L, slow.provider.networkMinTimeMs);
        assertEquals(-1L, slow.controller.acquisition().recoveryDeadlineMs());
        Fixture gpsOnly = new Fixture(false, true, GPS_PROVIDER);
        gpsOnly.controller.requestLocationUpdates(3_000L);
        recordGoodGps(gpsOnly);
        assertEquals(-1L, gpsOnly.controller.acquisition().recoveryDeadlineMs());
    }

    @Test
    public void fallbackRecoveryAndResumeResetGnssHealth() {
        Fixture fixture = new Fixture(true, GPS_PROVIDER, NETWORK_PROVIDER);
        fixture.controller.requestLocationUpdates(3_000L);
        fixture.fused.failActiveRequest();
        recordGoodGps(fixture);
        assertEquals(30_000L, fixture.provider.networkMinTimeMs);
        fixture.controller.restartActiveLocationUpdates(3_000L);
        assertEquals(3_000L, fixture.provider.networkMinTimeMs);
        recordGoodGps(fixture);
        fixture.controller.stopTracking();
        fixture.controller.requestLocationUpdates(3_000L);
        assertEquals(3_000L, fixture.provider.networkMinTimeMs);
    }

    private static void recordGoodGps(Fixture fixture) {
        long startMs = fixture.clock.nowMs;
        for (long offset : new long[] {0L, 5_000L, 10_000L}) {
            long time = startMs + offset;
            fixture.clock.nowMs = time;
            NavigationLocation gps = new NavigationLocation(GPS_PROVIDER);
            gps.setTime(time, time);
            gps.setAccuracy(20f);
            fixture.controller.acquisition().recordRawLocation(gps);
        }
    }

    @Test
    public void fusedPreference_keepsLegacyProvidersDormant() {
        Fixture fixture = new Fixture(true, GPS_PROVIDER, NETWORK_PROVIDER);

        fixture.controller.requestLocationUpdates(60_000L);

        assertEquals(1, fixture.fused.requestUpdatesCount);
        assertTrue(fixture.provider.requestedProviders.isEmpty());
        assertEquals(65_000L, fixture.controller.getNextEvaluationDeadlineElapsedMs());
    }

    @Test
    public void fusedStall_activatesLegacyFallbackAtSameInterval() {
        Fixture fixture = new Fixture(true, GPS_PROVIDER, NETWORK_PROVIDER);

        fixture.controller.requestLocationUpdates(60_000L);
        fixture.controller.restartActiveLocationUpdates(3_000L);

        assertEquals(1, fixture.fused.requestUpdatesCount);
        assertEquals(Arrays.asList(GPS_PROVIDER, NETWORK_PROVIDER), fixture.provider.requestedProviders);
        assertEquals(60_000L, fixture.provider.requestedMinTimeMs);
    }

    @Test
    public void fusedImmediateFailure_keepsLegacyFallbackAcrossCadenceChanges() {
        Fixture fixture = new Fixture(false, GPS_PROVIDER);

        fixture.controller.requestLocationUpdates(10_000L);
        fixture.controller.requestLocationUpdates(20_000L);

        assertEquals(1, fixture.fused.requestUpdatesCount);
        assertEquals(2, fixture.provider.requestProviderUpdatesCount);
        assertEquals(Collections.singletonList(GPS_PROVIDER), fixture.provider.requestedProviders);
    }

    @Test
    public void fusedAsynchronousFailure_activatesLegacyFallbackImmediately() {
        Fixture fixture = new Fixture(true, GPS_PROVIDER);

        fixture.controller.requestLocationUpdates(20_000L);
        fixture.fused.failActiveRequest();

        assertEquals(Collections.singletonList(GPS_PROVIDER), fixture.provider.requestedProviders);
        assertEquals(20_000L, fixture.provider.requestedMinTimeMs);
    }

    private static final class Fixture {
        private final FakeLocationProvider provider;
        private final FakeFusedLocationUpdateClient fused;
        private final NavigationLocationController controller;
        private final FixedClock clock = new FixedClock();

        Fixture(boolean fusedRequestSucceeds, @NonNull String... enabledProviders) {
            this(true, fusedRequestSucceeds, enabledProviders);
        }

        Fixture(boolean useFused, boolean fusedRequestSucceeds, @NonNull String... enabledProviders) {
            provider = new FakeLocationProvider(enabledProviders);
            fused = new FakeFusedLocationUpdateClient(fusedRequestSucceeds);
            controller = new NavigationLocationController(
                    provider,
                    new FakeGnssTracker(),
                    fused,
                    () -> useFused,
                    clock
            );
        }
    }

    private static final class FixedClock implements ElapsedRealtimeClock {
        private long nowMs = 5_000L;
        @Override
        public long elapsedRealtimeMs() {
            return nowMs;
        }
    }

    private static final class FakeLocationProvider implements NavigationLocationProvider {
        @NonNull
        private final List<String> enabledProviders;
        @NonNull
        private List<String> requestedProviders = Collections.emptyList();
        private int requestProviderUpdatesCount;
        private long requestedMinTimeMs;
        private long networkMinTimeMs;

        @Override
        public List<String> requestProviderUpdates(@NonNull List<String> providers, long minTimeMs, long networkIntervalMs) {
            networkMinTimeMs = networkIntervalMs;
            return requestProviderUpdates(providers, minTimeMs);
        }

        FakeLocationProvider(@NonNull String... enabledProviders) {
            this.enabledProviders = Arrays.asList(enabledProviders);
        }

        @Override
        public boolean hasFineLocationPermission() {
            return true;
        }

        @Override
        public boolean hasCoarseLocationPermission() {
            return true;
        }

        @NonNull
        @Override
        public List<String> enabledPermittedProviders(boolean fineGranted, boolean coarseGranted) {
            return enabledProviders;
        }

        @NonNull
        @Override
        public List<String> requestProviderUpdates(@NonNull List<String> providers, long minTimeMs) {
            requestProviderUpdatesCount++;
            requestedProviders = new ArrayList<>(providers);
            requestedMinTimeMs = minTimeMs;
            return new ArrayList<>(providers);
        }

        @Nullable
        @Override
        public NavigationLocation getLastKnownLocationQuietly(@NonNull String provider) {
            return null;
        }

        @Override
        public void requestCurrentLocationSeeds(boolean fineGranted, boolean coarseGranted) {
        }

        @Override
        public void requestSeedForEnabledProvider(@NonNull String provider) {
        }

        @Override
        public void cancelPendingCurrentLocationRequests() {
        }

        @Override
        public void removeUpdates() {
            requestedProviders = Collections.emptyList();
        }

        @NonNull
        @Override
        public String describeAvailability() {
            return "fake provider";
        }
    }

    private static final class FakeFusedLocationUpdateClient implements FusedLocationUpdateClient {
        private final boolean requestSucceeds;
        private int requestUpdatesCount;
        @NonNull
        private Runnable failureListener = () -> {
        };

        FakeFusedLocationUpdateClient(boolean requestSucceeds) {
            this.requestSucceeds = requestSucceeds;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void setUpdateFailureListener(@NonNull Runnable listener) {
            failureListener = listener;
        }

        @Override
        public boolean requestUpdates(long minTimeMs, boolean fineGranted, boolean coarseGranted) {
            requestUpdatesCount++;
            return requestSucceeds;
        }

        @Override
        public void requestCurrentLocationSeed(boolean fineGranted, boolean coarseGranted) {
        }

        @Override
        public void cancelCurrentLocationSeed() {
        }

        @Override
        public void removeUpdates() {
        }

        @NonNull
        @Override
        public String describeAvailability() {
            return "fake fused";
        }

        void failActiveRequest() {
            failureListener.run();
        }
    }

    private static final class FakeGnssTracker implements NavigationGnssTracker {
        @Nullable
        @Override
        public Integer getFixedSatelliteCount() {
            return null;
        }

        @Override
        public void updateForRequestedProviders(@NonNull List<String> requestedProviders) {
        }

        @Override
        public void setTrackingAllowed(boolean allowed) {
        }

        @Override
        public void reset() {
        }
    }
}
