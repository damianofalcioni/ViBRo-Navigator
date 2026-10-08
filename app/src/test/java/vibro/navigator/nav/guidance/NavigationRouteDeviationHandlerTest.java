package vibro.navigator.nav.guidance;

import org.junit.Test;

import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker.DirectionAssessment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationRouteDeviationHandlerTest {
    @Test
    public void loggedRejoinHeadingsWithStalledProgressDoNotRequestConfirmationOrReroute() {
        NavigationRouteProgressTracker progress = new NavigationRouteProgressTracker();
        NavigationRouteDeviationHandler handler = new NavigationRouteDeviationHandler(progress);
        progress.rememberAlongTrackSample(100.0, 13_336_459L);
        assertHeld(handler.evaluate(match(5.863368, 101.472975), 5.681934,
                progress.assessDirection(101.472975, 13_339_598L),
                75.280714, 164.97888, 13_339_598L));
        assertHeld(handler.evaluate(match(4.5, 101.739377), 5.2,
                progress.assessDirection(101.739377, 13_340_643L),
                75.280714, 161.3681, 13_340_643L));
    }

    @Test
    public void repeatedUnknownStalledAndForwardMismatchesCannotConfirmWrongDirection() {
        for (DirectionAssessment direction : nonBackwardDirections()) {
            NavigationRouteDeviationHandler handler = newHandler();
            for (long now : new long[]{1_000L, 2_000L, 4_000L, 10_000L}) {
                NavigationRouteDeviationHandler.Decision decision = mismatch(handler, direction, now);
                assertTrue(decision.shouldKeepCurrentRoute());
                assertFalse(decision.shouldRecalculateRoute());
                assertFalse(decision.isDeviationConfirmationPending());
            }
        }
    }

    @Test
    public void backwardMismatchesStillRequireTwoTimeSeparatedSamples() {
        NavigationRouteDeviationHandler handler = newHandler();
        assertTrue(mismatch(handler, DirectionAssessment.backward(-5.0), 1_000L)
                .isDeviationConfirmationPending());
        assertFalse(mismatch(handler, DirectionAssessment.backward(-6.0), 1_749L)
                .shouldRecalculateRoute());
        NavigationRouteDeviationHandler.Decision confirmed =
                mismatch(handler, DirectionAssessment.backward(-7.0), 1_750L);
        assertTrue(confirmed.shouldRecalculateRoute());
        assertEquals(RouteDeviationPolicy.Reason.BEARING_MISMATCH, confirmed.getRerouteNotice().reason);
    }

    @Test
    public void nonBackwardSampleClearsEarlierBackwardMismatchEvidence() {
        for (DirectionAssessment direction : nonBackwardDirections()) {
            NavigationRouteDeviationHandler handler = newHandler();
            assertTrue(mismatch(handler, DirectionAssessment.backward(-5.0), 1_000L)
                    .isDeviationConfirmationPending());
            assertFalse(mismatch(handler, direction, 2_000L).isDeviationConfirmationPending());
            assertFalse(mismatch(handler, DirectionAssessment.backward(-5.0), 3_000L)
                    .shouldRecalculateRoute());
            assertTrue(mismatch(handler, DirectionAssessment.backward(-6.0), 3_750L)
                    .shouldRecalculateRoute());
        }
    }

    @Test
    public void backwardProgressWithoutTrustedMismatchDoesNotReroute() {
        for (Double bearing : new Double[]{null, 90.0}) {
            NavigationRouteDeviationHandler handler = newHandler();
            for (long now : new long[]{1_000L, 2_000L}) {
                assertFalse(handler.evaluate(match(5.0, 100.0), 5.0,
                        DirectionAssessment.backward(-5.0), 90.0, bearing, now).shouldRecalculateRoute());
            }
        }
    }

    @Test
    public void offTrackDetectionStillConfirmsRegardlessOfProgressStatus() {
        for (DirectionAssessment direction : nonBackwardDirections()) {
            NavigationRouteDeviationHandler handler = newHandler();
            assertTrue(handler.evaluate(match(30.0, 100.0), 5.0, direction, 90.0, 180.0, 1_000L)
                    .isDeviationConfirmationPending());
            NavigationRouteDeviationHandler.Decision confirmed =
                    handler.evaluate(match(30.0, 100.0), 5.0, direction, 90.0, 180.0, 1_750L);
            assertTrue(confirmed.shouldRecalculateRoute());
            assertEquals(RouteDeviationPolicy.Reason.OFF_TRACK, confirmed.getRerouteNotice().reason);
        }
    }

    private static DirectionAssessment[] nonBackwardDirections() {
        return new DirectionAssessment[]{DirectionAssessment.unknown(),
                DirectionAssessment.stalled(1.5), DirectionAssessment.stalled(-3.9),
                DirectionAssessment.forward(5.0)};
    }

    private static NavigationRouteDeviationHandler newHandler() {
        return new NavigationRouteDeviationHandler(new NavigationRouteProgressTracker());
    }

    private static PolylineIndex.Match match(double distance, double progress) {
        return new PolylineIndex.Match(distance, progress, 90.0, 0);
    }

    private static NavigationRouteDeviationHandler.Decision mismatch(
            NavigationRouteDeviationHandler handler, DirectionAssessment direction, long now) {
        return handler.evaluate(match(5.0, 100.0), 5.0, direction, 90.0, 180.0, now);
    }

    private static void assertHeld(NavigationRouteDeviationHandler.Decision decision) {
        assertTrue(decision.shouldKeepCurrentRoute());
        assertFalse(decision.isStableOnRouteSample());
        assertFalse(decision.isDeviationConfirmationPending());
        assertFalse(decision.shouldRecalculateRoute());
    }
}
