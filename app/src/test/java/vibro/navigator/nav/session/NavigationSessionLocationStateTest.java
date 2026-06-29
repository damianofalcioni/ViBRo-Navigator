package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;

import org.junit.Test;

public class NavigationSessionLocationStateTest {

    @Test
    public void isLikelyStationary_returnsFalseWhenLowSpeedSamplesStillCoverGround() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 0.10f));
        onRawLocationChanged(state, location(baseTimeMs + 1_000L, 48.2082060, 16.3738000, 0.12f));
        onRawLocationChanged(state, location(baseTimeMs + 2_000L, 48.2082120, 16.3738000, 0.08f));
        onRawLocationChanged(state, location(baseTimeMs + 3_000L, 48.2082180, 16.3738000, 0.09f));

        assertFalse(state.isLikelyStationary());
    }

    @Test
    public void isLikelyStationary_returnsTrueWhenRecentSamplesOnlyJitterInPlace() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 0.00f));
        onRawLocationChanged(state, location(baseTimeMs + 1_000L, 48.2082004, 16.3738002, 0.05f));
        onRawLocationChanged(state, location(baseTimeMs + 2_000L, 48.2082002, 16.3738001, 0.04f));
        onRawLocationChanged(state, location(baseTimeMs + 3_000L, 48.2082003, 16.3738000, 0.03f));

        assertTrue(state.isLikelyStationary());
    }

    @Test
    public void trustedActualBearingDegreesForReroute_acceptsAccurateWalkingSpeedGpsBearing() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = location(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.2f);
        update.setBearing(84f);
        update.setBearingAccuracyDegrees(12f);
        NavigationSessionLocationState.Update accepted = onRawLocationChanged(state, update);

        Double bearingDegrees = state.trustedActualBearingDegreesForReroute(accepted.getFilteredLocation());

        assertNotNull(bearingDegrees);
        assertEquals(84.0, bearingDegrees, 0.0);
    }

    @Test
    public void trustedActualBearingDegreesForReroute_rejectsNoAccuracyGpsBearingBelowCourseSpeed() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = location(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.8f);
        update.setBearing(84f);
        NavigationSessionLocationState.Update accepted = onRawLocationChanged(state, update);

        Double bearingDegrees = state.trustedActualBearingDegreesForReroute(accepted.getFilteredLocation());

        assertNull(bearingDegrees);
    }

    @Test
    public void trustedActualBearingDegreesForReroute_acceptsNoAccuracyGpsBearingAtCourseSpeed() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = location(baseTimeMs + 2_500L, 48.2082600, 16.3738000, 3.0f);
        update.setBearing(84f);
        NavigationSessionLocationState.Update accepted = onRawLocationChanged(state, update);

        Double bearingDegrees = state.trustedActualBearingDegreesForReroute(accepted.getFilteredLocation());

        assertNotNull(bearingDegrees);
        assertEquals(84.0, bearingDegrees, 0.0);
    }

    @Test
    public void preferredCompassHeading_keepsCompassSensorAtWalkingSpeed() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = location(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.2f);
        update.setBearing(84f);
        update.setBearingAccuracyDegrees(12f);
        NavigationSessionLocationState.Update accepted = onRawLocationChanged(state, update);

        NavigationSessionLocationState.HeadingEstimate headingEstimate =
                state.preferredCompassHeading(accepted.getFilteredLocation(), false);

        assertNull(headingEstimate);
    }

    @Test
    public void preferredCompassHeading_prefersTrustedGpsBearingWhileMovingFast() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 3_000L;

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = location(baseTimeMs + 2_500L, 48.2082600, 16.3738000, 3.0f);
        update.setBearing(84f);
        update.setBearingAccuracyDegrees(12f);
        NavigationSessionLocationState.Update accepted = onRawLocationChanged(state, update);

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

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 0.4f));
        NavigationLocation update = location(baseTimeMs + 2_500L, 48.2082600, 16.3738000, 3.0f);
        update.setBearing(84f);
        update.setBearingAccuracyDegrees(40f);
        NavigationSessionLocationState.Update accepted = onRawLocationChanged(state, update);

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

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 0f));
        NavigationLocation update = location(baseTimeMs + 2_500L, 48.2082200, 16.3738000, 1.2f);
        update.setBearing(84f);
        update.setBearingAccuracyDegrees(12f);
        NavigationSessionLocationState.Update accepted = onRawLocationChanged(state, update);

        NavigationSessionLocationState.HeadingEstimate headingEstimate =
                state.preferredCompassHeading(accepted.getFilteredLocation(), true);

        assertNull(headingEstimate);
    }

    @Test
    public void reset_reinitializesKalmanFilterOnNextLocation() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 5_000L;

        onRawLocationChanged(state, location(baseTimeMs, 48.2082000, 16.3738000, 1.0f));
        onRawLocationChanged(state, location(baseTimeMs + 1_000L, 48.2082600, 16.3738000, 1.0f));

        state.reset();

        NavigationLocation restartLocation = location(baseTimeMs + 2_000L, 48.2100000, 16.3800000, 0.5f);
        NavigationSessionLocationState.Update accepted = onRawLocationChanged(state, restartLocation);
        NavigationLocation filtered = accepted.getFilteredLocation();

        assertEquals(restartLocation.getLatitude(), filtered.getLatitude(), 0.0);
        assertEquals(restartLocation.getLongitude(), filtered.getLongitude(), 0.0);
    }

    @Test
    public void onRawLocationChanged_reinitializesFilterAndMotionAfterLongAcceptedFixGap() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 1_000L;

        state.onRawLocationChanged(location(baseTimeMs, 48.2082000, 16.3738000, 1.0f), 1_000L);

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
    public void speedMps_returnsZeroForNonFiniteReportedSpeed() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();

        assertEquals(0.0f, state.speedMps(location(1_000L, 48.2082000, 16.3738000, Float.NaN)), 0.0f);
        assertEquals(
                0.0f,
                state.speedMps(location(1_000L, 48.2082000, 16.3738000, Float.POSITIVE_INFINITY)),
                0.0f
        );
    }

    @Test
    public void onRawLocationChanged_usesProvidedNowForLiveFreshness() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long nowMs = 100_000L;
        NavigationLocation rawLocation = location(nowMs - 1_000L, 48.2082000, 16.3738000, 1.0f);

        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(rawLocation, nowMs);

        assertFalse(accepted.isDropped());
        assertEquals(rawLocation.getLatitude(), accepted.getFilteredLocation().getLatitude(), 0.0);
        assertEquals(rawLocation.getLongitude(), accepted.getFilteredLocation().getLongitude(), 0.0);
    }

    @Test
    public void onRawLocationChanged_resetsStartupFilterWhenFirstRouteGradeFixArrives() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();

        state.onRawLocationChanged(
                location(NavigationLocationProviders.NETWORK_PROVIDER, 1_000L, 48.2080000, 16.3730000, 300f),
                1_000L,
                true
        );
        state.onRawLocationChanged(
                location("fused", 2_000L, 48.2079000, 16.3732000, 35f),
                2_000L,
                true
        );
        NavigationLocation gps = location(
                NavigationLocationProviders.GPS_PROVIDER,
                6_000L,
                48.2071000,
                16.3740000,
                19f
        );

        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(gps, 6_000L, true);

        assertEquals(gps.getLatitude(), accepted.getFilteredLocation().getLatitude(), 0.0);
        assertEquals(gps.getLongitude(), accepted.getFilteredLocation().getLongitude(), 0.0);
    }

    @Test
    public void onRawLocationChanged_doesNotReacquireAfterShortAcceptedFixGap() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        long baseTimeMs = System.currentTimeMillis() - 1_000L;

        state.onRawLocationChanged(location(baseTimeMs, 48.2082000, 16.3738000, 1.0f), 1_000L);
        NavigationSessionLocationState.Update accepted = state.onRawLocationChanged(
                location(System.currentTimeMillis(), 48.2082600, 16.3738000, 1.0f),
                5_000L
        );

        assertFalse(accepted.isReacquiringAfterLongGap());
    }

    private static NavigationLocation location(long timeMs, double lat, double lon, float speedMps) {
        NavigationLocation location = new NavigationLocation(NavigationLocationProviders.GPS_PROVIDER);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(5f);
        location.setSpeed(speedMps);
        return location;
    }

    private static NavigationLocation location(
            String provider,
            long timeMs,
            double lat,
            double lon,
            float accuracyMeters
    ) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(accuracyMeters);
        return location;
    }

    private static NavigationLocation locationWithoutSpeed(long timeMs, double lat, double lon) {
        NavigationLocation location = new NavigationLocation(NavigationLocationProviders.GPS_PROVIDER);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(5f);
        return location;
    }

    private static NavigationSessionLocationState.Update onRawLocationChanged(
            NavigationSessionLocationState state,
            NavigationLocation location
    ) {
        return state.onRawLocationChanged(location, location.getTime());
    }
}
