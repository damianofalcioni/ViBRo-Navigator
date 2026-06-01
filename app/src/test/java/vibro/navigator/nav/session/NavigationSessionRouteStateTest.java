package vibro.navigator.nav.session;


import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterRouteException;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class NavigationSessionRouteStateTest extends NavigationSessionRouteStateTestSupport {
    @Test
    public void evaluateLocation_waitsForAccurateStartupFixBeforeFirstRoute() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();

        NavigationRouteEvaluation evaluation = state.evaluateLocation(
                location(0.0, 0.0, 1_000L, 30f),
                0f,
                30f,
                null,
                1_000L,
                0L
        );

        assertFalse(evaluation.shouldRecalculateRoute());
        assertFalse(evaluation.isStableOnRouteSample());
        assertEquals(1_000L, evaluation.getSuggestedUpdateIntervalMs());
        assertTrue(evaluation.turnEvents.isEmpty());
    }

    @Test
    public void evaluateLocation_requestsFirstRouteWithAccurateStartupFix() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();

        NavigationRouteEvaluation evaluation = state.evaluateLocation(
                location(0.0, 0.0, 1_000L, 25f),
                0f,
                25f,
                null,
                1_000L,
                0L
        );

        assertTrue(evaluation.shouldRecalculateRoute());
    }

    @Test
    public void applyRouteResult_buildsInitialTurnEventAndRenderableState() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );

        List<NavigationTurnEvent> turnEvents = state.applyRouteResult(
                context,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );
        NavState navState = state.buildState(
                context,
                location(0.0, 0.0, 1_000L),
                5f,
                false,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                1_000L,
                false,
                null,
                null
        );

        assertEquals(1, turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.INITIAL, turnEvents.get(0).type);
        assertFalse(navState.routeStatus.guidance.nextLine.isEmpty());
        assertTrue(navState.routeStatus.progress.destinationLine.contains(context.getString(R.string.nav_destination_label)));
        assertTrue(navState.routeStatus.progress.stopProgressBlock.isEmpty());
    }

    @Test
    public void buildState_showsTurnManeuverCueFromFiveSecondNotificationWithCoarseAccuracyUntilPassed() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.002),
                Collections.emptyList()
        );
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, -75)),
                30.0,
                222.0
        );
        state.applyRouteResult(
                context,
                snapshot(request),
                route,
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavigationLocation approachingTurn = location(0.0, 0.00082, 2_000L);
        NavigationRouteEvaluation approachingEvaluation = state.evaluateLocation(
                approachingTurn,
                5f,
                25f,
                90.0,
                2_000L,
                0L
        );
        NavState approachingState = state.buildState(
                context,
                approachingTurn,
                5f,
                false,
                25f,
                null,
                120.0,
                null,
                NavState.NO_DEADLINE,
                2_000L,
                false,
                null,
                null
        );

        assertEquals(1, approachingEvaluation.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, approachingEvaluation.turnEvents.get(0).type);
        assertNotNull(approachingState.routeStatus.compassState);
        assertNotNull(approachingState.routeStatus.compassState.orientationCue);
        assertEquals(
                15.0f,
                approachingState.routeStatus.compassState.orientationCue.targetHeadingDegrees,
                0.01f
        );
        NavState rotatedHeadingState = state.buildState(
                context,
                approachingTurn,
                5f,
                false,
                25f,
                null,
                45.0,
                null,
                NavState.NO_DEADLINE,
                2_100L,
                false,
                null,
                null
        );

        assertNotNull(rotatedHeadingState.routeStatus.compassState);
        assertNotNull(rotatedHeadingState.routeStatus.compassState.orientationCue);
        assertEquals(
                15.0f,
                rotatedHeadingState.routeStatus.compassState.orientationCue.targetHeadingDegrees,
                0.01f
        );
        NavigationLocation justAfterTurn = location(0.0, 0.00101, 2_500L);
        state.evaluateLocation(
                justAfterTurn,
                5f,
                5f,
                90.0,
                2_500L,
                0L
        );
        NavState justAfterTurnState = state.buildState(
                context,
                justAfterTurn,
                5f,
                false,
                5f,
                null,
                90.0,
                null,
                NavState.NO_DEADLINE,
                2_500L,
                false,
                null,
                null
        );

        assertNotNull(justAfterTurnState.routeStatus.compassState);
        assertNull(justAfterTurnState.routeStatus.compassState.orientationCue);

        NavigationLocation passedTurn = location(0.0, 0.0011, 3_000L);
        state.evaluateLocation(
                passedTurn,
                5f,
                5f,
                90.0,
                3_000L,
                0L
        );
        NavState passedState = state.buildState(
                context,
                passedTurn,
                5f,
                false,
                5f,
                null,
                90.0,
                null,
                NavState.NO_DEADLINE,
                3_000L,
                false,
                null,
                null
        );

        assertNotNull(passedState.routeStatus.compassState);
        assertNull(passedState.routeStatus.compassState.orientationCue);
    }

    @Test
    public void evaluateLocation_emitsArrivalEventAndReachedStateInsideDestinationRadius() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavigationRouteEvaluation arrivalEvaluation = state.evaluateLocation(
                location(0.0, 0.001, 2_000L),
                0f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationRouteEvaluation repeatedEvaluation = state.evaluateLocation(
                location(0.0, 0.001, 3_000L),
                0f,
                5f,
                90.0,
                3_000L,
                0L
        );
        NavState navState = state.buildState(
                context,
                location(0.0, 0.001, 3_000L),
                0f,
                true,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                3_000L,
                false,
                null,
                null
        );

        assertFalse(arrivalEvaluation.shouldRecalculateRoute());
        assertEquals(1, arrivalEvaluation.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, arrivalEvaluation.turnEvents.get(0).type);
        assertEquals(100, arrivalEvaluation.turnEvents.get(0).hint.command);
        assertTrue(repeatedEvaluation.turnEvents.isEmpty());
        assertEquals("■ Destination reached", navState.routeStatus.guidance.nextLine);
        assertEquals(context.getString(R.string.nav_destination_reached), navState.routeStatus.progress.destinationLine);
        assertTrue(navState.routeStatus.progress.stopProgressBlock.isEmpty());
    }

    @Test
    public void evaluateLocation_usesRouteThresholdForDestinationArrivalRadius() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavigationRouteEvaluation arrivalEvaluation = state.evaluateLocation(
                location(0.0, 0.0009, 2_000L),
                0f,
                5f,
                90.0,
                2_000L,
                0L
        );

        assertFalse(arrivalEvaluation.shouldRecalculateRoute());
        assertEquals(1, arrivalEvaluation.turnEvents.size());
        assertEquals(100, arrivalEvaluation.turnEvents.get(0).hint.command);
    }

    @Test
    public void evaluateLocation_emitsIntermediateArrivalEventOnceAndKeepsDestinationActive() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.singletonList(new LatLon(0.0, 0.001))
        );
        state.applyRouteResult(
                context,
                snapshot(request),
                routeWithoutHints(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavigationRouteEvaluation stopEvaluation = state.evaluateLocation(
                location(0.0, 0.001, 2_000L),
                0f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationRouteEvaluation repeatedEvaluation = state.evaluateLocation(
                location(0.0, 0.001, 3_000L),
                0f,
                5f,
                90.0,
                3_000L,
                0L
        );
        NavState navState = state.buildState(
                context,
                location(0.0, 0.001, 3_000L),
                0f,
                true,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                3_000L,
                false,
                null,
                null
        );

        assertFalse(stopEvaluation.shouldRecalculateRoute());
        assertEquals(1, stopEvaluation.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, stopEvaluation.turnEvents.get(0).type);
        assertEquals(101, stopEvaluation.turnEvents.get(0).hint.command);
        assertTrue(repeatedEvaluation.turnEvents.isEmpty());
        assertTrue(navState.routeStatus.guidance.nextLine.contains(context.getString(R.string.direction_arrive)));
        assertEquals("", navState.routeStatus.guidance.afterNextLine);
        assertTrue(navState.routeStatus.progress.destinationLine.contains(context.getString(R.string.nav_destination_label)));
        assertFalse(navState.routeStatus.progress.destinationLine.equals(context.getString(R.string.nav_destination_reached)));
        assertTrue(navState.routeStatus.progress.stopProgressBlock.isEmpty());
    }

    @Test
    public void evaluateLocation_usesRouteThresholdForIntermediateArrivalRadius() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.singletonList(new LatLon(0.0, 0.001))
        );
        state.applyRouteResult(
                context,
                snapshot(request),
                routeWithoutHints(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavigationRouteEvaluation stopEvaluation = state.evaluateLocation(
                location(0.0, 0.0009, 2_000L),
                0f,
                5f,
                90.0,
                2_000L,
                0L
        );

        assertFalse(stopEvaluation.shouldRecalculateRoute());
        assertEquals(1, stopEvaluation.turnEvents.size());
        assertEquals(101, stopEvaluation.turnEvents.get(0).hint.command);
    }

    @Test
    public void applyRouteResult_emitsIntermediateArrivalEventWhenAlreadyInsideStopRadius() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.singletonList(new LatLon(0.0, 0.001))
        );

        List<NavigationTurnEvent> turnEvents = state.applyRouteResult(
                context,
                snapshot(request),
                routeWithoutHints(),
                location(0.0, 0.001, 1_000L),
                5f,
                500L
        );

        assertEquals(1, turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, turnEvents.get(0).type);
        assertEquals(101, turnEvents.get(0).hint.command);
    }

    @Test
    public void remainingIntermediateStops_excludesReachedStopsAfterArrival() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        List<LatLon> stops = Arrays.asList(new LatLon(0.0, 0.001), new LatLon(0.0, 0.002));
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                stops
        );
        state.applyRouteResult(
                context,
                snapshot(request),
                routeWithoutHints(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        state.evaluateLocation(
                location(0.0, 0.001, 2_000L),
                0f,
                5f,
                90.0,
                2_000L,
                0L
        );

        List<LatLon> remainingStops = state.remainingIntermediateStops(stops);

        assertEquals(1, remainingStops.size());
        assertEquals(0.002, remainingStops.get(0).lon, 0.0);
    }

    @Test
    public void evaluateLocation_prefersArrivalOverOffTrackRerouteWhenInsideDestinationRadius() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavigationRouteEvaluation evaluation = state.evaluateLocation(
                location(0.00018, 0.001, 2_000L, 30f),
                1f,
                30f,
                90.0,
                2_000L,
                0L
        );

        assertFalse(evaluation.shouldRecalculateRoute());
        assertEquals(1, evaluation.turnEvents.size());
        assertEquals(100, evaluation.turnEvents.get(0).hint.command);
    }

    @Test
    public void buildState_usesSmoothedSlowProgressForEtaOnCurrentSegment() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        NavigationLocation startLocation = locationWithSpeed(0.0, 0.0, 1_000L, 0.4f);
        state.applyRouteResult(
                context,
                snapshot(request),
                new GeoJsonRoute(
                        Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001)),
                        Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                        10.0,
                        111.0
                ),
                startLocation,
                0.4f,
                false,
                500L
        );

        state.evaluateLocation(
                locationWithSpeed(0.0, 0.000003, 1_500L, 0.4f),
                0.4f,
                false,
                5f,
                90.0,
                1_500L,
                0L
        );
        NavigationLocation progressedLocation = locationWithSpeed(0.0, 0.000015, 4_000L, 0.4f);
        state.evaluateLocation(
                progressedLocation,
                0.4f,
                false,
                5f,
                90.0,
                4_000L,
                0L
        );
        NavState navState = state.buildState(
                context,
                progressedLocation,
                0.4f,
                false,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                4_000L,
                false,
                null,
                null
        );

        assertTrue(navState.routeStatus.guidance.nextLine.contains("min"));
        assertFalse(navState.routeStatus.guidance.nextLine.contains("10 s"));
    }

    @Test
    public void applyRouteResult_initialTurnEventUsesRouteTimingWhenLiveEtaSpeedIsUnavailable() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );

        List<NavigationTurnEvent> turnEvents = state.applyRouteResult(
                context,
                snapshot(request),
                new GeoJsonRoute(
                        Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001)),
                        Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                        Arrays.asList(0.0, 42.0),
                        42.0,
                        111.0
                ),
                location(0.0, 0.0, 1_000L),
                0f,
                false,
                500L
        );

        assertEquals(1, turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.INITIAL, turnEvents.get(0).type);
        assertEquals(42.0, turnEvents.get(0).timeSeconds, 0.0);
    }
}
