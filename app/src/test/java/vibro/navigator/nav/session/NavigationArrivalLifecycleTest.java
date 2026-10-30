package vibro.navigator.nav.session;

import java.util.Collections;
import java.util.Arrays;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import static org.junit.Assert.*;

public class NavigationArrivalLifecycleTest extends NavigationSessionRouteStateTestSupport {
    private static final LatLon TARGET = new LatLon(0, .003);

    @Test
    public void routedArrivalMustRestartConfirmationAfterPause() {
        checkRoutedPause(false);
    }

    @Test
    public void intermediateArrivalMustRestartConfirmationAfterPause() {
        checkRoutedPause(true);
    }

    @Test
    public void straightLineArrivalMustRestartConfirmationAfterPause() {
        checkStraightLinePause(false);
    }

    @Test
    public void straightLineIntermediateArrivalMustRestartConfirmationAfterPause() {
        checkStraightLinePause(true);
    }

    @Test
    public void routeAppliedWhilePausedCannotSeedTheResumedArrivalWindow() {
        NavigationSession session = new NavigationSession();
        LatLon nearbyTarget = new LatLon(0, 8.0 / 111_320);
        session.loadRequest(new NavigationRequest(TREKKING_PROFILE, DESTINATION, nearbyTarget, Collections.emptyList()));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        session.setActiveLocationProviders("gps");
        acceptNearTarget(session, 1_000);
        var pending = session.prepareRouteRequest(true, 1_000);
        assertNotNull(pending);
        assertTrue(session.pause());
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), nearbyTarget),
                Collections.emptyList(), 8, 8);
        assertTrue(NavigationSessionResourceAdapter.applyRouteResult(session, TestNavigationTextResources.metric(),
                pending, route, 1_000, 3_000).isEmpty());
        assertTrue(session.resume());
        assertTrue("Route application during pause cannot restore pre-pause arrival evidence",
                acceptNearTarget(session, 5_000).turnEvents.isEmpty());
        assertEquals(100, acceptNearTarget(session, 8_000).turnEvents.get(0).hint.command);
    }

    @Test
    public void nativeFinalBeelineMustRestartConfirmationAfterPause() {
        NavigationSession session = new NavigationSession();
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION, TARGET, Collections.emptyList());
        session.loadRequest(request);
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        NavigationSessionRouteState state = session.components.routeState;
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, .002), TARGET),
                Collections.singletonList(new VoiceHint(1, 16, 0, 111, 0)), 180, 333);
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), route,
                location(0, 0, 1_000), 0, 500);
        state.evaluateLocation(location(0, .002, 2_000), 0, 5, null, 2_000, 0);
        assertTrue(state.isBeelineGuidanceActive());
        assertTrue(evaluate(state, 5_000).turnEvents.isEmpty());
        assertTrue(session.pause());
        assertTrue(session.resume());
        assertTrue(evaluate(state, 8_000).turnEvents.isEmpty());
        assertTrue(state.isBeelineGuidanceActive());
        assertEquals(100, evaluate(state, 11_000).turnEvents.get(0).hint.command);
    }

    private static void checkStraightLinePause(boolean intermediate) {
        NavigationSession session = new NavigationSession();
        NavigationRequest request = new NavigationRequest(NavigationRoutingMode.STRAIGHT_LINE, null,
                DESTINATION, TARGET, intermediate ? Collections.singletonList(TARGET) : Collections.emptyList());
        session.loadRequest(request);
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        StraightLineNavigationState state = session.components.straightLineState;
        assertTrue(evaluate(state, request, 2_000).turnEvents.isEmpty());
        assertTrue(session.pause());
        assertTrue(session.resume());
        assertTrue("Paused time cannot finish arrival confirmation", evaluate(state, request, 5_000).turnEvents.isEmpty());
        assertEquals(intermediate ? 101 : 100, evaluate(state, request, 8_000).turnEvents.get(0).hint.command);
        assertTrue(session.pause());
        assertTrue(session.resume());
        assertTrue(evaluate(state, request, 11_000).turnEvents.isEmpty());
        if (intermediate) {
            assertEquals(1, state.nextStopIndexForDisplay());
            assertEquals(100, evaluate(state, request, 14_000).turnEvents.get(0).hint.command);
        } else {
            assertTrue(state.isDestinationReachedForDisplay());
        }
    }

    private static void checkRoutedPause(boolean intermediate) {
        NavigationSession session = new NavigationSession();
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION, TARGET,
                intermediate ? Collections.singletonList(TARGET) : Collections.emptyList());
        session.loadRequest(request);
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        NavigationSessionRouteState state = session.components.routeState;
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), routeWithoutHints(),
                location(0, 0, 1_000), 0, 500);
        assertTrue(evaluate(state, 2_000).turnEvents.isEmpty());
        assertTrue(session.pause());
        assertTrue(session.resume());
        assertTrue("Paused time cannot finish arrival confirmation", evaluate(state, 5_000).turnEvents.isEmpty());
        assertEquals(intermediate ? 101 : 100, evaluate(state, 8_000).turnEvents.get(0).hint.command);
        assertTrue(session.pause());
        assertTrue(session.resume());
        assertTrue(evaluate(state, 11_000).turnEvents.isEmpty());
        if (intermediate) {
            assertTrue(state.remainingIntermediateStops(request.stops).isEmpty());
            assertEquals(100, evaluate(state, 14_000).turnEvents.get(0).hint.command);
        } else {
            assertTrue(state.isDestinationReached());
        }
    }

    private static NavigationRouteEvaluation evaluate(NavigationSessionRouteState state, long time) {
        return state.evaluateLocation(location(TARGET.lat, TARGET.lon, time), 0, 5, null, time, 0);
    }

    private static NavigationRouteEvaluation evaluate(StraightLineNavigationState state, NavigationRequest request, long time) {
        return state.evaluateLocation(request, location(TARGET.lat, TARGET.lon, time), 0, true, 5, null, time, 0);
    }

    private static NavigationLocationUpdateResult acceptNearTarget(NavigationSession session, long time) {
        return NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(),
                location(0, 0, time), time, 3_000);
    }
}
