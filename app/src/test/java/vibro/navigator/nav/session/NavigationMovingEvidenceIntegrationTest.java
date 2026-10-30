package vibro.navigator.nav.session;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import static org.junit.Assert.*;

public class NavigationMovingEvidenceIntegrationTest {
    @Test
    public void preciseSlowSpeedWithUnconfirmedPositionsStopsStationaryAdviceAndAccountingInBothModes() {
        for (boolean routed : new boolean[] {false, true}) {
            NavigationSession session = start(routed);
            NavigationLocation stopped = fix(4_000, 0);
            stopped.setSpeed(0);
            accept(session, stopped);
            stopped.setTime(7_000, 7_000);
            accept(session, stopped);
            assertTrue(session.isLikelyStationaryForOrientation(7_000));
            long stationaryMs = -1;
            for (int i = 0; i <= 10; i++) {
                long time = 10_000 + i * 3_000L;
                NavigationLocation slow = fix(time, 0);
                slow.setSpeed(0.25f);
                slow.setSpeedAccuracyMetersPerSecond(0.01f);
                NavigationLocationUpdateResult result = accept(session, slow);
                assertFalse(result.isDropped());
                assertUnknown(session, time);
                assertEquals(3_000, result.getSuggestedUpdateIntervalMs());
                var stats = session.components.tripStatsTracker.snapshot();
                if (i == 0) {
                    stationaryMs = stats.stationaryDurationMs;
                }
                assertEquals(stationaryMs, stats.stationaryDurationMs(time + 3_000));
                assertEquals(0, stats.movingDurationMs);
                assertEquals(0, stats.travelledDistanceMeters, 0);
            }
        }
    }

    @Test
    public void delayedSpeedCannotPermanentlyInflateTheTripMaximumInEitherMode() {
        for (boolean routed : new boolean[] {false, true}) {
            NavigationSession session = start(routed);
            for (int i = 1; i <= 10; i++) {
                accept(session, fix(1_000 + i * 3_000L, i * 3));
            }
            float previousMaximum = session.components.tripStatsTracker.snapshot().maxSpeedMps;
            assertEquals(1f, previousMaximum, 0);
            NavigationLocation delayed = fix(34_000, 36);
            delayed.setSpeed(20);
            delayed.setSpeedAccuracyMetersPerSecond(0.1f);
            NavigationLocationUpdateResult result = NavigationSessionResourceAdapter.onRawLocationChanged(
                    session, TestNavigationTextResources.metric(), delayed, 45_000, 3_000);
            assertFalse(result.isDropped());
            assertFalse(session.components.locationState.isMotionKnown(45_000));
            assertEquals(previousMaximum, session.components.tripStatsTracker.snapshot().maxSpeedMps, 0);
        }
    }

    @Test
    public void preciseSlowWalkingEstablishesMovingTripEvidenceInBothModes() {
        for (boolean routed : new boolean[] {false, true}) {
            NavigationSession session = start(routed);
            for (int i = 1; i <= 30; i++) {
                long time = 1_000 + i * 3_000L;
                NavigationLocation location = fix(time, i * 0.9);
                location.setSpeed(0.3f);
                location.setSpeedAccuracyMetersPerSecond(0.01f);
                assertFalse(accept(session, location).isDropped());
                if (i >= 15) {
                    assertTrue(session.components.locationState.motionState(time).isMoving());
                    assertFalse(session.isLikelyStationaryForOrientation(time));
                }
            }
            assertTrue(session.components.tripStatsTracker.snapshot().movingDurationMs > 0);
            assertTrue(session.components.tripStatsTracker.snapshot().travelledDistanceMeters > 0);
        }
    }

    @Test
    public void slowRawDisplacementCannotSustainStationaryAdviceOrTripTimeInEitherMode() {
        for (boolean routed : new boolean[] {false, true}) {
            assertNoStopFromCreepingPositions(start(routed), false);
            assertNoStopFromCreepingPositions(start(routed), true);
        }
    }

    private static void assertNoStopFromCreepingPositions(NavigationSession session, boolean reportedStop) {
        for (int i = 1; i <= 30; i++) {
            long time = 1_000 + i * 1_000L;
            NavigationLocation location = new NavigationLocation("gps");
            location.setTime(time, time);
            location.setLongitude(i * 0.6 / 111_320.0);
            location.setAccuracy(5);
            if (reportedStop) {
                location.setSpeed(0);
            }
            NavigationLocationUpdateResult result = NavigationSessionResourceAdapter.onRawLocationChanged(
                    session, TestNavigationTextResources.metric(), location, time, 1_000);
            assertFalse(result.isDropped());
            if (i >= 6) {
                assertFalse(session.isLikelyStationaryForOrientation(time));
            }
        }
        assertTrue(session.components.tripStatsTracker.snapshot().stationaryDurationMs <= 3_000);
    }

    @Test
    public void conflictingPositionsRestoreVerificationAndStopMovingTotalsInBothModes() {
        for (boolean routed : new boolean[] {false, true}) {
            NavigationSession session = start(routed);
            for (int i = 0; i <= 30; i++) {
                accept(session, fix(1_000 + i * 3_000L, i * 3));
            }
            accept(session, fix(94_000, 90));
            assertUnknown(session, 94_000);
            long supportedMovingMs = session.components.tripStatsTracker.snapshot().movingDurationMs;
            long supportedStationaryMs = session.components.tripStatsTracker.snapshot().stationaryDurationMs;
            for (int i = 1; i <= 15; i++) {
                long time = 94_000 + i * 3_000L;
                NavigationLocationUpdateResult result = accept(session, fix(time, 90));
                assertUnknown(session, time);
                assertEquals(3_000, result.getSuggestedUpdateIntervalMs());
            }
            assertEquals(supportedMovingMs, session.components.tripStatsTracker.snapshot().movingDurationMs);
            assertEquals(supportedStationaryMs, session.components.tripStatsTracker.snapshot().stationaryDurationMs);
        }
    }

    @Test
    public void oneTrustedSpeedSpikeCannotOpenATripMovingInterval() {
        NavigationSession session = start(false);
        NavigationLocation stopped = fix(1_000, 0);
        stopped.setSpeed(0);
        accept(session, stopped);
        stopped.setTime(4_000, 4_000);
        accept(session, stopped);
        NavigationLocation spike = fix(7_000, 0);
        spike.setSpeed(2);
        spike.setSpeedAccuracyMetersPerSecond(0.1f);
        NavigationLocationUpdateResult result = accept(session, spike);
        assertUnknown(session, 7_000);
        assertEquals(3_000, result.getSuggestedUpdateIntervalMs());
        assertEquals(0, session.components.tripStatsTracker.snapshot().movingDurationMs(10_000));
    }

    private static void assertUnknown(NavigationSession session, long time) {
        assertFalse(session.components.locationState.isMotionKnown(time));
        assertFalse(session.isLikelyStationaryForOrientation(time));
        assertTrue(session.components.locationState.needsDepartureChecks());
    }

    @Test
    public void stoppedPositionCorrectionsCannotAddTravelInEitherNavigationMode() {
        for (boolean routed : new boolean[] {false, true}) {
            assertNoTravelFromStoppedCorrections(start(routed));
        }
    }

    private static void assertNoTravelFromStoppedCorrections(NavigationSession session) {
        for (int i = 0; i < 14; i++) {
            long time = 1_000 + i * 3_000L;
            NavigationLocation correction = fix(time, i < 2 || i % 2 != 0 ? 0 : 12);
            correction.setAccuracy(5);
            correction.setSpeed(0);
            NavigationLocationUpdateResult result = accept(session, correction);
            assertFalse(result.isDropped());
            if (i >= 2) {
                assertUnknown(session, time);
                assertEquals(3_000, result.getSuggestedUpdateIntervalMs());
            }
        }
        var stats = session.components.tripStatsTracker.snapshot();
        assertEquals(0, stats.movingDurationMs);
        assertEquals(0, stats.travelledDistanceMeters, 0);
    }

    private static NavigationSession start(boolean routed) {
        NavigationSession session = new NavigationSession();
        LatLon target = new LatLon(0, 0.1);
        session.loadRequest(routed
                ? new NavigationRequest("trekking", "Target", target, Collections.emptyList())
                : new NavigationRequest(NavigationRoutingMode.STRAIGHT_LINE, null, "Target", target, Collections.emptyList()));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        session.setActiveLocationProviders("gps");
        if (routed) {
            accept(session, fix(1_000, 0));
            var request = session.prepareRouteRequest(true, 1_000);
            NavigationSessionResourceAdapter.applyRouteResult(session, TestNavigationTextResources.metric(), request,
                    new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), target), Collections.emptyList(), 8_000, 11_132), 1_000);
        }
        return session;
    }

    private static NavigationLocationUpdateResult accept(NavigationSession session, NavigationLocation location) {
        return NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(),
                location, location.getElapsedRealtimeOrTimeMs(), 3_000);
    }

    private static NavigationLocation fix(long time, double east) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLongitude(east / 111_320.0);
        location.setAccuracy(13);
        location.setSpeed(1);
        return location;
    }
}
