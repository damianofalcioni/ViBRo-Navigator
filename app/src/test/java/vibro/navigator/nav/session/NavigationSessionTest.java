package vibro.navigator.nav.session;


import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.format.TestNavigationTextResources;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NavigationSessionTest {
    private static final String DESTINATION = "Destination";
    private static final String TREKKING_PROFILE = "trekking";

    @Test
    public void buildState_marksPausedSessionsAndClearsPauseStateOnResume() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        ));

        assertTrue(NavigationSessionResourceAdapter.start(session, context, 0L));
        assertTrue(session.pause());

        NavState pausedState = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                0L,
                null,
                null,
                null
        );

        assertTrue(pausedState.pauseStatus.paused);
        assertTrue(pausedState.routeStatus.progress.detailBlock.contains(context.getString(R.string.nav_paused_notice)));
        assertTrue(session.resume());

        NavState resumedState = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                0L,
                null,
                null,
                null
        );

        assertFalse(resumedState.pauseStatus.paused);
    }

    @Test
    public void prepareRouteRequest_excludesReachedIntermediateStops() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Arrays.asList(new LatLon(0.0, 0.001), new LatLon(0.0, 0.002))
        ));
        assertTrue(NavigationSessionResourceAdapter.start(session, context, 0L));
        long nowMs = System.currentTimeMillis();
        NavigationSessionResourceAdapter.onRawLocationChanged(session, context, location(0.0, 0.0, nowMs), nowMs);
        NavigationRouteRequestSnapshot firstSnapshot = session.prepareRouteRequest(true, nowMs);
        assertNotNull(firstSnapshot);
        assertEquals(2, firstSnapshot.intermediates.size());
        NavigationSessionResourceAdapter.applyRouteResult(session, context, firstSnapshot, routeWithoutHints(), 500L);

        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                location(0.0, 0.001, nowMs + 2_000L, 120f),
                nowMs + 2_000L
        );
        NavigationRouteRequestSnapshot secondSnapshot = session.prepareRouteRequest(true, nowMs + 3_000L);

        assertNotNull(secondSnapshot);
        assertEquals(1, secondSnapshot.intermediates.size());
        assertEquals(0.002, secondSnapshot.intermediates.get(0).lon, 0.0);
    }

    @Test
    public void buildState_includesAcceptedFixCountInGpsStatus() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        ));
        assertTrue(NavigationSessionResourceAdapter.start(session, context, 0L));
        long nowMs = System.currentTimeMillis();

        NavigationSessionResourceAdapter.onRawLocationChanged(session, context, location(0.0, 0.0, nowMs), nowMs);
        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                location(0.0, 0.0001, nowMs + 1_000L),
                nowMs + 1_000L
        );

        NavState state = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                nowMs + 1_000L,
                7,
                null,
                null
        );

        assertTrue(state.gpsStatus.statusLine.contains("(7)"));
        assertEquals("#2", state.gpsStatus.telemetry.acquiredFixCountText);
    }

    @Test
    public void onRawLocationChanged_resumesFastPollingAfterLongAcceptedFixGap() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        ));
        long nowMs = System.currentTimeMillis();
        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));

        NavigationSessionResourceAdapter.onRawLocationChanged(session, context, location(0.0, 0.0, nowMs), nowMs);
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, nowMs);
        assertNotNull(snapshot);
        NavigationSessionResourceAdapter.applyRouteResult(session, context, snapshot, routeWithoutHints(), nowMs);
        for (int i = 1; i <= 5; i++) {
            long sampleTimeMs = nowMs + i * 1_000L;
            NavigationSessionResourceAdapter.onRawLocationChanged(
                    session,
                    context,
                    location(0.0, i * 0.0001, sampleTimeMs),
                    sampleTimeMs
            );
        }

        long resumedTimeMs = nowMs + 21_000L;
        NavigationLocationUpdateResult result = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                location(0.0, 0.0006, resumedTimeMs),
                resumedTimeMs
        );

        assertFalse(result.isDropped());
        assertEquals(3_000L, result.getSuggestedUpdateIntervalMs());
    }

    @Test
    public void onRawLocationChanged_keepsDynamicBucketAfterExpectedLongInterval() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        ));
        long nowMs = 1_000L;
        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));

        NavigationSessionResourceAdapter.onRawLocationChanged(session, context, locationWithSpeed(0.0, 0.0, nowMs, 2f), nowMs);
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, nowMs);
        assertNotNull(snapshot);
        NavigationSessionResourceAdapter.applyRouteResult(session, context, snapshot, routeWithoutHints(), nowMs);
        NavigationLocationUpdateResult result = null;
        long sampleTimeMs = nowMs;
        for (int i = 1; i <= 6; i++) {
            sampleTimeMs = nowMs + i * 3_000L;
            result = NavigationSessionResourceAdapter.onRawLocationChanged(
                    session,
                    context,
                    locationWithSpeed(0.0, i * 0.00001, sampleTimeMs, 2f),
                    sampleTimeMs
            );
        }
        assertNotNull(result);
        long dynamicIntervalMs = result.getSuggestedUpdateIntervalMs();
        assertTrue(dynamicIntervalMs > 3_000L);

        long expectedLongIntervalTimeMs = sampleTimeMs + dynamicIntervalMs;
        NavigationLocationUpdateResult longIntervalResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0, 0.00007, expectedLongIntervalTimeMs, 2f),
                expectedLongIntervalTimeMs,
                dynamicIntervalMs
        );

        assertFalse(longIntervalResult.isDropped());
        assertEquals(dynamicIntervalMs, longIntervalResult.getSuggestedUpdateIntervalMs());
    }

    @Test
    public void straightLineModeKeepsGuidanceDirectWithoutRouteRequestOrManeuverEvents() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Arrays.asList(new LatLon(0.0, 0.001), new LatLon(0.0, 0.002))
        ));
        long nowMs = System.currentTimeMillis();

        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));
        NavigationLocationUpdateResult result = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0, 0.0, nowMs, 2f),
                nowMs
        );
        NavState state = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                nowMs,
                null,
                0.0,
                null
        );

        assertFalse(result.shouldRecalculateRoute());
        assertEquals(3_000L, result.getSuggestedUpdateIntervalMs());
        assertTrue(result.turnEvents.isEmpty());
        assertNull(session.prepareRouteRequest(true, nowMs));
        assertTrue(state.routeStatus.guidance.nextLine.contains(
                context.getString(R.string.direction_intermediate_arrive)
        ));
        assertTrue(state.routeStatus.guidance.afterNextLine.contains(
                context.getString(R.string.direction_intermediate_arrive)
        ));
        assertTrue(state.routeStatus.progress.stopProgressBlock.contains(context.getString(R.string.format_stop_label, 1)));
        assertTrue(state.routeStatus.progress.destinationLine.contains(context.getString(R.string.nav_destination_label)));
        assertNotNull(state.routeStatus.compassState);
        assertTrue(state.routeStatus.compassState.hasRouteGeometry());
        assertTrue(state.routeStatus.compassState.displayMode.straightLineMode);
        assertTrue(state.routeStatus.compassState.routeSamplePointCount() > 1);
        assertNotNull(state.routeStatus.compassState.orientationCue);
        assertNull(state.routeStatus.compassState.routeStartApproachProjection);
        assertEquals(2, state.routeStatus.compassState.routeGeometry().intermediateSamplePointCount());
        assertFalse(state.routeStatus.blockedRoadActionAvailable);
        assertFalse(session.canAddBlockedWaypoint());
    }

    @Test
    public void straightLineModeCompassShowsAcceptedFixPathAsPassedBeeline() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                DESTINATION,
                new LatLon(0.0, 0.01),
                Collections.emptyList()
        ));
        long nowMs = 1_000L;

        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));
        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0, 0.0, nowMs, 2f),
                nowMs
        );
        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0, 0.001, nowMs + 1_000L, 2f),
                nowMs + 1_000L
        );
        NavState state = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                nowMs + 1_000L,
                null,
                0.0,
                null
        );

        assertNotNull(state.routeStatus.compassState);
        assertTrue(state.routeStatus.compassState.displayMode.straightLineMode);
        assertEquals(1, state.routeStatus.compassState.archivedPassedRouteSegments().segmentCount());
        assertEquals(2, state.routeStatus.compassState.archivedPassedRouteSegments().samplePointCount(0));
    }

    @Test
    public void roundTripModeKeepsOffTrackNoticeWithoutRouteRecalculationOrBlockedRoadAction() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.ROUND_TRIP,
                TREKKING_PROFILE,
                null,
                null,
                null,
                Collections.emptyList(),
                2_387
        ));
        long nowMs = System.currentTimeMillis();

        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));
        NavigationSessionResourceAdapter.onRawLocationChanged(session, context, locationWithSpeed(0.0, 0.0, nowMs, 5f), nowMs);
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, nowMs);
        assertNotNull(snapshot);
        NavigationSessionResourceAdapter.applyRouteResult(session, context, snapshot, routeWithoutHints(), nowMs);

        NavigationLocationUpdateResult offTrackResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0003, 0.0, nowMs + 2_000L, 5f),
                nowMs + 2_000L
        );
        NavState state = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                nowMs + 2_000L,
                null,
                null,
                null
        );

        assertFalse(offTrackResult.shouldRecalculateRoute());
        assertNotNull(offTrackResult.getRerouteNotice());
        assertFalse(offTrackResult.getRerouteNotice().routeRecalculationExpected);
        assertFalse(state.routeStatus.blockedRoadActionAvailable);
        assertFalse(session.canAddBlockedWaypoint());
    }

    @Test
    public void roundTripModeAlwaysShowsNextManeuverCueInCompass() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.ROUND_TRIP,
                TREKKING_PROFILE,
                null,
                null,
                null,
                Collections.emptyList(),
                2_387
        ));
        long nowMs = System.currentTimeMillis();

        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));
        NavigationSessionResourceAdapter.onRawLocationChanged(session, context, locationWithSpeed(0.0, 0.0, nowMs, 5f), nowMs);
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, nowMs);
        assertNotNull(snapshot);
        NavigationSessionResourceAdapter.applyRouteResult(session, context, snapshot, roundTripRouteWithManeuver(), nowMs);
        NavState state = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                nowMs + 1_000L,
                null,
                90.0,
                null
        );

        assertNotNull(state.routeStatus.compassState);
        assertNotNull(state.routeStatus.compassState.orientationCue);
        assertEquals(0.0f, state.routeStatus.compassState.orientationCue.targetHeadingDegrees, 0.01f);
    }

    @Test
    public void straightLineModeUsesTargetDistanceForUpdateIntervalsAfterWarmup() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                DESTINATION,
                new LatLon(0.0, 0.01),
                Collections.emptyList()
        ));
        long nowMs = 1_000L;

        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));
        NavigationLocationUpdateResult result = null;
        for (int i = 0; i < 6; i++) {
            long sampleTimeMs = nowMs + i * 3_000L;
            result = NavigationSessionResourceAdapter.onRawLocationChanged(
                    session,
                    context,
                    locationWithSpeed(0.0, i * 0.00001, sampleTimeMs, 2f),
                    sampleTimeMs
            );
        }

        assertNotNull(result);
        assertEquals(60_000L, result.getSuggestedUpdateIntervalMs());
    }

    @Test
    public void straightLineModeEmitsWrongDirectionNoticeAfterConfirmedOppositeFixes() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                DESTINATION,
                new LatLon(0.0, 0.01),
                Collections.emptyList()
        ));
        long nowMs = 1_000L;

        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));
        NavigationLocationUpdateResult firstResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithBearing(0.0, 0.002, nowMs, 2f, 270f),
                nowMs
        );
        NavigationLocationUpdateResult secondResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithBearing(0.0, 0.0019, nowMs + 3_000L, 2f, 270f),
                nowMs + 3_000L
        );

        assertNull(firstResult.getWrongDirectionNotice());
        assertFalse(secondResult.shouldRecalculateRoute());
        NavigationWrongDirectionNotice notice = secondResult.getWrongDirectionNotice();
        assertNotNull(notice);
        assertEquals(90.0, notice.expectedBearingDegrees, 1.0);
        assertEquals(270.0, notice.actualBearingDegrees, 0.0);
        assertEquals(180.0, notice.bearingDiffDegrees, 1.0);
    }

    @Test
    public void straightLineModeSumsIntermediateLegsForDestinationProgress() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        LatLon stop = new LatLon(0.0, 0.001);
        LatLon destination = new LatLon(0.001, 0.001);
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                DESTINATION,
                destination,
                Collections.singletonList(stop)
        ));
        long nowMs = 0L;

        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));
        NavigationSessionResourceAdapter.onRawLocationChanged(session, context, locationWithSpeed(0.0, 0.0, nowMs, 2f), nowMs);
        NavState state = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                nowMs,
                null,
                0.0,
                null
        );

        double summedDistanceMeters = GeoMath.distanceMeters(0.0, 0.0, stop.lat, stop.lon)
                + GeoMath.distanceMeters(stop.lat, stop.lon, destination.lat, destination.lon);
        double shortcutDistanceMeters = GeoMath.distanceMeters(0.0, 0.0, destination.lat, destination.lon);
        String destinationLine = state.routeStatus.progress.destinationLine;
        assertTrue(destinationLine.contains(NavigationTextFormatter.formatDistance(context, summedDistanceMeters)));
        assertTrue(destinationLine.contains(NavigationTextFormatter.formatTimeSeconds(context, summedDistanceMeters / 2.0)));
        assertFalse(destinationLine.contains(NavigationTextFormatter.formatDistance(context, shortcutDistanceMeters)));
        assertFalse(destinationLine.contains(NavigationTextFormatter.formatTimeSeconds(context, shortcutDistanceMeters / 2.0)));
    }

    @Test
    public void straightLineModeAdvancesStopsAndCompletesDestinationWithArrivalEvents() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Arrays.asList(new LatLon(0.0, 0.001), new LatLon(0.0, 0.002))
        ));
        long nowMs = System.currentTimeMillis();

        assertTrue(NavigationSessionResourceAdapter.start(session, context, nowMs));
        NavigationSessionResourceAdapter.onRawLocationChanged(session, context, locationWithSpeed(0.0, 0.0, nowMs, 2f), nowMs);
        NavigationLocationUpdateResult stopResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0, 0.001, nowMs + 20_000L, 2f),
                nowMs + 20_000L
        );
        NavigationLocationUpdateResult repeatedStopResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0, 0.001, nowMs + 21_000L, 2f),
                nowMs + 21_000L
        );
        NavState afterFirstStop = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                nowMs + 20_000L,
                null,
                0.0,
                null
        );
        NavigationLocationUpdateResult secondStopResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0, 0.002, nowMs + 40_000L, 2f),
                nowMs + 40_000L
        );
        NavigationLocationUpdateResult destinationResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0, 0.003, nowMs + 60_000L, 2f),
                nowMs + 60_000L
        );
        NavigationLocationUpdateResult repeatedDestinationResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                context,
                locationWithSpeed(0.0, 0.003, nowMs + 61_000L, 2f),
                nowMs + 61_000L
        );
        NavState reachedState = NavigationSessionResourceAdapter.buildState(
                session,
                context,
                NavState.NO_DEADLINE,
                nowMs + 60_000L,
                null,
                0.0,
                null
        );

        assertEquals(1, stopResult.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, stopResult.turnEvents.get(0).type);
        assertEquals(101, stopResult.turnEvents.get(0).hint.command);
        assertTrue(repeatedStopResult.turnEvents.isEmpty());
        assertEquals(1, secondStopResult.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, secondStopResult.turnEvents.get(0).type);
        assertEquals(101, secondStopResult.turnEvents.get(0).hint.command);
        assertEquals(1, destinationResult.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, destinationResult.turnEvents.get(0).type);
        assertEquals(100, destinationResult.turnEvents.get(0).hint.command);
        assertTrue(repeatedDestinationResult.turnEvents.isEmpty());
        assertTrue(afterFirstStop.routeStatus.progress.stopProgressBlock.contains(
                context.getString(R.string.format_stop_label, 2)
        ));
        assertFalse(afterFirstStop.routeStatus.progress.stopProgressBlock.contains(
                context.getString(R.string.format_stop_label, 1)
        ));
        assertNotNull(afterFirstStop.routeStatus.compassState);
        assertEquals(2, afterFirstStop.routeStatus.compassState.routeGeometry().intermediateSamplePointCount());
        assertTrue(reachedState.routeStatus.guidance.nextLine.contains(
                context.getString(R.string.direction_arrive)
        ));
        assertEquals(context.getString(R.string.nav_destination_reached),
                reachedState.routeStatus.progress.destinationLine);
        assertNotNull(reachedState.routeStatus.compassState);
        assertTrue(reachedState.routeStatus.compassState.displayMode.straightLineMode);
        assertNotNull(reachedState.routeStatus.compassState.routeGeometry());
        assertEquals(2, reachedState.routeStatus.compassState.routeGeometry().intermediateSamplePointCount());
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
    private static GeoJsonRoute roundTripRouteWithManeuver() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.001, 0.001),
                        new LatLon(0.0, 0.0)
                ),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, -90)),
                300.0,
                380.0
        );
    }

    @NonNull
    private static NavigationLocation location(double lat, double lon, long timeMs) {
        return location(lat, lon, timeMs, 5f);
    }

    @NonNull
    private static NavigationLocation location(double lat, double lon, long timeMs, float accuracyMeters) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(accuracyMeters);
        return location;
    }

    @NonNull
    private static NavigationLocation locationWithSpeed(
            double lat,
            double lon,
            long timeMs,
            float speedMetersPerSecond
    ) {
        NavigationLocation location = location(lat, lon, timeMs);
        location.setSpeed(speedMetersPerSecond);
        return location;
    }

    @NonNull
    private static NavigationLocation locationWithBearing(
            double lat,
            double lon,
            long timeMs,
            float speedMetersPerSecond,
            float bearingDegrees
    ) {
        NavigationLocation location = locationWithSpeed(lat, lon, timeMs, speedMetersPerSecond);
        location.setBearing(bearingDegrees);
        location.setBearingAccuracyDegrees(5f);
        return location;
    }
}
