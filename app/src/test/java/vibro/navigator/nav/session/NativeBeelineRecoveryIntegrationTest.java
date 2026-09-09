package vibro.navigator.nav.session;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import static org.junit.Assert.*;

public class NativeBeelineRecoveryIntegrationTest {
    @Test
    public void intermediateAndFinalBeelinesCanRecoverAtWalkingSpeed() {
        for (boolean intermediate : new boolean[] {true, false}) {
            NavigationSessionRouteState state = state(intermediate, NavigationRoutingMode.BROUTER);
            assertFalse(evaluate(state, 0).shouldSpeculativelyRecalculateRoute());
            for (int step = 1; step < 10; step++) {
                assertFalse(evaluate(state, step).shouldSpeculativelyRecalculateRoute());
            }
            NavigationRouteEvaluation recovery = evaluate(state, 10);
            assertTrue(recovery.shouldSpeculativelyRecalculateRoute());
            assertFalse(recovery.shouldRecalculateRoute());
            assertFalse(recovery.shouldCancelSpeculativeRouteRecalculation());
            assertEquals(intermediate ? 1 : 0, state.remainingIntermediateStops(Collections.emptyList()).size());
            assertEquals(11, state.historyForExport().recalculationBridgeSegmentsSnapshot().get(0).size());
        }
    }

    @Test
    public void roundTripNativeBeelineKeepsItsNoReroutingRule() {
        NavigationSessionRouteState state = state(false, NavigationRoutingMode.ROUND_TRIP);
        for (int step = 0; step <= 20; step++) {
            NavigationRouteEvaluation evaluation = evaluate(state, step);
            assertFalse(evaluation.shouldSpeculativelyRecalculateRoute());
            assertFalse(evaluation.shouldRecalculateRoute());
        }
    }

    @Test
    public void reachingTheTargetInvalidatesAnOutstandingRecoveryContext() {
        NavigationSessionRouteState state = state(true, NavigationRoutingMode.BROUTER);
        evaluate(state, 0);
        long context = state.beelineRecovery().context();
        NavigationLocation reached = fix(0.001, 4_000);
        state.evaluateLocation(reached, 1.4f, false, 5, 90.0, 4_000, 0, false);
        assertFalse(state.beelineRecovery().isCurrent(context, 4_000));
        assertTrue(state.remainingIntermediateStops(Collections.emptyList()).isEmpty());
    }

    private static NavigationSessionRouteState state(boolean intermediate, NavigationRoutingMode mode) {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        LatLon stop = new LatLon(0, 0.001);
        LatLon destination = intermediate ? new LatLon(0, 0.003) : stop;
        List<LatLon> points = intermediate ? Arrays.asList(new LatLon(0, 0), stop, destination)
                : Arrays.asList(new LatLon(0, 0), destination);
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(1, 1, mode,
                new LatLon(0, 0), intermediate ? Collections.singletonList(stop) : Collections.emptyList(),
                destination, "trekking", null, Collections.emptyList(), 1_000);
        GeoJsonRoute route = new GeoJsonRoute(points, Collections.singletonList(new VoiceHint(0, 16, 0, 111, 0)),
                200, 333);
        state.applyRouteResult(TestNavigationTextResources.metric(), request, route, fix(0, 1_000), 1.4f, 500);
        return state;
    }

    private static NavigationRouteEvaluation evaluate(NavigationSessionRouteState state, int step) {
        long now = 1_000 + step * 3_000L;
        NavigationLocation fix = fix(-step * 0.00005, now);
        NavigationRouteEvaluation evaluation = state.evaluateLocation(fix, 1.4f, false, 5, 270.0, now, 0, false);
        state.recordRecalculationFixPath(fix, evaluation, false);
        return evaluation;
    }

    private static NavigationLocation fix(double lon, long now) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setLatitude(0);
        fix.setLongitude(lon);
        fix.setAccuracy(5);
        fix.setTime(now);
        return fix;
    }
}
