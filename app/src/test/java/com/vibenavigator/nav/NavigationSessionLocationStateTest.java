package com.vibenavigator.nav;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.location.Location;
import android.location.LocationManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NavigationSessionLocationStateTest {

    @Test
    public void isLikelyStationary_returnsFalseWhenLowSpeedSamplesStillCoverGround() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(location(baseTimeMs, 48.2082000, 16.3738000, 0.10f));
        state.onRawLocationChanged(location(baseTimeMs + 1_000L, 48.2082060, 16.3738000, 0.12f));
        state.onRawLocationChanged(location(baseTimeMs + 2_000L, 48.2082120, 16.3738000, 0.08f));
        state.onRawLocationChanged(location(baseTimeMs + 3_000L, 48.2082180, 16.3738000, 0.09f));

        assertFalse(state.isLikelyStationary());
    }

    @Test
    public void isLikelyStationary_returnsTrueWhenRecentSamplesOnlyJitterInPlace() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(location(baseTimeMs, 48.2082000, 16.3738000, 0.00f));
        state.onRawLocationChanged(location(baseTimeMs + 1_000L, 48.2082004, 16.3738002, 0.05f));
        state.onRawLocationChanged(location(baseTimeMs + 2_000L, 48.2082002, 16.3738001, 0.04f));
        state.onRawLocationChanged(location(baseTimeMs + 3_000L, 48.2082003, 16.3738000, 0.03f));

        assertTrue(state.isLikelyStationary());
    }

    private static Location location(long timeMs, double lat, double lon, float speedMps) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(5f);
        location.setSpeed(speedMps);
        return location;
    }
}
