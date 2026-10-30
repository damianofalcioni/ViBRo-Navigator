package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.model.NavigationRequest;

public class NavigationArrivalOrderingTest extends NavigationSessionRouteStateTestSupport {
    @Test
    public void colocatedStopIsConfirmedBeforeFinalArrival() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        LatLon destination = new LatLon(0, 0.003);
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION,
                destination, Collections.singletonList(destination));
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), routeWithoutHints(),
                location(0, 0, 1_000), 5, 500);

        assertTrue(evaluate(state, 2_000).turnEvents.isEmpty());
        NavigationRouteEvaluation stop = evaluate(state, 5_000);
        assertEquals(1, stop.turnEvents.size());
        assertEquals(101, stop.turnEvents.get(0).hint.command);
        assertTrue(evaluate(state, 6_000).turnEvents.isEmpty());
        NavigationRouteEvaluation arrival = evaluate(state, 9_000);
        assertEquals(1, arrival.turnEvents.size());
        assertEquals(100, arrival.turnEvents.get(0).hint.command);
    }

    private static NavigationRouteEvaluation evaluate(NavigationSessionRouteState state, long time) {
        return state.evaluateLocation(location(0, 0.003, time), 0, 5, 90.0, time, 0);
    }
}
