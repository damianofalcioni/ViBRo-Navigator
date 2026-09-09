package vibro.navigator.nav.session;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import static org.junit.Assert.*;

public class RouteStartDivergenceIntegrationTest {
    @Test
    public void routeModeRequestsRecoveryAfterSustainedMovementAwayFromStart() {
        NavigationSessionRouteState state = startedState();
        assertFalse(evaluate(state, 0, 1_000).shouldSpeculativelyRecalculateRoute());
        for (long now = 4_000; now < 16_000; now += 3_000) {
            assertFalse(evaluate(state, -0.002, now).shouldSpeculativelyRecalculateRoute());
        }
        NavigationRouteEvaluation recovery = evaluate(state, -0.002, 16_000);
        assertFalse(recovery.shouldRecalculateRoute());
        assertTrue(recovery.shouldSpeculativelyRecalculateRoute());
        assertFalse(recovery.shouldCancelSpeculativeRouteRecalculation());
        assertEquals(NavigationRouteRecalculationReason.BEELINE_RECOVERY, recovery.recalculationReason);
    }

    @Test
    public void clearingMotionEvidenceRetainsApproachButCancelsPendingRecovery() {
        NavigationSessionRouteState state = startedState();
        evaluate(state, 0, 1_000);
        evaluate(state, -0.002, 4_000);
        evaluate(state, -0.002, 7_000);
        state.clearMotionEvidence();
        for (long now = 10_000; now <= 28_000; now += 3_000) {
            assertFalse(evaluate(state, -0.002, now).shouldSpeculativelyRecalculateRoute());
        }
    }

    private static NavigationSessionRouteState startedState() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRouteRequestSnapshot snapshot = new NavigationRouteRequestSnapshot(
                1, 1, new LatLon(0, 0), Collections.emptyList(), new LatLon(0, 0.003),
                "trekking", null, Collections.emptyList());
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0.001), new LatLon(0, 0.003)),
                Collections.emptyList(), 60, 222);
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot, route, fix(0, 1_000), 5, 500);
        return state;
    }

    private static NavigationRouteEvaluation evaluate(NavigationSessionRouteState state, double lon, long now) {
        return state.evaluateLocation(fix(lon, now), 5, false, 20, 270.0, now, 0, false);
    }

    private static NavigationLocation fix(double lon, long now) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(0);
        location.setLongitude(lon);
        location.setTime(now);
        // Exercise divergence independently of the existing <=12 m startup-refresh policy.
        location.setAccuracy(20);
        location.setSpeed(5);
        return location;
    }
}
