package vibro.navigator.nav.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import static org.junit.Assert.*;

public class NavigationRouteAppliedGuidanceFreshnessTest extends NavigationSessionRouteStateTestSupport {
    private static final long MEASURED_MS = 20_000;
    private static final LatLon START = new LatLon(0, 0);
    private static final LatLon TURN = new LatLon(0, .001);
    private static final LatLon END = new LatLon(.001, .001);
    private final NavigationSessionRouteState state = new NavigationSessionRouteState();

    @Test
    public void expiredOrFutureFixCannotAnnounceAnInitialTurnInEitherRoutedMode() {
        for (boolean roundTrip : new boolean[] {false, true}) {
            assertTrue(apply(roundTrip, MEASURED_MS + 10_001, START).isEmpty());
            assertTrue(state.hasActiveRoute());
            assertTrue(apply(roundTrip, MEASURED_MS - 1, START).isEmpty());
        }
    }

    @Test
    public void freshFixIncludingTheExpiryBoundaryRetainsInitialGuidance() {
        for (boolean roundTrip : new boolean[] {false, true}) {
            assertEquals(1, apply(roundTrip, MEASURED_MS, START).size());
            assertEquals(1, apply(roundTrip, MEASURED_MS + 10_000, START).size());
        }
    }

    @Test
    public void stalePositionCannotSkipTheFirstManeuverAndFreshProgressCanStillAlert() {
        assertTrue(apply(false, MEASURED_MS + 10_001, END).isEmpty());
        NavigationLocation fresh = location(0, .0009, 34_000);
        fresh.setTime(34_000, 34_000);
        NavigationRouteEvaluation evaluation = state.evaluateLocation(fresh, 2, 5, 90.0, 34_000, 0);
        assertEquals(1, evaluation.turnEvents.size());
        assertEquals(2, evaluation.turnEvents.get(0).hint.command);
    }

    @Test
    public void delayedRouteStartApproachFixCannotAnnounceAnInitialTurn() {
        LatLon destination = new LatLon(.001, .002);
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION,
                destination, Collections.emptyList());
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(TURN, new LatLon(0, .002), destination),
                Collections.singletonList(new VoiceHint(1, 2, 0, 111, -90)), 120, 222);
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), route,
                location(START.lat, START.lon, MEASURED_MS), 2, false, MEASURED_MS, MEASURED_MS);
        NavigationLocation delayed = location(TURN.lat, TURN.lon, 23_000);
        delayed.setTime(23_000, 23_000);
        NavigationRouteEvaluation held = state.evaluateLocation(delayed, 2, 5, 90.0, 34_001, 0);
        assertTrue(held.turnEvents.isEmpty());
        assertFalse(held.isStableOnRouteSample());
    }

    @Test
    public void delayedOnRouteProgressCannotConsumeAnApproachingTurnAlert() {
        apply(false, MEASURED_MS, START);
        NavigationLocation delayed = location(0, .0009, 23_000);
        delayed.setTime(23_000, 23_000);
        NavigationRouteEvaluation held = state.evaluateLocation(delayed, 2, 5, 90.0, 34_001, 0);
        assertTrue(held.turnEvents.isEmpty());
        assertFalse(held.isStableOnRouteSample());
        NavigationLocation fresh = location(0, .0009, 37_000);
        fresh.setTime(37_000, 37_000);
        NavigationRouteEvaluation resumed = state.evaluateLocation(fresh, 2, 5, 90.0, 37_000, 0);
        assertEquals(1, resumed.turnEvents.size());
        assertEquals(2, resumed.turnEvents.get(0).hint.command);
    }

    @Test
    public void delayedReturnToRoadCannotConsumeTheFollowingTurnAlert() {
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION, END,
                Collections.emptyList());
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(START, TURN, new LatLon(.0001, .001), END),
                Arrays.asList(new VoiceHint(0, 16, 0, 111, 0), new VoiceHint(2, 2, 0, 100, -90)), 120, 222);
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), route,
                location(START.lat, START.lon, MEASURED_MS), 2, false, MEASURED_MS, MEASURED_MS);
        assertTrue(state.isBeelineGuidanceActive());
        NavigationLocation delayed = location(TURN.lat, TURN.lon, 23_000);
        delayed.setTime(23_000, 23_000);
        NavigationRouteEvaluation held = state.evaluateLocation(delayed, 2, 5, 0.0, 34_001, 0);
        assertTrue(held.turnEvents.isEmpty());
        assertFalse(held.isStableOnRouteSample());
        NavigationLocation fresh = location(TURN.lat, TURN.lon, 37_000);
        fresh.setTime(37_000, 37_000);
        NavigationRouteEvaluation resumed = state.evaluateLocation(fresh, 2, 5, 0.0, 37_000, 0);
        assertTrue(resumed.turnEvents.stream().anyMatch(event -> event.hint.command == 2));
    }

    private List<NavigationTurnEvent> apply(boolean roundTrip, long appliedMs, LatLon point) {
        state.reset();
        NavigationRequest request = new NavigationRequest(roundTrip ? NavigationRoutingMode.ROUND_TRIP
                : NavigationRoutingMode.BROUTER, TREKKING_PROFILE, DESTINATION,
                roundTrip ? START : END, Collections.emptyList());
        GeoJsonRoute route = new GeoJsonRoute(roundTrip ? Arrays.asList(START, TURN, END, START)
                : Arrays.asList(START, TURN, END), Collections.singletonList(new VoiceHint(1, 2, 0, 111, -90)),
                roundTrip ? 180 : 120, roundTrip ? 380 : 222);
        NavigationLocation fix = location(point.lat, point.lon, MEASURED_MS);
        fix.setTime(MEASURED_MS, MEASURED_MS);
        return state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), route,
                fix, 2, false, MEASURED_MS, appliedMs);
    }
}
