package vibro.navigator.nav.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NavigationLocationUpdateRequesterTest {
    private static final String GPS_PROVIDER = "gps";
    private static final String NETWORK_PROVIDER = "network";

    @Test
    public void request_whenFusedEnabled_requestsFusedAndLegacyProvidersInParallel() {
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER, NETWORK_PROVIDER);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient(true);
        NavigationLocationUpdateRequester requester = requester(provider, fused);

        NavigationLocationUpdateRequester.Result result = requester.request(1_000L, true, -1L, null);

        assertTrue(result.hasActiveRequest());
        assertEquals("fused+gps+network", result.activeProviderSummary());
        assertEquals(1, fused.requestUpdatesCount);
        assertEquals(Arrays.asList(GPS_PROVIDER, NETWORK_PROVIDER), provider.requestedProviders);
        assertEquals(1_000L, provider.requestedMinTimeMs);
    }

    @Test
    public void request_whenFusedRequestFails_keepsLegacyProvidersActive() {
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER, NETWORK_PROVIDER);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient(false);
        NavigationLocationUpdateRequester requester = requester(provider, fused);

        NavigationLocationUpdateRequester.Result result = requester.request(1_000L, true, -1L, null);

        assertTrue(result.hasActiveRequest());
        assertEquals("gps+network", result.activeProviderSummary());
        assertEquals(1, fused.requestUpdatesCount);
        assertEquals(Arrays.asList(GPS_PROVIDER, NETWORK_PROVIDER), provider.requestedProviders);
    }

    @Test
    public void request_whenProviderSummaryMatches_reusesExistingSubscription() {
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER, NETWORK_PROVIDER);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient(true);
        NavigationLocationUpdateRequester requester = requester(provider, fused);

        NavigationLocationUpdateRequester.Result result =
                requester.request(1_000L, true, 1_000L, "fused+gps+network");

        assertTrue(result.hasActiveRequest());
        assertEquals("fused+gps+network", result.activeProviderSummary());
        assertEquals(0, fused.requestUpdatesCount);
        assertTrue(provider.requestedProviders.isEmpty());
    }

    @NonNull
    private static NavigationLocationUpdateRequester requester(
            @NonNull NavigationLocationProvider provider,
            @NonNull FusedLocationUpdateClient fused
    ) {
        return new NavigationLocationUpdateRequester(
                provider,
                fused,
                new FakeGnssTracker(),
                () -> {
                },
                () -> "test availability"
        );
    }

    private static final class FakeLocationProvider implements NavigationLocationProvider {
        @NonNull
        private final List<String> enabledProviders;
        @NonNull
        private List<String> requestedProviders = Collections.emptyList();
        private long requestedMinTimeMs = -1L;

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
        }

        @NonNull
        @Override
        public String describeAvailability() {
            return "fake provider";
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
        public void reset() {
        }
    }

    private static final class FakeFusedLocationUpdateClient implements FusedLocationUpdateClient {
        private final boolean requestSucceeds;
        private int requestUpdatesCount;

        FakeFusedLocationUpdateClient(boolean requestSucceeds) {
            this.requestSucceeds = requestSucceeds;
        }

        @Override
        public boolean isAvailable() {
            return true;
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
        public void removeUpdates() {
        }

        @NonNull
        @Override
        public String describeAvailability() {
            return "fake fused";
        }
    }
}
