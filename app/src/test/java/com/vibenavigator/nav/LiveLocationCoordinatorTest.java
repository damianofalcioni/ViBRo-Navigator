package com.vibenavigator.nav;

import android.location.Location;
import android.location.LocationManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class LiveLocationCoordinatorTest {

    @Test
    public void selectBestLiveLocation_prefersMoreAccurateGpsWhenFresh() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        coordinator.remember(newLocation(LocationManager.NETWORK_PROVIDER, 500L, 35f, 48.20d, 16.37d));
        coordinator.remember(newLocation(LocationManager.GPS_PROVIDER, 700L, 5f, 48.21d, 16.38d));

        Location selected = coordinator.selectBestLiveLocation();

        assertNotNull(selected);
        assertEquals(LocationManager.GPS_PROVIDER, selected.getProvider());
        assertEquals(48.21d, selected.getLatitude(), 0.0);
        assertEquals(16.38d, selected.getLongitude(), 0.0);
    }

    @Test
    public void selectBestLiveLocation_prefersMuchFresherNetworkFix() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        coordinator.remember(newLocation(LocationManager.GPS_PROVIDER, 12_000L, 8f, 48.20d, 16.37d));
        coordinator.remember(newLocation(LocationManager.NETWORK_PROVIDER, 1_000L, 12f, 48.25d, 16.40d));

        Location selected = coordinator.selectBestLiveLocation();

        assertNotNull(selected);
        assertEquals(LocationManager.NETWORK_PROVIDER, selected.getProvider());
        assertEquals(48.25d, selected.getLatitude(), 0.0);
        assertEquals(16.40d, selected.getLongitude(), 0.0);
    }

    @Test
    public void shouldDispatch_rejectsSameFixButAcceptsAccuracyImprovement() {
        LiveLocationCoordinator coordinator = new LiveLocationCoordinator();
        Location initial = newLocation(LocationManager.GPS_PROVIDER, 500L, 30f, 48.20d, 16.37d);
        coordinator.markDispatched(initial);

        assertFalse(coordinator.shouldDispatch(new Location(initial)));

        Location improved = new Location(initial);
        improved.setAccuracy(10f);

        assertTrue(coordinator.shouldDispatch(improved));
    }

    private static Location newLocation(
            String provider,
            long ageMs,
            float accuracyMeters,
            double lat,
            double lon
    ) {
        Location location = new Location(provider);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAccuracy(accuracyMeters);
        location.setTime(System.currentTimeMillis() - ageMs);
        return location;
    }
}
