package vibro.navigator.nav.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

public class NavigationLocationControllerTest {
    private static final String GPS_PROVIDER = "gps";
    private static final String NETWORK_PROVIDER = "network";
    private static final String PASSIVE_PROVIDER = "passive";
    private static final String GPS_AND_NETWORK = "gps+network";

    @Test
    public void shouldReuseActiveLocationRequest_returnsTrueForMatchingIntervalAndProviders() {
        assertTrue(NavigationLocationProviders.shouldReuseActiveLocationRequest(
                1_000L,
                GPS_AND_NETWORK,
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseForChangedInterval() {
        assertFalse(NavigationLocationProviders.shouldReuseActiveLocationRequest(
                2_000L,
                GPS_AND_NETWORK,
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseForChangedProviders() {
        assertFalse(NavigationLocationProviders.shouldReuseActiveLocationRequest(
                1_000L,
                GPS_PROVIDER,
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseWhenNoProviderSummaryExists() {
        assertFalse(NavigationLocationProviders.shouldReuseActiveLocationRequest(
                1_000L,
                null,
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void canUseProvider_requiresFinePermissionForGps() {
        assertFalse(NavigationLocationProviders.canUseProvider(
                GPS_PROVIDER,
                false,
                true
        ));
        assertTrue(NavigationLocationProviders.canUseProvider(
                GPS_PROVIDER,
                true,
                false
        ));
    }

    @Test
    public void canUseProvider_allowsNetworkWithCoarsePermission() {
        assertTrue(NavigationLocationProviders.canUseProvider(
                NETWORK_PROVIDER,
                false,
                true
        ));
        assertTrue(NavigationLocationProviders.canUseProvider(
                PASSIVE_PROVIDER,
                false,
                true
        ));
    }

    @Test
    public void recordAcceptedLocationUpdate_refreshesActiveRequestDeadline() {
        MutableClock clock = new MutableClock(5_000L);
        NavigationLocationController controller = controller(clock, GPS_PROVIDER);

        controller.requestLocationUpdates(10_000L);
        long initialDeadline = controller.getNextEvaluationDeadlineElapsedMs();

        clock.nowMs += 2_000L;
        controller.recordAcceptedLocationUpdate();

        assertEquals(initialDeadline + 2_000L, controller.getNextEvaluationDeadlineElapsedMs());
    }

    @Test
    public void requestLocationUpdates_afterStopTrackingRequestsProviderAgainWithLastInterval() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        NavigationLocationController controller = controller(provider, clock);

        controller.requestLocationUpdates(10_000L);
        controller.stopTracking();

        assertEquals(10_000L, controller.getLastRequestedLocationMinTimeMsOrDefault(1_000L));
        assertTrue(provider.requestedProviders.isEmpty());

        controller.requestLocationUpdates(controller.getLastRequestedLocationMinTimeMsOrDefault(1_000L));

        assertEquals(Collections.singletonList(GPS_PROVIDER), provider.requestedProviders);
        assertEquals(2, provider.requestProviderUpdatesCount);
    }

    @Test
    public void resetTrackingState_clearsActiveProviderUpdatesAndPendingSeeds() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        NavigationLocationController controller = controller(provider, clock);

        controller.requestLocationUpdates(10_000L);
        controller.requestCurrentLocationSeeds();
        controller.resetTrackingState();

        assertTrue(provider.requestedProviders.isEmpty());
        assertEquals(1, provider.cancelCurrentLocationRequestsCount);
        assertEquals(NavState.NO_DEADLINE, controller.getNextEvaluationDeadlineElapsedMs());
        assertEquals(1_000L, controller.getLastRequestedLocationMinTimeMsOrDefault(1_000L));
    }

    @Test
    public void requestLocationUpdates_afterPermissionLossClearsActiveProviderState() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        NavigationLocationController controller = controller(provider, clock);

        controller.requestLocationUpdates(10_000L);
        provider.setPermissions(false, false);
        controller.requestLocationUpdates(10_000L);

        assertTrue(provider.requestedProviders.isEmpty());
        assertEquals(NavState.NO_DEADLINE, controller.getNextEvaluationDeadlineElapsedMs());
        assertEquals(1_000L, controller.getLastRequestedLocationMinTimeMsOrDefault(1_000L));
    }

    @Test
    public void requestLocationUpdates_clampsShortIntervalsToDefaultFixInterval() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        NavigationLocationController controller = controller(provider, clock);

        controller.requestLocationUpdates(1_000L);

        assertEquals(NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS, provider.requestedMinTimeMs);
        assertEquals(8_000L, controller.getNextEvaluationDeadlineElapsedMs());
    }

    @Test
    public void requestStartupLocationUpdates_usesFastStartupFixInterval() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        NavigationLocationController controller = controller(provider, clock);

        controller.requestStartupLocationUpdates();

        assertEquals(NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS, provider.requestedMinTimeMs);
        assertEquals(6_000L, controller.getNextEvaluationDeadlineElapsedMs());
    }

    @Test
    public void requestFastLocationUpdates_usesOneSecondFixInterval() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        NavigationLocationController controller = controller(provider, clock);

        controller.requestFastLocationUpdates();

        assertEquals(NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS, provider.requestedMinTimeMs);
        assertEquals(6_000L, controller.getNextEvaluationDeadlineElapsedMs());
    }

    @Test
    public void cancelCurrentLocationSeeds_cancelsLegacyAndFusedSeeds() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient();
        NavigationLocationController controller = controller(provider, fused, clock);

        controller.cancelCurrentLocationSeeds();

        assertEquals(1, provider.cancelCurrentLocationRequestsCount);
        assertEquals(1, fused.cancelCurrentLocationSeedCount);
    }

    @Test
    public void setGnssStatusTrackingAllowed_doesNotStopLocationUpdatesOrClearSatelliteCount() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        FakeGnssTracker gnssTracker = new FakeGnssTracker();
        NavigationLocationController controller =
                controller(provider, new FakeFusedLocationUpdateClient(), gnssTracker, clock);

        controller.requestLocationUpdates(10_000L);
        gnssTracker.fixedSatelliteCount = 8;

        controller.setGnssStatusTrackingAllowed(false);

        assertEquals(Collections.singletonList(GPS_PROVIDER), provider.requestedProviders);
        assertEquals(Integer.valueOf(8), controller.getFixedSatelliteCount());
        assertFalse(gnssTracker.trackingAllowed);
    }

    @Test
    public void setGnssStatusTrackingAllowed_reenablesTrackerAfterUiReturns() {
        MutableClock clock = new MutableClock(5_000L);
        FakeGnssTracker gnssTracker = new FakeGnssTracker();
        NavigationLocationController controller = controller(
                new FakeLocationProvider(GPS_PROVIDER),
                new FakeFusedLocationUpdateClient(),
                gnssTracker,
                clock
        );

        controller.setGnssStatusTrackingAllowed(false);
        controller.requestLocationUpdates(10_000L);
        controller.setGnssStatusTrackingAllowed(true);

        assertTrue(gnssTracker.trackingAllowed);
        assertEquals(Collections.singletonList(GPS_PROVIDER), gnssTracker.requestedProviders);
    }

    @Test
    public void onProviderEnabled_skipsOneShotSeedWhenNotAllowed() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        NavigationLocationController controller = controller(provider, clock);

        controller.onProviderEnabled(GPS_PROVIDER, 1_000L, false);

        assertEquals(Collections.singletonList(GPS_PROVIDER), provider.requestedProviders);
        assertEquals(0, provider.requestSeedForEnabledProviderCount);
    }

    @Test
    public void onProviderEnabledFast_usesOneSecondFixInterval() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        NavigationLocationController controller = controller(provider, clock);

        controller.onProviderEnabledFast(GPS_PROVIDER, false);

        assertEquals(NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS, provider.requestedMinTimeMs);
    }

    @Test
    public void getBestStartupLastKnownLocation_usesProvidedNowForFreshness() {
        MutableClock clock = new MutableClock(5_000L);
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER);
        provider.setLastKnown(location(GPS_PROVIDER, 90_000L, 10f));
        NavigationLocationController controller = controller(provider, clock);

        NavigationLocation selected = controller.getBestStartupLastKnownLocation(100_000L);

        assertNotNull(selected);
        assertEquals(GPS_PROVIDER, selected.getProvider());
    }

    @NonNull
    private static NavigationLocationController controller(
            @NonNull MutableClock clock,
            @NonNull String... enabledProviders
    ) {
        return controller(new FakeLocationProvider(enabledProviders), clock);
    }

    @NonNull
    private static NavigationLocationController controller(
            @NonNull FakeLocationProvider provider,
            @NonNull MutableClock clock
    ) {
        return controller(provider, new FakeFusedLocationUpdateClient(), clock);
    }

    @NonNull
    private static NavigationLocationController controller(
            @NonNull FakeLocationProvider provider,
            @NonNull FakeFusedLocationUpdateClient fused,
            @NonNull MutableClock clock
    ) {
        return controller(provider, fused, new FakeGnssTracker(), clock);
    }

    @NonNull
    private static NavigationLocationController controller(
            @NonNull FakeLocationProvider provider,
            @NonNull FakeFusedLocationUpdateClient fused,
            @NonNull FakeGnssTracker gnssTracker,
            @NonNull MutableClock clock
    ) {
        return new NavigationLocationController(
                provider,
                gnssTracker,
                fused,
                () -> false,
                clock
        );
    }

    private static final class MutableClock implements ElapsedRealtimeClock {
        private long nowMs;

        MutableClock(long nowMs) {
            this.nowMs = nowMs;
        }

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
        private boolean fineGranted = true;
        private boolean coarseGranted = true;
        private int requestProviderUpdatesCount;
        private int cancelCurrentLocationRequestsCount;
        private int requestSeedForEnabledProviderCount;
        private long requestedMinTimeMs;
        @Nullable
        private NavigationLocation gpsLastKnown;
        @Nullable
        private NavigationLocation networkLastKnown;

        FakeLocationProvider(@NonNull String... enabledProviders) {
            this.enabledProviders = Arrays.asList(enabledProviders);
        }

        void setPermissions(boolean fineGranted, boolean coarseGranted) {
            this.fineGranted = fineGranted;
            this.coarseGranted = coarseGranted;
        }

        void setLastKnown(@NonNull NavigationLocation location) {
            if (GPS_PROVIDER.equals(location.getProvider())) {
                gpsLastKnown = location;
            } else if (NETWORK_PROVIDER.equals(location.getProvider())) {
                networkLastKnown = location;
            }
        }

        @Override
        public boolean hasFineLocationPermission() {
            return fineGranted;
        }

        @Override
        public boolean hasCoarseLocationPermission() {
            return coarseGranted;
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
            if (GPS_PROVIDER.equals(provider)) {
                return gpsLastKnown;
            }
            if (NETWORK_PROVIDER.equals(provider)) {
                return networkLastKnown;
            }
            return null;
        }

        @Override
        public void requestCurrentLocationSeeds(boolean fineGranted, boolean coarseGranted) {
        }

        @Override
        public void requestSeedForEnabledProvider(@NonNull String provider) {
            requestSeedForEnabledProviderCount++;
        }

        @Override
        public void cancelPendingCurrentLocationRequests() {
            cancelCurrentLocationRequestsCount++;
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

    private static final class FakeGnssTracker implements NavigationGnssTracker {
        @Nullable
        private Integer fixedSatelliteCount;
        @NonNull
        private List<String> requestedProviders = Collections.emptyList();
        private boolean trackingAllowed = true;

        @Nullable
        @Override
        public Integer getFixedSatelliteCount() {
            return fixedSatelliteCount;
        }

        @Override
        public void updateForRequestedProviders(@NonNull List<String> requestedProviders) {
            this.requestedProviders = new ArrayList<>(requestedProviders);
        }

        @Override
        public void setTrackingAllowed(boolean allowed) {
            trackingAllowed = allowed;
        }

        @Override
        public void reset() {
            fixedSatelliteCount = null;
            requestedProviders = Collections.emptyList();
        }
    }

    private static final class FakeFusedLocationUpdateClient implements FusedLocationUpdateClient {
        private int cancelCurrentLocationSeedCount;

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public boolean requestUpdates(long minTimeMs, boolean fineGranted, boolean coarseGranted) {
            return false;
        }

        @Override
        public void requestCurrentLocationSeed(boolean fineGranted, boolean coarseGranted) {
        }

        @Override
        public void cancelCurrentLocationSeed() {
            cancelCurrentLocationSeedCount++;
        }

        @Override
        public void removeUpdates() {
        }

        @NonNull
        @Override
        public String describeAvailability() {
            return "fake fused";
        }
    }

    @NonNull
    private static NavigationLocation location(@NonNull String provider, long timeMs, float accuracyMeters) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setLatitude(48.2082d);
        location.setLongitude(16.3738d);
        location.setTime(timeMs);
        location.setAccuracy(accuracyMeters);
        return location;
    }
}
