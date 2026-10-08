package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;

public class NavigationRoadOrientationCueTest extends NavigationSessionRouteStateTestSupport {
    @Test
    public void tentativeDeviationHidesAnnouncedRoadCueWhileKeepingTheActiveRoute() {
        NavigationSessionRouteState state = roadState();
        NavigationLocation approaching = location(0, 0.00082, 2_000L);
        assertFalse(evaluate(state, approaching).turnEvents.isEmpty());
        assertNotNull(compass(state, approaching, false, false, 120).orientationCue);
        GeoJsonRoute originalRoute = state.currentRoute();

        NavigationLocation offRoute = location(0.0003, 0.00082, 3_000L);
        NavigationRouteEvaluation tentative = evaluate(state, offRoute);
        assertTrue(tentative.isRouteDeviationConfirmationPending());
        assertFalse(tentative.shouldRecalculateRoute());
        assertSame(originalRoute, state.currentRoute());
        assertNull(compass(state, offRoute, false, false, 120).orientationCue);
        // Heading-only refreshes must not restore the old maneuver while awaiting a reroute.
        assertNull(compass(state, offRoute, false, false, 200).orientationCue);

        NavigationLocation returned = location(0, 0.00082, 4_000L);
        evaluate(state, returned);
        assertNotNull(compass(state, returned, false, false, 200).orientationCue);
        NavigationLocation passed = location(0, 0.0011, 5_000L);
        evaluate(state, passed);
        assertNull(compass(state, passed, false, false, 200).orientationCue);
    }

    @Test
    public void offRouteDisplaySuppressesStationaryAndRoundTripRoadCues() {
        NavigationSessionRouteState state = roadState();
        NavigationLocation onRoute = location(0, 0.0005, 2_000L);
        evaluate(state, onRoute);
        assertEquals(270f, compass(state, onRoute, true, false, 120)
                .orientationCue.targetHeadingDegrees, 0.01f);
        assertEquals(15f, compass(state, onRoute, true, true, 120)
                .orientationCue.targetHeadingDegrees, 0.01f);

        NavigationLocation offRoute = location(0.0003, 0.0005, 3_000L);
        evaluate(state, offRoute);
        assertNull(compass(state, offRoute, true, false, 120).orientationCue);
        assertNull(compass(state, offRoute, true, true, 200).orientationCue);
    }

    @Test
    public void roadCueUsesTrustedSmoothedCorridorRatherThanOneCoarseDisplayFix() {
        NavigationSessionRouteState state = roadState();
        NavigationLocation approaching = location(0, 0.00082, 2_000L);
        evaluate(state, approaching);
        assertNotNull(compass(state, approaching, false, false, 120).orientationCue);

        NavigationLocation withinCorridor = location(0.0001, 0.00082, 3_000L);
        assertNotNull(compass(state, withinCorridor, false, false, 120).orientationCue);
        NavigationLocation outsideCorridor = location(0.00013, 0.00082, 3_000L, 50f);
        assertNull(compass(state, outsideCorridor, false, false, 120).orientationCue);
    }

    private static NavigationSessionRouteState roadState() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0),
                new LatLon(0, 0.001), new LatLon(0, 0.002)),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0, -75)), 30, 222);
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION,
                new LatLon(0, 0.002), Collections.emptyList());
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), route,
                location(0, 0, 1_000L), 5f, 500L);
        return state;
    }

    private static NavigationRouteEvaluation evaluate(NavigationSessionRouteState state,
            NavigationLocation location) {
        return state.evaluateLocation(location, 5f, false, 5f, null, location.getTime(), 0L);
    }

    private static NavCompassState compass(NavigationSessionRouteState state, NavigationLocation location,
            boolean stationary, boolean showNextManeuverCue, double heading) {
        NavState display = state.advanceDisplayState(
                NavigationDisplaySnapshot.builder(TestNavigationTextResources.metric())
                        .location(location, 5f, stationary, location.getAccuracy())
                        .heading(heading, 5f)
                        .orientationCue(stationary ? new CompassOrientationCue(270f) : null)
                        .timing(NavState.NO_DEADLINE, location.getTime())
                        .build(), showNextManeuverCue);
        assertNotNull(display.routeStatus.compassState);
        return display.routeStatus.compassState;
    }
}
