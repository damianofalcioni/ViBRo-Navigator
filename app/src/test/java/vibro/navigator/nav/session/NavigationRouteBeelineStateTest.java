package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.VoiceHint;

public class NavigationRouteBeelineStateTest extends NavigationSessionRouteStateTestSupport {
    private static final LatLon STOP = new LatLon(0.001, 0.001);
    private static final LatLon DESTINATION_POINT = new LatLon(0.0, 0.003);

    @Test
    public void intermediateBeelineAllowsAnyPathAndTargetsReturnLegAfterArrival() {
        NavigationTextResources textResources = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = requestWithStop();
        state.applyRouteResult(
                textResources,
                snapshot(request),
                routeWithIntermediateBeeline(),
                location(0.0, 0.0, 1_000L, 3f),
                1.4f,
                500L
        );

        NavigationRouteEvaluation started = state.evaluateLocation(
                location(0.0, 0.001, 2_000L, 3f),
                1.4f,
                3f,
                0.0,
                2_000L,
                0L
        );
        NavigationLocation offDirectPath = location(0.0005, 0.0015, 3_000L, 3f);
        NavigationRouteEvaluation offPath = state.evaluateLocation(
                offDirectPath,
                1.4f,
                3f,
                315.0,
                3_000L,
                0L
        );
        NavState offPathState = buildState(textResources, state, offDirectPath, 3_000L);

        assertFalse(started.shouldRecalculateRoute());
        assertFalse(offPath.shouldRecalculateRoute());
        assertFalse(offPath.isStableOnRouteSample());
        assertTrue(offPathState.routeStatus.guidance.nextLine.contains(
                textResources.getString(R.string.direction_beeline)
        ));
        assertNotNull(offPathState.routeStatus.compassState);
        assertNotNull(offPathState.routeStatus.compassState.routeStartApproachProjection);

        NavigationRouteEvaluation reachedStop = state.evaluateLocation(
                location(STOP.lat, STOP.lon, 4_000L, 3f),
                1.4f,
                3f,
                180.0,
                4_000L,
                0L
        );
        NavState returnState = buildState(
                textResources,
                state,
                location(STOP.lat, STOP.lon, 4_000L, 3f),
                4_000L
        );

        assertEquals(1, reachedStop.turnEvents.size());
        assertEquals(101, reachedStop.turnEvents.get(0).hint.command);
        assertTrue(returnState.routeStatus.guidance.nextLine.contains(
                textResources.getString(R.string.direction_beeline)
        ));
        assertTrue(returnState.routeStatus.guidance.afterNextLine.contains(
                textResources.getString(R.string.direction_turn_left)
        ));
        assertNotNull(returnState.routeStatus.compassState.routeStartApproachProjection);

        NavigationRouteEvaluation returnedToRoute = state.evaluateLocation(
                location(0.0, 0.001, 5_000L, 3f),
                1.4f,
                3f,
                180.0,
                5_000L,
                0L
        );
        NavState routedState = buildState(
                textResources,
                state,
                location(0.0, 0.001, 5_000L, 3f),
                5_000L
        );

        assertFalse(returnedToRoute.shouldRecalculateRoute());
        assertNotNull(routedState.routeStatus.compassState);
        assertNull(routedState.routeStatus.compassState.routeStartApproachProjection);
        assertTrue(routedState.routeStatus.guidance.nextLine.contains(
                textResources.getString(R.string.direction_turn_left)
        ));
    }

    @Test
    public void finalBeelineSuppressesDeviationUntilRequestedDestinationIsReached() {
        NavigationTextResources textResources = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                DESTINATION_POINT,
                Collections.emptyList()
        );
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        DESTINATION_POINT
                ),
                Collections.singletonList(new VoiceHint(
                        1,
                        RouteStartApproach.BEELINE_COMMAND,
                        0,
                        222.0,
                        0
                )),
                180.0,
                333.0
        );
        state.applyRouteResult(
                textResources,
                snapshot(request),
                route,
                location(0.0, 0.0, 1_000L, 3f),
                1.4f,
                500L
        );
        state.evaluateLocation(
                location(0.0, 0.001, 2_000L, 3f),
                1.4f,
                3f,
                90.0,
                2_000L,
                0L
        );

        NavigationRouteEvaluation offPath = state.evaluateLocation(
                location(0.001, 0.002, 3_000L, 3f),
                1.4f,
                3f,
                270.0,
                3_000L,
                0L
        );
        NavigationRouteEvaluation reached = state.evaluateLocation(
                location(DESTINATION_POINT.lat, DESTINATION_POINT.lon, 4_000L, 3f),
                1.4f,
                3f,
                270.0,
                4_000L,
                0L
        );

        assertFalse(offPath.shouldRecalculateRoute());
        assertEquals(1, reached.turnEvents.size());
        assertEquals(100, reached.turnEvents.get(0).hint.command);
    }

    @NonNull
    private static NavigationRequest requestWithStop() {
        return new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                DESTINATION_POINT,
                Collections.singletonList(STOP)
        );
    }

    @NonNull
    private static GeoJsonRoute routeWithIntermediateBeeline() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        STOP,
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        DESTINATION_POINT
                ),
                Arrays.asList(
                        new VoiceHint(1, RouteStartApproach.BEELINE_COMMAND, 0, 111.0, 0),
                        new VoiceHint(2, RouteStartApproach.BEELINE_COMMAND, 0, 111.0, 0),
                        new VoiceHint(4, 2, 0, 111.0, -90)
                ),
                300.0,
                555.0
        );
    }

    @NonNull
    private static NavState buildState(
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationSessionRouteState state,
            @NonNull vibro.navigator.nav.location.NavigationLocation location,
            long nowMs
    ) {
        return state.buildState(
                textResources,
                location,
                1.4f,
                false,
                3f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                nowMs,
                false,
                null,
                null
        );
    }
}
