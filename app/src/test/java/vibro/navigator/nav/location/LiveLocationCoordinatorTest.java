package vibro.navigator.nav.location;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LiveLocationCoordinatorTest {

    private static final long NOW_MS = 100_000L;
    private static final String GPS_PROVIDER = "gps";
    private static final String NETWORK_PROVIDER = "network";

    @Test
    public void selectBestLiveLocation_prefersMoreAccurateGpsWhenFresh() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        coordinator.remember(newFix(NETWORK_PROVIDER, 500L, 35f, 48.20d, 16.37d));
        coordinator.remember(newFix(GPS_PROVIDER, 700L, 5f, 48.21d, 16.38d));

        NavigationLocationFix selected = coordinator.selectBestLiveFix(NOW_MS);

        assertNotNull(selected);
        assertEquals(GPS_PROVIDER, selected.provider);
        assertEquals(48.21d, selected.lat, 0.0);
        assertEquals(16.38d, selected.lon, 0.0);
    }

    @Test
    public void selectBestLiveLocation_prefersMuchFresherNetworkFix() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        coordinator.remember(newFix(GPS_PROVIDER, 12_000L, 8f, 48.20d, 16.37d));
        coordinator.remember(newFix(NETWORK_PROVIDER, 1_000L, 12f, 48.25d, 16.40d));

        NavigationLocationFix selected = coordinator.selectBestLiveFix(NOW_MS);

        assertNotNull(selected);
        assertEquals(NETWORK_PROVIDER, selected.provider);
        assertEquals(48.25d, selected.lat, 0.0);
        assertEquals(16.40d, selected.lon, 0.0);
    }

    @Test
    public void shouldDispatch_rejectsSameFixButAcceptsAccuracyImprovement() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        NavigationLocationFix initial =
                newFix(GPS_PROVIDER, 500L, 30f, 48.20d, 16.37d);
        coordinator.markDispatched(initial);

        assertFalse(coordinator.shouldDispatch(initial));

        NavigationLocationFix improved =
                newFix(GPS_PROVIDER, 500L, 10f, 48.20d, 16.37d);

        assertTrue(coordinator.shouldDispatch(improved));
    }

    @Test
    public void shouldDispatch_rejectsEarlyCrossProviderFixInsideRequestedInterval() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        NavigationLocation gps = location(GPS_PROVIDER, 1_000_000L, 100_000L, 5f, 48.20d, 16.37d);
        coordinator.markDispatched(gps, 100_000L);
        NavigationLocation fused = location(
                LiveLocationCoordinator.FUSED_PROVIDER,
                1_008_000L,
                108_000L,
                5f,
                48.21d,
                16.38d
        );

        assertFalse(coordinator.shouldDispatch(fused, 108_000L, 60_000L));
        assertTrue(coordinator.shouldDispatch(fused, 160_000L, 60_000L));
    }

    @Test
    public void shouldDispatch_acceptsMateriallyBetterCrossProviderFixInsideRequestedInterval() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        NavigationLocation fused = location(
                LiveLocationCoordinator.FUSED_PROVIDER,
                1_000_000L,
                100_000L,
                40f,
                48.20d,
                16.37d
        );
        coordinator.markDispatched(fused, 100_000L);
        NavigationLocation gps = location(GPS_PROVIDER, 1_001_000L, 101_000L, 5f, 48.20d, 16.37d);

        assertTrue(coordinator.shouldDispatch(gps, 101_000L, 60_000L));
    }

    @Test
    public void selectBestLiveLocation_acceptsFusedFixWhenItIsOnlyFreshCandidate() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        coordinator.remember(newFix(LiveLocationCoordinator.FUSED_PROVIDER, 500L, 8f, 48.30d, 16.41d));

        NavigationLocationFix selected = coordinator.selectBestLiveFix(NOW_MS);

        assertNotNull(selected);
        assertEquals(LiveLocationCoordinator.FUSED_PROVIDER, selected.provider);
        assertEquals(48.30d, selected.lat, 0.0);
        assertEquals(16.41d, selected.lon, 0.0);
    }

    @Test
    public void selectBestLiveLocation_usesElapsedRealtimeWhenAvailable() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        coordinator.remember(location(GPS_PROVIDER, 1_000_000L, NOW_MS - 20_000L, 5f, 48.20d, 16.37d));
        coordinator.remember(location(NETWORK_PROVIDER, 900_000L, NOW_MS - 1_000L, 12f, 48.25d, 16.40d));

        NavigationLocation selected = coordinator.selectBestLiveLocation(NOW_MS);

        assertNotNull(selected);
        assertEquals(NETWORK_PROVIDER, selected.getProvider());
        assertEquals(48.25d, selected.getLatitude(), 0.0);
        assertEquals(16.40d, selected.getLongitude(), 0.0);
    }

    private static NavigationLocationFix newFix(
            String provider,
            long ageMs,
            float accuracyMeters,
            double lat,
            double lon
    ) {
        return new NavigationLocationFix(provider, NOW_MS - ageMs, accuracyMeters, lat, lon);
    }

    private static NavigationLocation location(
            String provider,
            long wallTimeMs,
            long elapsedRealtimeMs,
            float accuracyMeters,
            double lat,
            double lon
    ) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setTime(wallTimeMs, elapsedRealtimeMs);
        location.setAccuracy(accuracyMeters);
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }
}
