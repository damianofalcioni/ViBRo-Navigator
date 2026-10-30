package vibro.navigator.nav.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import static org.junit.Assert.*;

public class NavigationArrivalFreshnessIntegrationTest extends NavigationSessionRouteStateTestSupport {
    private static final LatLon TARGET = new LatLon(0, 0.003);

    @Test
    public void delayedSelectedFixesCannotCompleteStraightLineArrival() {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(NavigationRoutingMode.STRAIGHT_LINE, null,
                DESTINATION, TARGET, Collections.emptyList()));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        session.setActiveLocationProviders("gps");
        assertStraightLineEvents(session, 1_000, 15_000, 0);
        assertStraightLineEvents(session, 4_000, 18_000, 0);
        assertStraightLineEvents(session, 19_000, 19_000, 0);
        assertStraightLineEvents(session, 22_000, 22_000, 1);
    }

    @Test
    public void staleRoutedFixesCannotCompleteFinalArrival() {
        NavigationSessionRouteState state = routed(routeWithoutHints(), Collections.emptyList(), TARGET);
        assertTrue(evaluate(state, TARGET, 2_000, 16_000).turnEvents.isEmpty());
        assertTrue(evaluate(state, TARGET, 5_000, 19_000).turnEvents.isEmpty());
        assertFalse(state.isDestinationReached());
        assertTrue(evaluate(state, TARGET, 20_000, 20_000).turnEvents.isEmpty());
        assertEquals(100, evaluate(state, TARGET, 23_000, 23_000).turnEvents.get(0).hint.command);
    }

    @Test
    public void staleIntermediateEvidencePreservesTheStopAndColocatedFinalOrder() {
        List<LatLon> stops = Collections.singletonList(TARGET);
        NavigationSessionRouteState state = routed(routeWithoutHints(), stops, TARGET);
        assertTrue(evaluate(state, TARGET, 2_000, 16_000).turnEvents.isEmpty());
        assertTrue(evaluate(state, TARGET, 5_000, 19_000).turnEvents.isEmpty());
        assertEquals(1, state.remainingIntermediateStops(stops).size());
        assertTrue(evaluate(state, TARGET, 20_000, 20_000).turnEvents.isEmpty());
        assertEquals(101, evaluate(state, TARGET, 23_000, 23_000).turnEvents.get(0).hint.command);
        assertFalse(state.isDestinationReached());
        assertTrue(evaluate(state, TARGET, 24_000, 24_000).turnEvents.isEmpty());
        assertEquals(100, evaluate(state, TARGET, 27_000, 27_000).turnEvents.get(0).hint.command);
    }

    @Test
    public void nativeFinalBeelineCannotAdvanceOnDelayedArrivalEvidence() {
        LatLon start = new LatLon(0, 0.001);
        LatLon target = new LatLon(0.001, 0.001);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), start, target),
                Collections.singletonList(new VoiceHint(1, 16, 0, 111, 0)), 120, 222);
        NavigationSessionRouteState state = routed(route, Collections.emptyList(), target);
        evaluate(state, start, 4_000, 4_000);
        assertTrue(state.isBeelineGuidanceActive());
        assertTrue(evaluate(state, target, 7_000, 21_000).turnEvents.isEmpty());
        assertTrue(evaluate(state, target, 10_000, 24_000).turnEvents.isEmpty());
        assertTrue(state.isBeelineGuidanceActive());
        assertFalse(state.isDestinationReached());
        assertTrue(evaluate(state, target, 25_000, 25_000).turnEvents.isEmpty());
        assertEquals(100, evaluate(state, target, 28_000, 28_000).turnEvents.get(0).hint.command);
    }

    private static NavigationSessionRouteState routed(GeoJsonRoute route, List<LatLon> stops, LatLon target) {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION, target, stops);
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), route,
                location(0, 0, 1_000), 0, 500);
        return state;
    }

    @Test
    public void aDelayedRoutedBeelineReminderCannotPublishLiveTime() {
        LatLon start = new LatLon(0, .001);
        LatLon target = new LatLon(.001, .001);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), start, target),
                Collections.singletonList(new VoiceHint(1, 16, 0, 111, 0)), 120, 222);
        NavigationSessionRouteState state = routed(route, Collections.emptyList(), target);
        evaluate(state, start, 4_000, 4_000);
        var reminder = evaluate(state, new LatLon(0, .0005), 7_000, 21_000);
        assertEquals(1, reminder.turnEvents.size());
        assertTrue(Double.isNaN(reminder.turnEvents.get(0).timeSeconds));
        assertFalse(state.isDestinationReached());
    }

    private static NavigationRouteEvaluation evaluate(NavigationSessionRouteState state, LatLon point, long measured, long now) {
        return state.evaluateLocation(location(point.lat, point.lon, measured), 0, false, false, 5, null, now, 0, false);
    }

    private static void assertStraightLineEvents(NavigationSession session, long measured, long now, int count) {
        NavigationLocation location = location(TARGET.lat, TARGET.lon, measured);
        location.setTime(measured, measured);
        var result = NavigationSessionResourceAdapter.onRawLocationChanged(session,
                TestNavigationTextResources.metric(), location, now, 3_000);
        assertFalse(result.isDropped());
        assertEquals(count, result.turnEvents.size());
    }
}
