package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

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

    @Test
    public void trustedActualBearingDegreesForReroute_acceptsAccurateWalkingSpeedGpsBearing() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(location(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        Location update = location(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.2f);
        update.setBearing(84f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            update.setBearingAccuracyDegrees(12f);
        }
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(update);

        Double bearingDegrees = state.trustedActualBearingDegreesForReroute(accepted.getFilteredLocation());

        assertNotNull(bearingDegrees);
        assertEquals(84.0, bearingDegrees, 0.0);
    }

    @Test
    public void preferredCompassHeading_prefersTrustedGpsBearingWhileMoving() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(location(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        Location update = location(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.2f);
        update.setBearing(84f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            update.setBearingAccuracyDegrees(12f);
        }
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(update);

        NavigationSessionLocationState.HeadingEstimate headingEstimate =
                state.preferredCompassHeading(accepted.getFilteredLocation(), false);

        assertNotNull(headingEstimate);
        assertEquals(84.0, headingEstimate.headingDegrees, 0.0);
        assertEquals(12.0f, headingEstimate.headingAccuracyDegrees, 0.0f);
    }

    @Test
    public void preferredCompassHeading_fallsBackToMovementCourseWhenGpsBearingAccuracyIsLow() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 4_000L;

        state.onRawLocationChanged(location(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        Location update = location(baseTimeMs + 2_500L, 48.2082600, 16.3738000, 1.2f);
        update.setBearing(84f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            update.setBearingAccuracyDegrees(40f);
        }
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(update);

        NavigationSessionLocationState.HeadingEstimate headingEstimate =
                state.preferredCompassHeading(accepted.getFilteredLocation(), false);

        assertNotNull(headingEstimate);
        assertEquals(0.0, headingEstimate.headingDegrees, 15.0);
        assertNull(headingEstimate.headingAccuracyDegrees);
    }

    @Test
    public void preferredCompassHeading_ignoresMovingSourcesWhileStationary() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(location(baseTimeMs, 48.2082000, 16.3738000, 0f));
        Location update = location(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.2f);
        update.setBearing(84f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            update.setBearingAccuracyDegrees(12f);
        }
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(update);

        NavigationSessionLocationState.HeadingEstimate headingEstimate =
                state.preferredCompassHeading(accepted.getFilteredLocation(), true);

        assertNull(headingEstimate);
    }

    @Test
    public void reset_reinitializesKalmanFilterOnNextLocation() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 5_000L;

        state.onRawLocationChanged(location(baseTimeMs, 48.2082000, 16.3738000, 1.0f));
        state.onRawLocationChanged(location(baseTimeMs + 1_000L, 48.2082600, 16.3738000, 1.0f));

        state.reset();

        Location restartLocation = location(baseTimeMs + 2_000L, 48.2100000, 16.3800000, 0.5f);
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(restartLocation);
        Location filtered = accepted.getFilteredLocation();

        assertEquals(restartLocation.getLatitude(), filtered.getLatitude(), 0.0);
        assertEquals(restartLocation.getLongitude(), filtered.getLongitude(), 0.0);
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
