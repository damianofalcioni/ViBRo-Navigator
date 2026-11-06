package vibro.navigator.nav.guidance;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;
import vibro.navigator.nav.location.NavigationMotionState;
import static org.junit.Assert.*;

public class NavigationRouteEtaHistoryTest {
    private final NavigationRouteEtaHistory history = new NavigationRouteEtaHistory();

    @Test
    public void variableDeliveryDelayDoesNotInventSpeedOrAcceleration() {
        record(1_000, 0, 2_000, NavigationMotionState.MOVING);
        record(4_000, 3, 5_500, NavigationMotionState.MOVING);
        RouteMotionEstimate estimate = record(7_000, 6, 7_500, NavigationMotionState.MOVING);
        assertEquals(1, estimate.speedMps, 0);
        assertEquals(0, estimate.accelerationMps2, 0);
    }

    @Test
    public void accelerationUsesIndependentObservationIntervals() {
        record(1_000, 0, 2_500, NavigationMotionState.MOVING);
        record(4_000, 6, 4_500, NavigationMotionState.MOVING);
        RouteMotionEstimate estimate = record(7_000, 18, 8_000, NavigationMotionState.MOVING);
        assertEquals(4, estimate.speedMps, 0);
        assertEquals(2f / 3, estimate.accelerationMps2, 0.0001);
    }

    @Test
    public void aReversalInsideTheMinimumSampleIntervalImmediatelyInvalidatesForwardEta() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        record(4_000, 6, 4_000, NavigationMotionState.MOVING);
        record(7_000, 12, 7_000, NavigationMotionState.MOVING);
        assertUnavailable(history.resolve(fix(8_000), 9, 2, true, 8_000));
        assertUnavailable(record(8_000, 9, 8_000, NavigationMotionState.MOVING));
        assertUnavailable(record(9_000, 8, 9_000, NavigationMotionState.MOVING));
        RouteMotionEstimate resumed = record(11_000, 12, 11_000, NavigationMotionState.MOVING);
        assertEquals(2, resumed.speedMps, 0);
        assertTrue(Float.isNaN(resumed.accelerationMps2));
    }

    @Test
    public void aBriefReversalCannotReuseAnEarlierAccelerationAnchor() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        record(4_000, 6, 4_000, NavigationMotionState.MOVING);
        record(7_000, 18, 7_000, NavigationMotionState.MOVING);
        assertUnavailable(record(8_000, 17, 8_000, NavigationMotionState.MOVING));
        assertUnavailable(record(9_000, 18, 9_000, NavigationMotionState.MOVING));
        RouteMotionEstimate resumed = record(10_000, 19, 10_000, NavigationMotionState.MOVING);
        assertEquals(1, resumed.speedMps, 0);
        assertTrue(Float.isNaN(resumed.accelerationMps2));
    }

    @Test
    public void anOlderPositionCannotDiscardTheCurrentEtaAnchors() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        record(4_000, 6, 4_000, NavigationMotionState.MOVING);
        record(7_000, 12, 7_000, NavigationMotionState.MOVING);
        record(6_000, 10, 7_000, NavigationMotionState.MOVING);
        RouteMotionEstimate continued = record(10_000, 18, 10_000, NavigationMotionState.MOVING);
        assertEquals(2, continued.speedMps, 0);
        assertEquals(0, continued.accelerationMps2, 0);
    }

    @Test
    public void unknownObservationInvalidatesOldTravelAndCannotAnchorRecovery() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        assertEquals(1, record(4_000, 3, 4_000, NavigationMotionState.MOVING).speedMps, 0);
        assertUnavailable(record(7_000, 15, 7_000, NavigationMotionState.UNKNOWN));
        assertUnavailable(record(10_000, 16, 10_000, NavigationMotionState.MOVING));
        assertEquals(1, record(13_000, 19, 13_000, NavigationMotionState.MOVING).speedMps, 0);
    }

    @Test
    public void confirmedStopDiscardsLiveTravelAndCannotAnchorDeparture() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        record(4_000, 3, 4_000, NavigationMotionState.STATIONARY);
        assertUnavailable(record(7_000, 6, 7_000, NavigationMotionState.MOVING));
    }

    @Test
    public void sparseMovingObservationsSurviveExpiredDisplayReads() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        record(4_000, 3, 4_000, NavigationMotionState.MOVING);
        assertUnavailable(history.resolve(fix(4_000), 3, 2, false, 20_000));
        assertUnavailable(history.resolve(fix(4_000), 3, 2, true, 20_000));
        RouteMotionEstimate sparse = record(64_000, 63, 65_000, NavigationMotionState.MOVING);
        assertEquals(1, sparse.speedMps, 0);
        assertTrue(Float.isNaN(sparse.accelerationMps2));
    }

    @Test
    public void staleAndFutureObservationsCannotOpenLiveEtaHistory() {
        assertUnavailable(record(1_000, 0, 12_000, NavigationMotionState.MOVING));
        assertUnavailable(record(13_000, 12, 13_000, NavigationMotionState.MOVING));
        assertUnavailable(record(16_000, 15, 15_000, NavigationMotionState.MOVING));
        assertUnavailable(record(19_000, 18, 19_000, NavigationMotionState.MOVING));
    }

    @Test
    public void providerChangeRequiresNewProviderLocalProgress() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        NavigationLocation network = fix(4_000);
        network.setProvider(NavigationLocationProviders.NETWORK_PROVIDER);
        assertUnavailable(history.record(network, 100, 2, true, 4_000));
        network.setTime(7_000, 7_000);
        assertEquals(1, history.record(network, 103, 2, true, 7_000).speedMps, 0);
    }

    @Test
    public void directionAndSyntheticProgressCannotBecomeEtaAnchors() {
        NavigationRouteProgressTracker tracker = new NavigationRouteProgressTracker();
        tracker.rememberAlongTrackSample(0, 2_000);
        tracker.rememberAlongTrackSample(100, 5_000);
        assertUnavailable(tracker.etaHistory().record(fix(7_000), 110, 2, true, 8_000));
        assertEquals(NavigationRouteProgressTracker.DirectionStatus.FORWARD,
                tracker.assessDirection(110, 8_000).status);
    }

    @Test
    public void untrustedRouteProgressCannotSupplyRecoverySpeed() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        history.recordTrustedProgress(fix(4_000), 100, 2, true, 4_000, false);
        assertUnavailable(record(7_000, 103, 7_000, NavigationMotionState.MOVING));
    }

    @Test
    public void duplicateCallbacksDoNotAddAnAccelerationAnchor() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        record(1_000, 0, 2_000, NavigationMotionState.MOVING);
        RouteMotionEstimate estimate = record(4_000, 3, 4_000, NavigationMotionState.MOVING);
        assertEquals(1, estimate.speedMps, 0);
        assertTrue(Float.isNaN(estimate.accelerationMps2));
    }

    @Test
    public void unexpectedGapAndResetDiscardOldEtaAnchors() {
        record(1_000, 0, 1_000, NavigationMotionState.MOVING);
        assertUnavailable(record(67_000, 66, 67_000, NavigationMotionState.MOVING));
        history.reset();
        assertUnavailable(record(70_000, 69, 70_000, NavigationMotionState.MOVING));
    }

    @Test
    public void normalSparseCadencesRetainMeasurementBasedSpeedWithDeliveryDelay() {
        for (long interval : new long[] {12_000, 20_000, 30_000, 60_000}) {
            history.reset();
            record(1_000, 0, 2_000, NavigationMotionState.MOVING);
            RouteMotionEstimate estimate = record(1_000 + interval, interval / 1_000.0,
                    3_000 + interval, NavigationMotionState.MOVING);
            assertEquals(1, estimate.speedMps, 0);
            assertTrue(Float.isNaN(estimate.accelerationMps2));
        }
    }

    private RouteMotionEstimate record(long measuredMs, double along, long receiptMs, NavigationMotionState state) {
        return history.record(fix(measuredMs), along, 2, state.isMoving(), receiptMs);
    }

    private static NavigationLocation fix(long time) {
        NavigationLocation location = new NavigationLocation(NavigationLocationProviders.GPS_PROVIDER);
        location.setTime(time, time);
        return location;
    }

    private static void assertUnavailable(RouteMotionEstimate estimate) {
        assertEquals(0, estimate.speedMps, 0);
        assertTrue(Float.isNaN(estimate.accelerationMps2));
    }
}
