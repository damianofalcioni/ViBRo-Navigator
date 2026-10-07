package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;

public class NavigationSessionHeadingResolverTest {
    private final NavigationSessionLocationState locationState = new NavigationSessionLocationState();
    private final NavigationSessionHeadingResolver resolver = new NavigationSessionHeadingResolver(locationState);

    @Test
    public void slowMovementUsesAccurateLocationBearingAndItsAccuracy() {
        NavigationLocation location = movingLocation(0.4f, 84f);
        NavigationSessionHeadingResolver.Selection heading = selectHeading(location, false, 180.0, 5f, 1_000L);
        assertEquals(84.0, heading.headingDegrees, 0.0);
        assertEquals(12f, heading.headingAccuracyDegrees, 0f);
    }

    @Test
    public void movingWithoutReliableCourseFollowsLiveCompass() {
        NavigationLocation location = movingLocation(1.2f, 84f);
        location.setBearingAccuracyDegrees(40f);
        assertEquals(180.0, selectHeading(location, false, 180.0, 5f, 1_000L).headingDegrees, 0.0);
        NavigationSessionHeadingResolver.Selection held = selectHeading(location, false, 210.0, 5f, 2_000L);
        assertEquals(210.0, held.headingDegrees, 0.0);
        assertEquals(5f, held.headingAccuracyDegrees, 0f);
    }

    @Test
    public void movingWithoutNewCourseFallsBackToCompass() {
        NavigationLocation location = movingLocation(1.2f, 84f);
        selectHeading(location, false, 180.0, 5f, 1_000L);
        location.setBearingAccuracyDegrees(40f);
        assertEquals(210.0, selectHeading(location, false, 210.0, 5f, 2_000L).headingDegrees, 0.0);
    }

    @Test
    public void stoppingWithDifferentCompassHeadingDoesNotRotateDisplay() {
        NavigationLocation location = movingLocation(15f, 84f);
        selectHeading(location, false, 180.0, 5f, 1_000L);
        assertEquals(84.0, selectHeading(location, true, 180.0, 5f, 2_000L).headingDegrees, 0.0);
        assertEquals(84.0, selectHeading(location, true, 185.0, 5f, 3_000L).headingDegrees, 0.0);
        assertEquals(84.0, selectHeading(location, true, 180.0, 5f, 4_000L).headingDegrees, 0.0);
    }

    @Test
    public void substantialTurnDuringStopThenResumingUsesTravelHeadingAgain() {
        NavigationLocation location = movingLocation(15f, 84f);
        selectHeading(location, false, 180.0, 5f, 1_000L);
        selectHeading(location, true, 180.0, 5f, 2_000L);
        selectHeading(location, true, 220.0, 5f, 3_000L);
        assertEquals(220.0, selectHeading(location, true, 220.0, 5f, 4_000L).headingDegrees, 0.0);
        assertEquals(84.0, selectHeading(location, false, 220.0, 5f, 5_000L).headingDegrees, 0.0);
        assertEquals(84.0, selectHeading(location, true, 260.0, 5f, 6_000L).headingDegrees, 0.0);
    }

    @Test
    public void resetDoesNotLeakHeadingIntoNextSession() {
        NavigationLocation location = movingLocation(15f, 84f);
        selectHeading(location, false, 180.0, 5f, 1_000L);
        resolver.reset();
        assertEquals(180.0, selectHeading(location, true, 180.0, 5f, 2_000L).headingDegrees, 0.0);
    }

    @Test
    public void waitingForLocationUsesLiveStartupCompass() {
        assertEquals(180.0, selectHeading(null, false, 180.0, 5f, 1_000L).headingDegrees, 0.0);
        assertEquals(185.0, selectHeading(null, false, 185.0, 5f, 1_100L).headingDegrees, 0.0);
    }

    @Test
    public void stationaryStartupTracksSmallCompassTurnsImmediately() {
        NavigationLocation location = movingLocation(0f, 0f);
        assertEquals(180.0, selectHeading(location, true, 180.0, 5f, 1_000L).headingDegrees, 0.0);
        assertEquals(185.0, selectHeading(location, true, 185.0, 5f, 1_100L).headingDegrees, 0.0);
    }

    @Test
    public void stationaryCompassBecomesContinuousAfterActivationAndResumesAfterPoorAccuracy() {
        NavigationLocation location = movingLocation(15f, 84f);
        selectHeading(location, false, 180.0, 5f, 1_000L);
        selectHeading(location, true, 180.0, 5f, 2_000L);
        selectHeading(location, true, 220.0, 5f, 3_000L);
        selectHeading(location, true, 220.0, 5f, 4_000L);
        assertEquals(225.0, selectHeading(location, true, 225.0, 5f, 4_100L).headingDegrees, 0.0);
        NavigationSessionHeadingResolver.Selection poor = selectHeading(location, true, 240.0, 40f, 4_200L);
        assertEquals(225.0, poor.headingDegrees, 0.0);
        assertEquals(90f, poor.headingAccuracyDegrees, 0f);
        assertEquals(230.0, selectHeading(location, true, 230.0, 5f, 4_300L).headingDegrees, 0.0);
    }

    @Test
    public void oldMovingFixFallsBackToCompassAndNextAccurateFixRestoresCourse() {
        NavigationLocation location = movingLocation(15f, 84f);
        selectHeading(location, false, 180.0, 5f, 1_000L);
        NavigationSessionHeadingResolver.Selection held = selectHeading(location, false, 210.0, 5f, 60_000L);
        assertEquals(210.0, held.headingDegrees, 0.0);
        assertEquals(5f, held.headingAccuracyDegrees, 0f);
        location.setTime(61_000L, 61_000L);
        location.setBearing(100f);
        NavigationSessionHeadingResolver.Selection fresh = selectHeading(location, false, 210.0, 5f, 61_000L);
        assertEquals(100.0, fresh.headingDegrees, 0.0);
        assertEquals(12f, fresh.headingAccuracyDegrees, 0f);
    }

    @Test
    public void unusableStartupCompassLeavesHeadingUnknown() {
        assertNull(selectHeading(null, false, 180.0, 40f, 1_000L).headingDegrees);
    }

    @Test
    public void noisyLongIntervalFixPreservesActivatedCompassUntilConfirmedSlowMovement() {
        accept(1_000L, 0.0, 15f, 5f, 84f);
        assertEquals(84.0, selectCurrent(180.0, 1_000L).headingDegrees, 0.0);
        accept(6_000L, 0.0, 0f, 5f, 84f);
        assertTrue(locationState.isLikelyStationary());
        selectCurrent(180.0, 6_000L);
        selectCurrent(220.0, 7_000L);
        assertEquals(220.0, selectCurrent(220.0, 8_000L).headingDegrees, 0.0);

        accept(66_000L, 2.0, 0.5f, 80f, 10f);
        assertTrue(locationState.isLikelyStationary());
        assertEquals(225.0, selectCurrent(225.0, 66_000L).headingDegrees, 0.0);
        assertEquals(228.0, selectCurrent(228.0, 66_100L).headingDegrees, 0.0);

        NavigationLocation moving = sample(126_000L, 3.0, 0.5f, 80f, 130f);
        moving.setSpeedAccuracyMetersPerSecond(0.05f);
        assertFalse(locationState.onRawLocationChanged(moving, 126_000L, false, 60_000L).isDropped());
        assertFalse(locationState.isLikelyStationary());
        assertEquals(130.0, selectCurrent(250.0, 126_000L).headingDegrees, 0.0);
        assertEquals(130.0, selectCurrent(270.0, 126_100L).headingDegrees, 0.0);
    }

    @Test
    public void poorStartupFixWithSmallVelocityContinuesLiveCompass() {
        accept(1_000L, 0.0, 0.5f, 80f, 84f);
        assertTrue(locationState.isLikelyStationary());
        assertEquals(180.0, selectCurrent(180.0, 1_000L).headingDegrees, 0.0);
        accept(61_000L, 5.0, 0.6f, 80f, 84f);
        assertEquals(185.0, selectCurrent(185.0, 61_000L).headingDegrees, 0.0);
    }

    @Test
    public void freshReliableWalkingBearingReplacesIncomingGpsHeadingOnFirstReversedFix() {
        accept(1_000L, 0.0, 1.4f, 5f, 353f);
        assertEquals(353.0, selectCurrent(103.0, 1_000L).headingDegrees, 0.0);
        accept(4_000L, 0.0, 0f, 5f, 353f);
        assertTrue(locationState.isLikelyStationary());
        selectCurrent(103.0, 4_000L);
        NavigationLocation reverse = sample(7_000L, -1.0, 0.37f, 6f, 147f);
        reverse.setSpeedAccuracyMetersPerSecond(0.1f);
        assertFalse(locationState.onRawLocationChanged(reverse, 7_000L, false).isDropped());
        assertFalse(locationState.isLikelyStationary());
        assertEquals(147.0, selectCurrent(103.0, 7_000L).headingDegrees, 0.0);
        assertEquals(147.0, selectCurrent(103.0, 7_100L).headingDegrees, 0.0);
    }

    @Test
    public void unknownBearingAccuracyFallsBackToCompassEvenAtHighSpeed() {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(1_000L, 1_000L);
        location.setSpeed(15f);
        location.setBearing(84f);
        assertEquals(180.0, selectHeading(location, false, 180.0, 5f, 1_000L).headingDegrees, 0.0);
    }

    @Test
    public void unusableCompassAndCourseHoldLastTravelHeadingWithUncertainty() {
        NavigationLocation location = movingLocation(15f, 84f);
        selectHeading(location, false, 180.0, 5f, 1_000L);
        location.setBearingAccuracyDegrees(40f);
        NavigationSessionHeadingResolver.Selection held = selectHeading(location, false, 210.0, 40f, 2_000L);
        assertEquals(84.0, held.headingDegrees, 0.0);
        assertEquals(90f, held.headingAccuracyDegrees, 0f);
    }

    private NavigationSessionHeadingResolver.Selection selectHeading(NavigationLocation location,
            boolean stationary, Double compassDegrees, Float compassAccuracy, long nowMs) {
        return resolver.selectHeading(location, stationary, compassDegrees, compassAccuracy, nowMs, null, true);
    }

    private NavigationSessionHeadingResolver.Selection selectCurrent(double compassDegrees, long nowMs) {
        return selectHeading(locationState.getLastFilteredLocation(), locationState.isLikelyStationary(),
                compassDegrees, 5f, nowMs);
    }

    private void accept(long timeMs, double distanceMeters, float speed, float accuracy, float bearing) {
        NavigationLocation location = sample(timeMs, distanceMeters, speed, accuracy, bearing);
        assertFalse(locationState.onRawLocationChanged(location, timeMs, false, 60_000L).isDropped());
    }

    private static NavigationLocation sample(long timeMs, double distanceMeters, float speed,
            float accuracy, float bearing) {
        NavigationLocation location = movingLocation(speed, bearing);
        location.setTime(timeMs, timeMs);
        location.setLatitude(48.2082 + distanceMeters / 111_320.0);
        location.setAccuracy(accuracy);
        return location;
    }

    private static NavigationLocation movingLocation(float speedMps, float bearingDegrees) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(1_000L, 1_000L);
        location.setLatitude(48.2082);
        location.setLongitude(16.3738);
        location.setAccuracy(5f);
        location.setSpeed(speedMps);
        location.setBearing(bearingDegrees);
        location.setBearingAccuracyDegrees(12f);
        return location;
    }
}
