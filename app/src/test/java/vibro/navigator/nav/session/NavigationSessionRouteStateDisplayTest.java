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

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterRouteException;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NavigationSessionRouteStateDisplayTest extends NavigationSessionRouteStateTestSupport {
    @Test
    public void buildState_keepsRouteVisibleAndShowsFriendlyNoRouteNotice() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        );
        NavigationLocation currentLocation = location(0.0, 0.0, 1_000L);
        state.applyRouteResult(
                context,
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

        assertTrue(navState.routeStatus.progress.detailBlock.contains(
                context.getString(R.string.nav_route_notice_no_alternative_keep_current)));
        assertTrue(navState.routeStatus.progress.destinationLine.contains(context.getString(R.string.nav_destination_label)));
    }

    @Test
    public void buildState_afterStationaryPauseReusesLastReliableMovingCompassRadius() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.09),
                Collections.emptyList()
        );
        state.applyRouteResult(
                context,
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

        assertTrue(stationaryState.routeStatus.compassState.radiusState.visibleRadiusMeters >= movingState.routeStatus.compassState.radiusState.visibleRadiusMeters);
        assertEquals(
                movingState.routeStatus.compassState.radiusState.visibleRadiusMeters,
                resumedState.routeStatus.compassState.radiusState.visibleRadiusMeters,
                0.01f
        );
    }

    @Test
    public void buildState_stationaryOverviewTransitionUsesFixedOneSecondDurationAcrossRouteLengths() {
        NavigationTextResources context = TestNavigationTextResources.metric();

        NavigationSessionRouteState shortRouteState = new NavigationSessionRouteState();
        NavigationRequest shortRouteRequest = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.018),
                Collections.emptyList()
        );
        shortRouteState.applyRouteResult(
                context,
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
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.18),
                Collections.emptyList()
        );
        longRouteState.applyRouteResult(
                context,
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
                locationWithSpeed(0.0, 0.0, 2_500L, 0f),
                0f,
                true,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                2_500L,
                false,
                null,
                null
        );
        NavState longMidTransitionState = longRouteState.buildState(
                context,
                locationWithSpeed(0.0, 0.0, 2_500L, 0f),
                0f,
                true,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                2_500L,
                false,
                null,
                null
        );
        NavState shortSettledOverviewState = shortRouteState.buildState(
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
        NavState longSettledOverviewState = longRouteState.buildState(
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

        float shortProgress = normalizedTransitionProgress(
                shortMovingState.routeStatus.compassState.radiusState.visibleRadiusMeters,
                shortMidTransitionState.routeStatus.compassState.radiusState.visibleRadiusMeters,
                shortSettledOverviewState.routeStatus.compassState.radiusState.visibleRadiusMeters
        );
        float longProgress = normalizedTransitionProgress(
                longMovingState.routeStatus.compassState.radiusState.visibleRadiusMeters,
                longMidTransitionState.routeStatus.compassState.radiusState.visibleRadiusMeters,
                longSettledOverviewState.routeStatus.compassState.radiusState.visibleRadiusMeters
        );

        assertEquals(0.5f, shortProgress, 0.08f);
        assertEquals(0.5f, longProgress, 0.08f);
        assertEquals(shortProgress, longProgress, 0.05f);
        assertTrue(shortSettledOverviewState.routeStatus.compassState.radiusState.visibleRadiusMeters
                > shortMovingState.routeStatus.compassState.radiusState.visibleRadiusMeters);
        assertTrue(longSettledOverviewState.routeStatus.compassState.radiusState.visibleRadiusMeters
                > longMovingState.routeStatus.compassState.radiusState.visibleRadiusMeters);
    }

    @Test
    public void buildState_withoutActiveRouteShowsFriendlyNoRouteMessage() {
        NavigationTextResources context = TestNavigationTextResources.metric();
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

        assertEquals(context.getString(R.string.nav_route_unavailable_title), navState.routeStatus.guidance.nextLine);
        assertTrue(navState.routeStatus.progress.detailBlock.contains(context.getString(R.string.nav_route_notice_no_route_found)));
    }

    @Test
    public void buildState_keepsIntermediateStopProgressSeparateFromDetailNoticeArea() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.singletonList(new LatLon(0.0, 0.002))
        );
        NavigationLocation currentLocation = location(0.0, 0.0, 1_000L);
        state.applyRouteResult(
                context,
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

        assertTrue(navState.routeStatus.progress.destinationLine.contains(context.getString(R.string.nav_destination_label)));
        assertTrue(navState.routeStatus.progress.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 1)));
        assertTrue(navState.routeStatus.progress.detailBlock.isEmpty());
    }

    @Test
    public void buildState_showsOnlyNextIntermediateStopAhead() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Arrays.asList(new LatLon(0.0, 0.001), new LatLon(0.0, 0.002))
        );
        state.applyRouteResult(
                context,
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

        assertTrue(beforeFirstStop.routeStatus.progress.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 1)));
        assertFalse(beforeFirstStop.routeStatus.progress.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 2)));
        assertTrue(afterFirstStop.routeStatus.progress.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 2)));
        assertFalse(afterFirstStop.routeStatus.progress.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 1)));
    }

    @Test
    public void buildState_showsBlockedRoadNoticeWhileRouteRecalculationIsRunning() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationLocation currentLocation = location(0.0, 0.0, 1_000L);

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

        assertTrue(navState.routeStatus.progress.detailBlock.contains(context.getString(R.string.nav_route_notice_blocked_road_recalculating)));
        assertTrue(navState.routeStatus.progress.detailBlock.contains(context.getString(R.string.nav_calculating_route_body)));
    }
}
