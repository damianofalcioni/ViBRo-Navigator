package vibro.navigator.nav.session;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import static org.junit.Assert.*;

public class NavigationBeelineArrivalOrderingTest extends NavigationSessionRouteStateTestSupport {
    private static final LatLon ROAD = new LatLon(0, .001);
    private static final LatLon FIRST = new LatLon(.00005, .001);
    private static final LatLon SECOND = new LatLon(.0001, .001);

    @Test
    public void confirmingOneStopCannotCompleteTheFollowingNearbyStopBeeline() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION,
                new LatLon(0, .003), Arrays.asList(FIRST, SECOND));
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), ROAD, FIRST, ROAD,
                SECOND, ROAD, request.destination), Arrays.asList(new VoiceHint(1, 16, 0, 6, 0),
                new VoiceHint(2, 16, 0, 6, 0), new VoiceHint(3, 16, 0, 12, 0),
                new VoiceHint(4, 16, 0, 12, 0)), 200, 366);
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), route,
                location(0, 0, 1_000), 0, 500);

        evaluate(state, 2_000);
        assertEquals(101, evaluate(state, 5_000).turnEvents.get(0).hint.command);
        assertEquals(1, state.remainingIntermediateStops(request.stops).size());
        assertTrue("The next stop must retain its direct guidance until independently confirmed",
                state.isBeelineGuidanceActive());
        assertTrue(evaluate(state, 6_000).turnEvents.isEmpty());
        assertEquals(101, evaluate(state, 9_000).turnEvents.get(0).hint.command);
        assertTrue(state.remainingIntermediateStops(request.stops).isEmpty());
    }

    @Test
    public void confirmingTheLastStopCannotCompleteANearbyFinalBeeline() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION,
                SECOND, Collections.singletonList(FIRST));
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), ROAD, FIRST, SECOND),
                Arrays.asList(new VoiceHint(1, 16, 0, 6, 0), new VoiceHint(2, 16, 0, 6, 0)), 100, 123);
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), route,
                location(0, 0, 1_000), 0, 500);

        evaluate(state, 2_000);
        assertEquals(101, evaluate(state, 5_000).turnEvents.get(0).hint.command);
        assertFalse(state.isDestinationReached());
        assertTrue("The final beeline needs its own confirmation window", state.isBeelineGuidanceActive());
        assertTrue(evaluate(state, 6_000).turnEvents.isEmpty());
        assertEquals(100, evaluate(state, 9_000).turnEvents.get(0).hint.command);
    }

    @Test
    public void colocatedStopsAndFinalBeelineKeepSeparateConfirmationWindows() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION,
                FIRST, Arrays.asList(FIRST, FIRST));
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), ROAD, FIRST, ROAD, FIRST, FIRST),
                Arrays.asList(new VoiceHint(1, 16, 0, 6, 0), new VoiceHint(2, 16, 0, 6, 0),
                        new VoiceHint(3, 16, 0, 6, 0), new VoiceHint(4, 16, 0, 0, 0)), 100, 129);
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), route,
                location(0, 0, 1_000), 0, 500);

        evaluate(state, 2_000);
        assertEquals(101, evaluate(state, 5_000).turnEvents.get(0).hint.command);
        assertEquals(1, state.remainingIntermediateStops(request.stops).size());
        assertTrue(state.isBeelineGuidanceActive());
        assertTrue(evaluate(state, 6_000).turnEvents.isEmpty());
        assertEquals(101, evaluate(state, 9_000).turnEvents.get(0).hint.command);
        assertTrue(state.remainingIntermediateStops(request.stops).isEmpty());
        assertTrue(state.isBeelineGuidanceActive());
        assertFalse(state.isDestinationReached());
        assertTrue(evaluate(state, 10_000).turnEvents.isEmpty());
        assertEquals(100, evaluate(state, 13_000).turnEvents.get(0).hint.command);
        assertTrue(state.isDestinationReached());
    }

    private static NavigationRouteEvaluation evaluate(NavigationSessionRouteState state, long time) {
        return state.evaluateLocation(location(ROAD.lat, ROAD.lon, time), 0, 5, null, time, 0);
    }
}
