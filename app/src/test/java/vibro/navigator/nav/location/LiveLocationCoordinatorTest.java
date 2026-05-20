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

        LiveLocationFix selected = coordinator.selectBestLiveFix(NOW_MS);

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

        LiveLocationFix selected = coordinator.selectBestLiveFix(NOW_MS);

        assertNotNull(selected);
        assertEquals(NETWORK_PROVIDER, selected.provider);
        assertEquals(48.25d, selected.lat, 0.0);
        assertEquals(16.40d, selected.lon, 0.0);
    }

    @Test
    public void shouldDispatch_rejectsSameFixButAcceptsAccuracyImprovement() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        LiveLocationFix initial =
                newFix(GPS_PROVIDER, 500L, 30f, 48.20d, 16.37d);
        coordinator.markDispatched(initial);

        assertFalse(coordinator.shouldDispatch(initial));

        LiveLocationFix improved =
                newFix(GPS_PROVIDER, 500L, 10f, 48.20d, 16.37d);

        assertTrue(coordinator.shouldDispatch(improved));
    }

    @Test
    public void selectBestLiveLocation_acceptsFusedFixWhenItIsOnlyFreshCandidate() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        coordinator.remember(newFix(LiveLocationCoordinator.FUSED_PROVIDER, 500L, 8f, 48.30d, 16.41d));

        LiveLocationFix selected = coordinator.selectBestLiveFix(NOW_MS);

        assertNotNull(selected);
        assertEquals(LiveLocationCoordinator.FUSED_PROVIDER, selected.provider);
        assertEquals(48.30d, selected.lat, 0.0);
        assertEquals(16.41d, selected.lon, 0.0);
    }

    private static LiveLocationFix newFix(
            String provider,
            long ageMs,
            float accuracyMeters,
            double lat,
            double lon
    ) {
        return new LiveLocationFix(provider, NOW_MS - ageMs, accuracyMeters, lat, lon);
    }
}
