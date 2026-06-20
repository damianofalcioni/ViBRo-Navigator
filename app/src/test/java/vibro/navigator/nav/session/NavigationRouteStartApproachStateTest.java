package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NavigationRouteStartApproachStateTest {
    private static final String DESTINATION = "Destination";
    private static final String TREKKING_PROFILE = "trekking";

    @Test
    public void routeStartApproachHoldsOriginalRouteUntilUserReachesRouteThreshold() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        );
        NavigationLocation requestedStart = location(0.0, 0.0, 1_000L);

        List<NavigationTurnEvent> appliedEvents = state.applyRouteResult(
                context,
                snapshot(request, new LatLon(0.0, 0.0)),
                routeStartingAwayFromRequestedStart(),
                requestedStart,
                1.4f,
                500L
        );
        NavigationRouteEvaluation approachEvaluation = state.evaluateLocation(
                requestedStart,
                1.4f,
                3f,
                90.0,
                1_000L,
                0L
        );
        NavState approachState = buildState(context, state, requestedStart, 1_000L);

        assertTrue(appliedEvents.isEmpty());
        assertFalse(approachEvaluation.shouldRecalculateRoute());
        assertFalse(approachEvaluation.isStableOnRouteSample());
        assertEquals(3_000L, approachEvaluation.getSuggestedUpdateIntervalMs());
        assertTrue(approachState.routeStatus.guidance.nextLine.contains(context.getString(R.string.direction_beeline)));
        assertNotNull(approachState.routeStatus.compassState);
        assertNotNull(approachState.routeStatus.compassState.routeStartApproachProjection);

        NavigationLocation routeStart = location(0.0, 0.001, 2_000L);
        NavigationRouteEvaluation reachedEvaluation = state.evaluateLocation(
                routeStart,
                1.4f,
                3f,
                90.0,
                2_000L,
                0L
        );
        NavState reachedState = buildState(context, state, routeStart, 2_000L);

        assertFalse(reachedEvaluation.shouldRecalculateRoute());
        assertTrue(reachedEvaluation.isStableOnRouteSample());
        assertEquals(1, reachedEvaluation.turnEvents.size());
        assertFalse(reachedState.routeStatus.guidance.nextLine.contains(context.getString(R.string.direction_beeline)));
        assertNotNull(reachedState.routeStatus.compassState);
        assertNull(reachedState.routeStatus.compassState.routeStartApproachProjection);
    }

    @Test
    public void roundTripRouteStartApproachIgnoresClosedLoopFinalSegment() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                NavigationRoutingMode.ROUND_TRIP,
                TREKKING_PROFILE,
                null,
                null,
                null,
                Collections.emptyList(),
                15_000
        );
        NavigationLocation requestedStart = location(0.0, 0.0, 1_000L);

        state.applyRouteResult(
                context,
                snapshot(request, new LatLon(0.0, 0.0)),
                roundTripRouteStartingAwayAndEndingAtRequestedStart(),
                requestedStart,
                1.4f,
                500L
        );
        NavigationRouteEvaluation approachEvaluation = state.evaluateLocation(
                requestedStart,
                1.4f,
                3f,
                90.0,
                1_000L,
                0L
        );
        NavState approachState = buildState(context, state, requestedStart, 1_000L);

        assertFalse(approachEvaluation.shouldRecalculateRoute());
        assertFalse(approachEvaluation.isStableOnRouteSample());
        assertTrue(approachEvaluation.turnEvents.isEmpty());
        assertTrue(approachState.routeStatus.guidance.nextLine.contains(context.getString(R.string.direction_beeline)));
        assertNotNull(approachState.routeStatus.compassState);
        assertNotNull(approachState.routeStatus.compassState.routeStartApproachProjection);
    }

    @NonNull
    private static NavState buildState(
            @NonNull NavigationTextResources context,
            @NonNull NavigationSessionRouteState state,
            @NonNull NavigationLocation location,
            long nowMs
    ) {
        return state.buildState(
                context,
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

    @NonNull
    private static GeoJsonRoute routeStartingAwayFromRequestedStart() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003)
                ),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                120.0,
                222.0
        );
    }

    @NonNull
    private static GeoJsonRoute roundTripRouteStartingAwayAndEndingAtRequestedStart() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.001),
                        new LatLon(0.001, 0.001),
                        new LatLon(0.0, 0.0)
                ),
                Collections.emptyList(),
                180.0,
                268.0
        );
    }

    @NonNull
    private static NavigationRouteRequestSnapshot snapshot(
            @NonNull NavigationRequest request,
            @NonNull LatLon start
    ) {
        return new NavigationRouteRequestSnapshot(
                1,
                1,
                request.routingMode,
                start,
                request.stops,
                request.destination,
                request.profile,
                request.profileParameters,
                Collections.emptyList(),
                request.roundTripDistanceMeters
        );
    }

    @NonNull
    private static NavigationLocation location(double lat, double lon, long timeMs) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(3f);
        return location;
    }
}
