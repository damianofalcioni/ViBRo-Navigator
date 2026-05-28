package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;
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

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 0.10f));
        state.onRawLocationChanged(NavigationLocation(baseTimeMs + 1_000L, 48.2082060, 16.3738000, 0.12f));
        state.onRawLocationChanged(NavigationLocation(baseTimeMs + 2_000L, 48.2082120, 16.3738000, 0.08f));
        state.onRawLocationChanged(NavigationLocation(baseTimeMs + 3_000L, 48.2082180, 16.3738000, 0.09f));

        assertFalse(state.isLikelyStationary());
    }

    @Test
    public void isLikelyStationary_returnsTrueWhenRecentSamplesOnlyJitterInPlace() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 0.00f));
        state.onRawLocationChanged(NavigationLocation(baseTimeMs + 1_000L, 48.2082004, 16.3738002, 0.05f));
        state.onRawLocationChanged(NavigationLocation(baseTimeMs + 2_000L, 48.2082002, 16.3738001, 0.04f));
        state.onRawLocationChanged(NavigationLocation(baseTimeMs + 3_000L, 48.2082003, 16.3738000, 0.03f));

        assertTrue(state.isLikelyStationary());
    }

    @Test
    public void trustedActualBearingDegreesForReroute_acceptsAccurateWalkingSpeedGpsBearing() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = NavigationLocation(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.2f);
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
    public void trustedActualBearingDegreesForReroute_rejectsNoAccuracyGpsBearingBelowCourseSpeed() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = NavigationLocation(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.8f);
        update.setBearing(84f);
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(update);

        Double bearingDegrees = state.trustedActualBearingDegreesForReroute(accepted.getFilteredLocation());

        assertNull(bearingDegrees);
    }

    @Test
    public void trustedActualBearingDegreesForReroute_acceptsNoAccuracyGpsBearingAtCourseSpeed() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = NavigationLocation(baseTimeMs + 2_500L, 48.2082600, 16.3738000, 3.0f);
        update.setBearing(84f);
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(update);

        Double bearingDegrees = state.trustedActualBearingDegreesForReroute(accepted.getFilteredLocation());

        assertNotNull(bearingDegrees);
        assertEquals(84.0, bearingDegrees, 0.0);
    }

    @Test
    public void preferredCompassHeading_keepsCompassSensorAtWalkingSpeed() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = NavigationLocation(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.2f);
        update.setBearing(84f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            update.setBearingAccuracyDegrees(12f);
        }
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(update);

        NavigationSessionLocationState.HeadingEstimate headingEstimate =
                state.preferredCompassHeading(accepted.getFilteredLocation(), false);

        assertNull(headingEstimate);
    }

    @Test
    public void preferredCompassHeading_prefersTrustedGpsBearingWhileMovingFast() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = NavigationLocation(baseTimeMs + 2_500L, 48.2082600, 16.3738000, 3.0f);
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

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = NavigationLocation(baseTimeMs + 2_500L, 48.2082600, 16.3738000, 3.0f);
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

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 0f));
        NavigationLocation update = NavigationLocation(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.2f);
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

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 1.0f));
        state.onRawLocationChanged(NavigationLocation(baseTimeMs + 1_000L, 48.2082600, 16.3738000, 1.0f));

        state.reset();

        NavigationLocation restartLocation = NavigationLocation(baseTimeMs + 2_000L, 48.2100000, 16.3800000, 0.5f);
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(restartLocation);
        NavigationLocation filtered = accepted.getFilteredLocation();

        assertEquals(restartLocation.getLatitude(), filtered.getLatitude(), 0.0);
        assertEquals(restartLocation.getLongitude(), filtered.getLongitude(), 0.0);
    }

    @Test
    public void onRawLocationChanged_reinitializesFilterAndMotionAfterLongAcceptedFixGap() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 1_000L;

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 1.0f), 1_000L);

        NavigationLocation reacquiredLocation = locationWithoutSpeed(
                System.currentTimeMillis(),
                48.2100000,
                16.3800000
        );
        NavigationSessionLocationState.Update accepted =
                state.onRawLocationChanged(reacquiredLocation, 17_000L);
        NavigationLocation filtered = accepted.getFilteredLocation();

        assertTrue(accepted.isReacquiringAfterLongGap());
        assertEquals(reacquiredLocation.getLatitude(), filtered.getLatitude(), 0.0);
        assertEquals(reacquiredLocation.getLongitude(), filtered.getLongitude(), 0.0);
        assertEquals(0.0f, state.speedMps(filtered), 0.0f);
    }

    @Test
    public void onRawLocationChanged_doesNotReacquireAfterShortAcceptedFixGap() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 1_000L;

        state.onRawLocationChanged(NavigationLocation(baseTimeMs, 48.2082000, 16.3738000, 1.0f), 1_000L);
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(
                NavigationLocation(System.currentTimeMillis(), 48.2082600, 16.3738000, 1.0f),
                5_000L
        );

        assertFalse(accepted.isReacquiringAfterLongGap());
    }

    private static NavigationLocation NavigationLocation(long timeMs, double lat, double lon, float speedMps) {
        NavigationLocation NavigationLocation = new NavigationLocation(NavigationLocationProviders.GPS_PROVIDER);
        NavigationLocation.setLatitude(lat);
        NavigationLocation.setLongitude(lon);
        NavigationLocation.setTime(timeMs);
        NavigationLocation.setAccuracy(5f);
        NavigationLocation.setSpeed(speedMps);
        return NavigationLocation;
    }

    private static NavigationLocation locationWithoutSpeed(long timeMs, double lat, double lon) {
        NavigationLocation NavigationLocation = new NavigationLocation(NavigationLocationProviders.GPS_PROVIDER);
        NavigationLocation.setLatitude(lat);
        NavigationLocation.setLongitude(lon);
        NavigationLocation.setTime(timeMs);
        NavigationLocation.setAccuracy(5f);
        return NavigationLocation;
    }
}
