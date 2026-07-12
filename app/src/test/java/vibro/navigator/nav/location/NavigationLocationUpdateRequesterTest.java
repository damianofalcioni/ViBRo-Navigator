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
    public void request_whenFusedEnabled_requestsOnlyFusedProvider() {
        FakeLocationProvider provider = new FakeLocationProvider(GPS_PROVIDER, NETWORK_PROVIDER);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient(true);
        NavigationLocationUpdateRequester requester = requester(provider, fused);

        NavigationLocationUpdateRequester.Result result = requester.request(1_000L, true, -1L, null);

        assertTrue(result.hasActiveRequest());
        assertEquals("fused", result.activeProviderSummary());
        assertTrue(result.isFusedActive());
        assertEquals(1, fused.requestUpdatesCount);
        assertTrue(provider.requestedProviders.isEmpty());
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
                requester.request(1_000L, true, 1_000L, "fused");

        assertTrue(result.hasActiveRequest());
        assertEquals("fused", result.activeProviderSummary());
        assertEquals(0, fused.requestUpdatesCount);
        assertTrue(provider.requestedProviders.isEmpty());
    }

    @Test
    public void request_whenLocationPermissionMissing_clearsActiveRequest() {
        FakeLocationProvider provider = new FakeLocationProvider(false, false, GPS_PROVIDER);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient(true);
        NavigationLocationUpdateRequester requester = requester(provider, fused);

        NavigationLocationUpdateRequester.Result result =
                requester.request(1_000L, true, 1_000L, GPS_PROVIDER);

        assertTrue(result.shouldClearActiveRequest());
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
        private final boolean fineGranted;
        private final boolean coarseGranted;
        @NonNull
        private List<String> requestedProviders = Collections.emptyList();

        FakeLocationProvider(@NonNull String... enabledProviders) {
            this(true, true, enabledProviders);
        }

        FakeLocationProvider(boolean fineGranted, boolean coarseGranted, @NonNull String... enabledProviders) {
            this.enabledProviders = Arrays.asList(enabledProviders);
            this.fineGranted = fineGranted;
            this.coarseGranted = coarseGranted;
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
            requestedProviders = new ArrayList<>(providers);
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
        public void setTrackingAllowed(boolean allowed) {
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
        public void setUpdateFailureListener(@NonNull Runnable listener) {
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
    }
}
