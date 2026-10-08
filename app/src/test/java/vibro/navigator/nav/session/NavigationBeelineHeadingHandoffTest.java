package vibro.navigator.nav.session;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationBeelineHeadingHandoffTest {
    @Test
    public void command16BeelinesWaitForForwardRoadProgressWithoutDisplayUpdatesInBothRoutedModes() {
        for (NavigationRoutingMode mode : new NavigationRoutingMode[]{
                NavigationRoutingMode.BROUTER, NavigationRoutingMode.ROUND_TRIP}) {
            NavigationSessionRouteState state = new NavigationSessionRouteState();
            apply(state, beelineRoute(), mode);
            evaluate(state, fix(0, 0, 1_000), false);
            evaluate(state, fix(0, 0.001, 4_000), false);
            assertFalse(state.isBeelineGuidanceActive());
            assertTrue(state.usesLocationHeading());
            evaluate(state, fix(0.0002, 0.001, 7_000), false);
            assertFalse(state.usesLocationHeading());
        }
    }

    @Test
    public void incomingBeelineSamplesCannotConfirmAnEarlyRoadHandoff() {
        NavigationSessionRouteState state = routedBeeline();
        evaluate(state, fix(0, 0, 1_000), false);
        evaluate(state, fix(0, 0.001, 4_000), false);
        evaluate(state, fix(0.0002, 0.001, 5_000), false);
        assertTrue(state.usesLocationHeading());
        evaluate(state, fix(0.0003, 0.001, 7_000), false);
        assertFalse(state.usesLocationHeading());
    }

    @Test
    public void roadRecoveryFromAnActiveBeelineRetainsHeadingUntilFreshForwardProgress() {
        NavigationSessionRouteState state = routedBeeline();
        GeoJsonRoute road = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0.003, 0)),
                Collections.emptyList(), 100, 333);
        apply(state, road);
        assertFalse(state.isBeelineGuidanceActive());
        assertTrue(state.usesLocationHeading());
        evaluate(state, fix(0.0002, 0, 4_000), false);
        assertTrue(state.usesLocationHeading());
        evaluate(state, fix(0.0004, 0, 7_000), false);
        assertFalse(state.usesLocationHeading());
    }

    @Test
    public void roadReplacementClearsPendingHandoffFromThePreviousRoute() {
        NavigationSessionRouteState state = routedBeeline();
        evaluate(state, fix(0, 0.001, 4_000), false);
        assertTrue(state.usesLocationHeading());
        apply(state, new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0.003, 0)),
                Collections.emptyList(), 100, 333));
        assertFalse(state.usesLocationHeading());
    }

    @Test
    public void backwardStalledAndUnknownProgressKeepLocationHeadingUntilForward() {
        NavigationSessionRouteState state = routedBeeline();
        evaluate(state, fix(0, 0.001, 4_000), false);
        evaluate(state, fix(0.0004, 0.001, 5_000), false);
        assertTrue(state.usesLocationHeading()); // Not enough independently timed road fixes.
        evaluate(state, fix(0.0003, 0.001, 8_000), false);
        assertTrue(state.usesLocationHeading()); // Backward.
        evaluate(state, fix(0.0003, 0.001, 11_000), false);
        assertTrue(state.usesLocationHeading()); // Stalled.
        evaluate(state, fix(0.0005, 0.001, 14_000), false);
        assertFalse(state.usesLocationHeading());
        evaluate(state, fix(0.0005, 0.001, 17_000), true);
        assertFalse(state.usesLocationHeading()); // Ordinary stops do not re-arm the handoff.
    }

    @Test
    public void stationaryAndTentativeOffRouteFixesCannotFinishHandoff() {
        NavigationSessionRouteState state = routedBeeline();
        evaluate(state, fix(0, 0.001, 4_000), false);
        evaluate(state, fix(0.0002, 0.001, 7_000), true);
        assertTrue(state.usesLocationHeading());
        evaluate(state, fix(0.0004, 0.0013, 10_000), false);
        assertTrue(state.usesLocationHeading());
    }

    @Test
    public void pauseMotionResetRequiresFreshForwardEvidenceAndSessionResetClearsHandoff() {
        NavigationSessionRouteState state = routedBeeline();
        evaluate(state, fix(0, 0.001, 4_000), false);
        state.clearMotionEvidence();
        evaluate(state, fix(0.0002, 0.001, 7_000), false);
        assertTrue(state.usesLocationHeading());
        evaluate(state, fix(0.0004, 0.001, 10_000), false);
        assertFalse(state.usesLocationHeading());
        state.reset();
        assertFalse(state.usesLocationHeading());
    }

    @Test
    public void routeStartApproachWaitsForRoadProgressAndRoadReplacementClearsOldPendingHandoff() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        GeoJsonRoute road = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0.001),
                new LatLon(0.003, 0.001)), Collections.emptyList(), 100, 333);
        apply(state, road);
        assertTrue(state.isBeelineGuidanceActive());
        evaluate(state, fix(0, 0.001, 4_000), false);
        assertFalse(state.isBeelineGuidanceActive());
        assertTrue(state.usesLocationHeading());
        evaluate(state, fix(0.0001, 0.001, 7_000), false);
        assertTrue(state.usesLocationHeading()); // First road progress sample after approach completion.
        evaluate(state, fix(0.0003, 0.001, 10_000), false);
        assertFalse(state.usesLocationHeading());
        apply(state, new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0.003, 0)),
                Collections.emptyList(), 100, 333));
        assertFalse(state.usesLocationHeading());
    }

    private static NavigationSessionRouteState routedBeeline() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        apply(state, beelineRoute());
        return state;
    }

    private static GeoJsonRoute beelineRoute() {
        return new GeoJsonRoute(Arrays.asList(new LatLon(0, 0),
                new LatLon(0, 0.001), new LatLon(0.003, 0.001)),
                Collections.singletonList(new VoiceHint(0, 16, 0, 111, 0)), 100, 444);
    }

    private static void apply(NavigationSessionRouteState state, GeoJsonRoute route) {
        apply(state, route, NavigationRoutingMode.BROUTER);
    }

    private static void apply(NavigationSessionRouteState state, GeoJsonRoute route, NavigationRoutingMode mode) {
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(1, 1,
                mode, new LatLon(0, 0), Collections.emptyList(),
                route.track.get(route.track.size() - 1), "trekking", null, Collections.emptyList(), 1_000);
        state.applyRouteResult(TestNavigationTextResources.metric(), request, route,
                fix(0, 0, 1_000), 3f, false, 1_000);
    }

    private static void evaluate(NavigationSessionRouteState state, NavigationLocation location, boolean stationary) {
        state.evaluateLocation(location, 3f, stationary, 5f, null,
                location.getElapsedRealtimeOrTimeMs(), 0L);
    }

    private static NavigationLocation fix(double latitude, double longitude, long timeMs) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(timeMs, timeMs);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(5f);
        location.setSpeed(3f);
        return location;
    }
}
