package vibro.navigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.location.Location;

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
public class NavigationSessionRouteStateTest {

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

        List<NavigationSession.TurnEvent> turnEvents = state.applyRouteResult(
                context,
                request,
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
        assertEquals(NavigationSession.TurnEvent.Type.INITIAL, turnEvents.get(0).type);
        assertFalse(navState.nextLine.isEmpty());
        assertTrue(navState.destinationLine.contains(context.getString(R.string.nav_destination_label)));
        assertTrue(navState.stopProgressBlock.isEmpty());
    }

    @Test
    public void buildState_usesSmoothedSlowProgressForEtaOnCurrentSegment() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        Location startLocation = locationWithSpeed(0.0, 0.0, 1_000L, 0.4f);
        state.applyRouteResult(
                context,
                request,
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
        Location progressedLocation = locationWithSpeed(0.0, 0.000015, 4_000L, 0.4f);
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

        assertTrue(navState.nextLine.contains("min"));
        assertFalse(navState.nextLine.contains("10 s"));
    }

    @Test
    public void applyRouteResult_initialTurnEventUsesRouteTimingWhenLiveEtaSpeedIsUnavailable() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );

        List<NavigationSession.TurnEvent> turnEvents = state.applyRouteResult(
                context,
                request,
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
        assertEquals(NavigationSession.TurnEvent.Type.INITIAL, turnEvents.get(0).type);
        assertEquals(42.0, turnEvents.get(0).timeSeconds, 0.0);
    }

    @Test
    public void evaluateLocation_surfacesOffTrackRerouteNotice() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavigationSessionRouteState.Evaluation evaluation = state.evaluateLocation(
                location(0.0003, 0.0, 2_000L),
                5f,
                5f,
                90.0,
                2_000L,
                0L
        );

        assertTrue(evaluation.shouldRecalculateRoute());
        assertEquals(RouteDeviationPolicy.Reason.OFF_TRACK, evaluation.rerouteNotice.reason);
        assertEquals(13.0, evaluation.rerouteNotice.offTrackThresholdMeters, 0.0);
    }

    @Test
    public void evaluateLocation_surfacesBearingMismatchRerouteNotice() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavigationSessionRouteState.Evaluation seedEvaluation = state.evaluateLocation(
                location(0.0, 0.00035, 2_000L),
                5f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationSessionRouteState.Evaluation firstMismatchEvaluation = state.evaluateLocation(
                location(0.0, 0.00018, 5_500L),
                5f,
                5f,
                270.0,
                5_500L,
                0L
        );
        NavigationSessionRouteState.Evaluation secondMismatchEvaluation = state.evaluateLocation(
                location(0.0, 0.00008, 6_500L),
                5f,
                5f,
                270.0,
                6_500L,
                0L
        );

        assertFalse(seedEvaluation.shouldRecalculateRoute());
        assertFalse(firstMismatchEvaluation.shouldRecalculateRoute());
        assertFalse(firstMismatchEvaluation.isStableOnRouteSample());
        assertTrue(secondMismatchEvaluation.shouldRecalculateRoute());
        assertEquals(RouteDeviationPolicy.Reason.BEARING_MISMATCH, secondMismatchEvaluation.rerouteNotice.reason);
        assertEquals(180.0, secondMismatchEvaluation.rerouteNotice.bearingDiffDegrees, 0.0);
        assertEquals(90.0, secondMismatchEvaluation.rerouteNotice.expectedBearingDegrees, 0.0);
        assertEquals(270.0, secondMismatchEvaluation.rerouteNotice.actualBearingDegrees, 0.0);
    }

    @Test
    public void evaluateLocation_requiresConfirmationForNearThresholdOffTrackSamples() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavigationSessionRouteState.Evaluation firstEvaluation = state.evaluateLocation(
                location(0.00018, 0.0001, 2_000L),
                5f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationSessionRouteState.Evaluation secondEvaluation = state.evaluateLocation(
                location(0.00018, 0.00015, 3_000L),
                5f,
                5f,
                90.0,
                3_000L,
                0L
        );

        assertFalse(firstEvaluation.shouldRecalculateRoute());
        assertFalse(firstEvaluation.isStableOnRouteSample());
        assertTrue(secondEvaluation.shouldRecalculateRoute());
        assertEquals(RouteDeviationPolicy.Reason.OFF_TRACK, secondEvaluation.rerouteNotice.reason);
    }

    @Test
    public void evaluateLocation_usesMedianAccuracyInsteadOfSingleGpsSpikeForOffTrackThreshold() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                1.0f,
                500L
        );

        state.evaluateLocation(location(0.0, 0.00005, 2_000L), 1.0f, 5f, 90.0, 2_000L, 0L);
        state.evaluateLocation(location(0.0, 0.00010, 3_000L), 1.0f, 5f, 90.0, 3_000L, 0L);
        state.evaluateLocation(location(0.0, 0.00015, 4_000L), 1.0f, 5f, 90.0, 4_000L, 0L);

        NavigationSessionRouteState.Evaluation firstEvaluation = state.evaluateLocation(
                location(0.00018, 0.00012, 5_000L, 30f),
                1.0f,
                30f,
                90.0,
                5_000L,
                0L
        );
        NavigationSessionRouteState.Evaluation secondEvaluation = state.evaluateLocation(
                location(0.00018, 0.00017, 6_000L, 30f),
                1.0f,
                30f,
                90.0,
                6_000L,
                0L
        );

        assertFalse(firstEvaluation.shouldRecalculateRoute());
        assertTrue(secondEvaluation.shouldRecalculateRoute());
        assertEquals(RouteDeviationPolicy.Reason.OFF_TRACK, secondEvaluation.rerouteNotice.reason);
        assertEquals(13.0, secondEvaluation.rerouteNotice.offTrackThresholdMeters, 0.0);
    }

    @Test
    public void evaluateLocation_allowsFasterImmediateOffTrackRerouteAtHigherSpeed() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                10f,
                500L
        );

        NavigationSessionRouteState.Evaluation evaluation = state.evaluateLocation(
                location(0.00018, 0.0, 2_000L),
                10f,
                5f,
                90.0,
                2_000L,
                0L
        );

        assertTrue(evaluation.shouldRecalculateRoute());
        assertEquals(RouteDeviationPolicy.Reason.OFF_TRACK, evaluation.rerouteNotice.reason);
        assertEquals(13.0, evaluation.rerouteNotice.offTrackThresholdMeters, 0.0);
    }

    @Test
    public void evaluateLocation_usesForwardLookaheadBearingNearTurns() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.00018, 0.00018),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithSharpTurn(),
                location(0.0, 0.0, 1_000L),
                1.4f,
                500L
        );

        NavigationSessionRouteState.Evaluation evaluation = state.evaluateLocation(
                location(0.0, 0.00016, 2_000L),
                1.4f,
                5f,
                0.0,
                2_000L,
                0L
        );
        Double routeBearingDegrees = state.currentSegmentBearingDegrees(location(0.0, 0.00016, 2_000L));

        assertFalse(evaluation.shouldRecalculateRoute());
        assertTrue(evaluation.isStableOnRouteSample());
        assertEquals(0.0, routeBearingDegrees, 20.0);
    }

    @Test
    public void evaluateLocation_doesNotRerouteOnBearingMismatchWhenAlongTrackProgressIsForward() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                1.2f,
                500L
        );

        NavigationSessionRouteState.Evaluation seedEvaluation = state.evaluateLocation(
                location(0.0, 0.00008, 2_000L),
                1.2f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationSessionRouteState.Evaluation mismatchEvaluation = state.evaluateLocation(
                location(0.0, 0.00018, 5_500L),
                1.2f,
                5f,
                180.0,
                5_500L,
                0L
        );

        assertFalse(seedEvaluation.shouldRecalculateRoute());
        assertFalse(mismatchEvaluation.shouldRecalculateRoute());
        assertTrue(mismatchEvaluation.isStableOnRouteSample());
    }

    @Test
    public void evaluateLocation_reroutesOnBearingMismatchWhenAlongTrackProgressIsBackward() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithHint(),
                location(0.0, 0.0, 1_000L),
                1.2f,
                500L
        );

        NavigationSessionRouteState.Evaluation seedEvaluation = state.evaluateLocation(
                location(0.0, 0.00035, 2_000L),
                1.2f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationSessionRouteState.Evaluation firstMismatchEvaluation = state.evaluateLocation(
                location(0.0, 0.00018, 5_500L),
                1.2f,
                5f,
                270.0,
                5_500L,
                0L
        );
        NavigationSessionRouteState.Evaluation secondMismatchEvaluation = state.evaluateLocation(
                location(0.0, 0.00008, 6_500L),
                1.2f,
                5f,
                270.0,
                6_500L,
                0L
        );

        assertFalse(seedEvaluation.shouldRecalculateRoute());
        assertFalse(firstMismatchEvaluation.shouldRecalculateRoute());
        assertFalse(firstMismatchEvaluation.isStableOnRouteSample());
        assertTrue(secondMismatchEvaluation.shouldRecalculateRoute());
        assertEquals(RouteDeviationPolicy.Reason.BEARING_MISMATCH, secondMismatchEvaluation.rerouteNotice.reason);
    }

    @Test
    public void addBlockedPointsAhead_escalatesNearbyRepeatsAndReplacesOldMarkers() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        );
        Location currentLocation = location(0.0, 0.0, 1_000L);
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithoutHints(),
                currentLocation,
                5f,
                500L
        );

        List<NogoPoint> first = state.addBlockedPointsAhead(currentLocation, 10_000L);
        List<NogoPoint> second = state.addBlockedPointsAhead(currentLocation, 12_000L);

        assertEquals(1, first.size());
        assertEquals(12.0, first.get(0).radiusMeters, 0.0);
        assertEquals(2, second.size());
        assertEquals(18.0, second.get(0).radiusMeters, 0.0);
        assertEquals(18.0, second.get(1).radiusMeters, 0.0);
        assertEquals(2, state.copyBlockedPoints().size());
    }

    @Test
    public void buildState_keepsRouteVisibleAndShowsFriendlyNoRouteNotice() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        );
        Location currentLocation = location(0.0, 0.0, 1_000L);
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithoutHints(),
                currentLocation,
                5f,
                500L
        );

        NavState navState = state.buildState(
                context,
                currentLocation,
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
                BRouterRouteException.fromTextResponse("no track found at pass=0")
        );

        assertTrue(navState.detailBlock.contains(
                context.getString(R.string.nav_route_notice_no_alternative_keep_current)));
        assertTrue(navState.destinationLine.contains(context.getString(R.string.nav_destination_label)));
    }

    @Test
    public void buildState_afterStationaryPauseReusesLastReliableMovingCompassRadius() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.09),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                new GeoJsonRoute(
                        Arrays.asList(
                                new LatLon(0.0, 0.0),
                                new LatLon(0.0, 0.03),
                                new LatLon(0.0, 0.06),
                                new LatLon(0.0, 0.09)
                        ),
                        Collections.emptyList(),
                        6_000.0,
                        9_999.0
                ),
                locationWithSpeed(0.0, 0.0, 1_000L, 20f),
                20f,
                500L
        );

        NavState movingState = state.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 1_000L, 20f),
                20f,
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
        NavState stationaryState = state.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 2_000L, 0f),
                0f,
                true,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                2_000L,
                false,
                null,
                null
        );
        NavState resumedState = state.buildState(
                context,
                location(0.0, 0.0005, 3_000L),
                20f,
                false,
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

        assertTrue(stationaryState.compassState.visibleRadiusMeters >= movingState.compassState.visibleRadiusMeters);
        assertEquals(
                movingState.compassState.visibleRadiusMeters,
                resumedState.compassState.visibleRadiusMeters,
                0.01f
        );
    }

    @Test
    public void buildState_stationaryOverviewTransitionUsesFixedTwoSecondDurationAcrossRouteLengths() {
        Context context = ApplicationProvider.getApplicationContext();

        NavigationSessionRouteState shortRouteState = new NavigationSessionRouteState();
        NavigationRequest shortRouteRequest = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.018),
                Collections.emptyList()
        );
        shortRouteState.applyRouteResult(
                context,
                shortRouteRequest,
                snapshot(shortRouteRequest),
                new GeoJsonRoute(
                        Arrays.asList(
                                new LatLon(0.0, 0.0),
                                new LatLon(0.0, 0.006),
                                new LatLon(0.0, 0.012),
                                new LatLon(0.0, 0.018)
                        ),
                        Collections.emptyList(),
                        2_000.0,
                        1_999.0
                ),
                locationWithSpeed(0.0, 0.0, 1_000L, 20f),
                20f,
                500L
        );

        NavigationSessionRouteState longRouteState = new NavigationSessionRouteState();
        NavigationRequest longRouteRequest = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.18),
                Collections.emptyList()
        );
        longRouteState.applyRouteResult(
                context,
                longRouteRequest,
                snapshot(longRouteRequest),
                new GeoJsonRoute(
                        Arrays.asList(
                                new LatLon(0.0, 0.0),
                                new LatLon(0.0, 0.06),
                                new LatLon(0.0, 0.12),
                                new LatLon(0.0, 0.18)
                        ),
                        Collections.emptyList(),
                        20_000.0,
                        19_998.0
                ),
                locationWithSpeed(0.0, 0.0, 1_000L, 20f),
                20f,
                500L
        );

        NavState shortMovingState = shortRouteState.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 1_000L, 20f),
                20f,
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
        NavState longMovingState = longRouteState.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 1_000L, 20f),
                20f,
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

        shortRouteState.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 2_000L, 0f),
                0f,
                true,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                2_000L,
                false,
                null,
                null
        );
        longRouteState.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 2_000L, 0f),
                0f,
                true,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                2_000L,
                false,
                null,
                null
        );

        NavState shortMidTransitionState = shortRouteState.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 3_000L, 0f),
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
        NavState longMidTransitionState = longRouteState.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 3_000L, 0f),
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
        NavState shortSettledOverviewState = shortRouteState.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 4_000L, 0f),
                0f,
                true,
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
        NavState longSettledOverviewState = longRouteState.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 4_000L, 0f),
                0f,
                true,
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

        float shortProgress = normalizedTransitionProgress(
                shortMovingState.compassState.visibleRadiusMeters,
                shortMidTransitionState.compassState.visibleRadiusMeters,
                shortSettledOverviewState.compassState.visibleRadiusMeters
        );
        float longProgress = normalizedTransitionProgress(
                longMovingState.compassState.visibleRadiusMeters,
                longMidTransitionState.compassState.visibleRadiusMeters,
                longSettledOverviewState.compassState.visibleRadiusMeters
        );

        assertEquals(0.5f, shortProgress, 0.08f);
        assertEquals(0.5f, longProgress, 0.08f);
        assertEquals(shortProgress, longProgress, 0.05f);
        assertTrue(shortSettledOverviewState.compassState.visibleRadiusMeters
                > shortMovingState.compassState.visibleRadiusMeters);
        assertTrue(longSettledOverviewState.compassState.visibleRadiusMeters
                > longMovingState.compassState.visibleRadiusMeters);
    }

    @Test
    public void buildState_withoutActiveRouteShowsFriendlyNoRouteMessage() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();

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
                BRouterRouteException.fromTextResponse("no track found at pass=0")
        );

        assertEquals(context.getString(R.string.nav_route_unavailable_title), navState.nextLine);
        assertTrue(navState.detailBlock.contains(context.getString(R.string.nav_route_notice_no_route_found)));
    }

    @Test
    public void buildState_keepsIntermediateStopProgressSeparateFromDetailNoticeArea() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.003),
                Collections.singletonList(new LatLon(0.0, 0.002))
        );
        Location currentLocation = location(0.0, 0.0, 1_000L);
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithoutHints(),
                currentLocation,
                5f,
                500L
        );

        NavState navState = state.buildState(
                context,
                currentLocation,
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

        assertTrue(navState.destinationLine.contains(context.getString(R.string.nav_destination_label)));
        assertTrue(navState.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 1)));
        assertTrue(navState.detailBlock.isEmpty());
    }

    @Test
    public void buildState_showsOnlyNextIntermediateStopAhead() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.003),
                Arrays.asList(new LatLon(0.0, 0.001), new LatLon(0.0, 0.002))
        );
        state.applyRouteResult(
                context,
                request,
                snapshot(request),
                routeWithoutHints(),
                location(0.0, 0.0, 1_000L),
                5f,
                500L
        );

        NavState beforeFirstStop = state.buildState(
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
        NavState afterFirstStop = state.buildState(
                context,
                location(0.0, 0.0015, 2_000L),
                5f,
                false,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                2_000L,
                false,
                null,
                null
        );

        assertTrue(beforeFirstStop.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 1)));
        assertFalse(beforeFirstStop.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 2)));
        assertTrue(afterFirstStop.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 2)));
        assertFalse(afterFirstStop.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 1)));
    }

    @Test
    public void buildState_showsBlockedRoadNoticeWhileRouteRecalculationIsRunning() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        Location currentLocation = location(0.0, 0.0, 1_000L);

        NavState navState = state.buildState(
                context,
                currentLocation,
                5f,
                false,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                1_000L,
                true,
                context.getString(R.string.nav_route_notice_blocked_road_recalculating),
                null
        );

        assertTrue(navState.detailBlock.contains(context.getString(R.string.nav_route_notice_blocked_road_recalculating)));
        assertTrue(navState.detailBlock.contains(context.getString(R.string.nav_calculating_route_body)));
    }

    @NonNull
    private static NavigationSession.RouteRequestSnapshot snapshot(@NonNull NavigationRequest request) {
        return new NavigationSession.RouteRequestSnapshot(
                1,
                1,
                new LatLon(0.0, 0.0),
                request.stops,
                request.destination,
                request.profile,
                Collections.emptyList()
        );
    }

    @NonNull
    private static GeoJsonRoute routeWithHint() {
        return new GeoJsonRoute(
                Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001)),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                60.0,
                111.0
        );
    }

    @NonNull
    private static GeoJsonRoute routeWithoutHints() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003)
                ),
                Collections.emptyList(),
                180.0,
                333.0
        );
    }

    @NonNull
    private static GeoJsonRoute routeWithSharpTurn() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.00018),
                        new LatLon(0.00018, 0.00018)
                ),
                Collections.emptyList(),
                40.0,
                40.0
        );
    }

    @NonNull
    private static Location location(double lat, double lon, long timeMs) {
        return location(lat, lon, timeMs, 5f);
    }

    @NonNull
    private static Location locationWithSpeed(double lat, double lon, long timeMs, float speedMetersPerSecond) {
        Location location = location(lat, lon, timeMs, 5f);
        location.setSpeed(speedMetersPerSecond);
        return location;
    }

    @NonNull
    private static Location location(double lat, double lon, long timeMs, float accuracyMeters) {
        Location location = new Location("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(accuracyMeters);
        return location;
    }

    private static float normalizedTransitionProgress(float start, float current, float end) {
        if (Math.abs(end - start) < 0.01f) {
            return 1f;
        }
        return (current - start) / (end - start);
    }
}
