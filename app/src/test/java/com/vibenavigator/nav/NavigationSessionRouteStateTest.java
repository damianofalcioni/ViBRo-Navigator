package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.R;
import com.vibenavigator.brouter.BRouterRouteException;
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.VoiceHint;

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
                5f,
                NavState.NO_DEADLINE,
                1_000L,
                false,
                null
        );

        assertEquals(1, turnEvents.size());
        assertEquals(NavigationSession.TurnEvent.Type.INITIAL, turnEvents.get(0).type);
        assertFalse(navState.nextLine.isEmpty());
        assertTrue(navState.remainingBlock.contains(context.getString(R.string.label_destination)));
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
        assertEquals(15.0, evaluation.rerouteNotice.offTrackThresholdMeters, 0.0);
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

        NavigationSessionRouteState.Evaluation evaluation = state.evaluateLocation(
                location(0.0, 0.0001, 2_000L),
                5f,
                5f,
                180.0,
                2_000L,
                0L
        );

        assertTrue(evaluation.shouldRecalculateRoute());
        assertEquals(RouteDeviationPolicy.Reason.BEARING_MISMATCH, evaluation.rerouteNotice.reason);
        assertEquals(90.0, evaluation.rerouteNotice.bearingDiffDegrees, 0.0);
        assertEquals(90.0, evaluation.rerouteNotice.expectedBearingDegrees, 0.0);
        assertEquals(180.0, evaluation.rerouteNotice.actualBearingDegrees, 0.0);
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
                5f,
                NavState.NO_DEADLINE,
                1_000L,
                false,
                BRouterRouteException.fromTextResponse("no track found at pass=0")
        );

        assertTrue(navState.remainingBlock.contains(
                context.getString(R.string.nav_route_notice_no_alternative_keep_current)));
        assertTrue(navState.remainingBlock.contains(context.getString(R.string.label_destination)));
    }

    @Test
    public void buildState_withoutActiveRouteShowsFriendlyNoRouteMessage() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSessionRouteState state = new NavigationSessionRouteState();

        NavState navState = state.buildState(
                context,
                location(0.0, 0.0, 1_000L),
                5f,
                5f,
                NavState.NO_DEADLINE,
                1_000L,
                false,
                BRouterRouteException.fromTextResponse("no track found at pass=0")
        );

        assertEquals(context.getString(R.string.nav_route_unavailable_title), navState.nextLine);
        assertTrue(navState.remainingBlock.contains(context.getString(R.string.nav_route_notice_no_route_found)));
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
    private static Location location(double lat, double lon, long timeMs) {
        Location location = new Location("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(5f);
        return location;
    }
}
