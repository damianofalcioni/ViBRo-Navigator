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
public class NavigationSessionRouteStateDeviationTest extends NavigationSessionRouteStateTestSupport {
    @Test
    public void evaluateLocation_surfacesOffTrackRerouteNotice() {
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
    public void evaluateLocation_suppressesImmediateDeviationDuringReacquisition() {
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

        NavigationRouteEvaluation reacquiringEvaluation = state.evaluateLocation(
                location(0.0003, 0.0, 20_000L),
                5f,
                false,
                5f,
                90.0,
                20_000L,
                80_000L,
                true
        );
        NavigationRouteEvaluation followUpEvaluation = state.evaluateLocation(
                location(0.0003, 0.0, 21_000L),
                5f,
                5f,
                90.0,
                21_000L,
                80_000L
        );

        assertFalse(reacquiringEvaluation.shouldRecalculateRoute());
        assertFalse(reacquiringEvaluation.isStableOnRouteSample());
        assertEquals(1_000L, reacquiringEvaluation.getSuggestedUpdateIntervalMs());
        assertTrue(followUpEvaluation.shouldRecalculateRoute());
        assertEquals(RouteDeviationPolicy.Reason.OFF_TRACK, followUpEvaluation.rerouteNotice.reason);
    }

    @Test
    public void evaluateLocation_surfacesBearingMismatchRerouteNotice() {
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

        NavigationRouteEvaluation seedEvaluation = state.evaluateLocation(
                location(0.0, 0.00035, 2_000L),
                5f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationRouteEvaluation firstMismatchEvaluation = state.evaluateLocation(
                location(0.0, 0.00018, 5_500L),
                5f,
                5f,
                270.0,
                5_500L,
                0L
        );
        NavigationRouteEvaluation secondMismatchEvaluation = state.evaluateLocation(
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

        NavigationRouteEvaluation firstEvaluation = state.evaluateLocation(
                location(0.00018, 0.0001, 2_000L),
                5f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationRouteEvaluation secondEvaluation = state.evaluateLocation(
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
                1.0f,
                500L
        );

        state.evaluateLocation(location(0.0, 0.00005, 2_000L), 1.0f, 5f, 90.0, 2_000L, 0L);
        state.evaluateLocation(location(0.0, 0.00010, 3_000L), 1.0f, 5f, 90.0, 3_000L, 0L);
        state.evaluateLocation(location(0.0, 0.00015, 4_000L), 1.0f, 5f, 90.0, 4_000L, 0L);

        NavigationRouteEvaluation firstEvaluation = state.evaluateLocation(
                location(0.00018, 0.00012, 5_000L, 30f),
                1.0f,
                30f,
                90.0,
                5_000L,
                0L
        );
        NavigationRouteEvaluation secondEvaluation = state.evaluateLocation(
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
                10f,
                500L
        );

        NavigationRouteEvaluation evaluation = state.evaluateLocation(
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
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.00018, 0.00018),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
                snapshot(request),
                routeWithSharpTurn(),
                location(0.0, 0.0, 1_000L),
                1.4f,
                500L
        );

        NavigationRouteEvaluation evaluation = state.evaluateLocation(
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
                1.2f,
                500L
        );

        NavigationRouteEvaluation seedEvaluation = state.evaluateLocation(
                location(0.0, 0.00008, 2_000L),
                1.2f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationRouteEvaluation mismatchEvaluation = state.evaluateLocation(
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
                1.2f,
                500L
        );

        NavigationRouteEvaluation seedEvaluation = state.evaluateLocation(
                location(0.0, 0.00035, 2_000L),
                1.2f,
                5f,
                90.0,
                2_000L,
                0L
        );
        NavigationRouteEvaluation firstMismatchEvaluation = state.evaluateLocation(
                location(0.0, 0.00018, 5_500L),
                1.2f,
                5f,
                270.0,
                5_500L,
                0L
        );
        NavigationRouteEvaluation secondMismatchEvaluation = state.evaluateLocation(
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
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        );
        Location currentLocation = location(0.0, 0.0, 1_000L);
        state.applyRouteResult(
                context,
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
}
