package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RouteDeviationPolicyTest {

    private final RouteDeviationPolicy policy = new RouteDeviationPolicy();

    @Test
    public void evaluate_requestsRerouteWhenDistanceExceedsAccuracyAdjustedThreshold() {
        RouteDeviationPolicy.Decision decision = policy.evaluate(25.5, 10f, null, 90.0);

        assertEquals(RouteDeviationPolicy.Reason.OFF_TRACK, decision.reason);
        assertTrue(decision.shouldRecalculateRoute());
        assertEquals(25.5, decision.distanceToTrackMeters, 0.0);
        assertEquals(20.0, decision.offTrackThresholdMeters, 0.0);
    }

    @Test
    public void evaluate_requestsRerouteWhenBearingMismatchIsTooLarge() {
        RouteDeviationPolicy.Decision decision = policy.evaluate(5.0, 5f, 180.0, 90.0);

        assertEquals(RouteDeviationPolicy.Reason.BEARING_MISMATCH, decision.reason);
        assertTrue(decision.shouldRecalculateRoute());
        assertEquals(5.0, decision.distanceToTrackMeters, 0.0);
        assertEquals(90.0, decision.bearingDiffDegrees, 0.0);
        assertEquals(90.0, decision.expectedBearingDegrees, 0.0);
        assertEquals(180.0, decision.actualBearingDegrees, 0.0);
    }

    @Test
    public void evaluate_keepsCurrentRouteWhenWithinThresholds() {
        RouteDeviationPolicy.Decision decision = policy.evaluate(8.0, 5f, 100.0, 90.0);

        assertEquals(RouteDeviationPolicy.Reason.NONE, decision.reason);
        assertFalse(decision.shouldRecalculateRoute());
        assertEquals(15.0, decision.offTrackThresholdMeters, 0.0);
        assertEquals(10.0, decision.bearingDiffDegrees, 0.0);
    }
}
