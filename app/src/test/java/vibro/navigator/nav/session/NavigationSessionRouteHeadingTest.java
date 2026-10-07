package vibro.navigator.nav.session;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.time.NavigationDisplayTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationSessionRouteHeadingTest {
    private static final long WALL_TIME_MS = 1_791_367_985_000L;

    @Test
    public void distinctClocksKeepFullAndSensorUpdatesOnTheSameBeelineHeading() {
        for (int scenario = 0; scenario < 3; scenario++) {
            assertBeelineClockStability(beelineSession(scenario));
        }
    }

    private static NavigationSession beelineSession(int scenario) {
        NavigationSession session = session(scenario == 0
                ? NavigationRoutingMode.STRAIGHT_LINE : NavigationRoutingMode.BROUTER);
        if (scenario != 0) {
            applyRoute(session, new LatLon(0, scenario == 1 ? 0.001 : 0), scenario == 2);
            evaluateRoute(session, 1_000L);
            assertTrue(session.components.routeState.isBeelineGuidanceActive());
        }
        return session;
    }

    private static void assertBeelineClockStability(NavigationSession session) {
        for (long elapsed = 1_000; elapsed <= 7_000; elapsed += 3_000) {
            if (elapsed != 1_000) {
                accept(session, fix(elapsed));
            }
            // A calendar-clock correction must not change GPS freshness.
            NavState full = buildWithClocks(session, WALL_TIME_MS - elapsed, elapsed);
            assertHeading(full, 84f);
            assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                    session, full, 220.0, 5f, elapsed + 100), 84f);
        }
        NavState stale = buildWithClocks(session, WALL_TIME_MS, 18_000);
        assertHeading(stale, 180f);
        assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                session, stale, 220.0, 5f, 18_100), 220f);
    }

    @Test
    public void routedDisagreementRemainsConfirmedAcrossDifferentClockUpdates() {
        NavigationSession session = session(NavigationRoutingMode.BROUTER);
        applyRoute(session, new LatLon(0, 0), false);
        session.getLastFilteredLocation().setBearing(270f);
        assertHeading(buildWithClocks(session, WALL_TIME_MS, 1_000), 90f);
        NavigationLocation reversed = fix(3_000);
        reversed.setLongitude(-0.00015);
        reversed.setBearing(270f);
        accept(session, reversed);
        NavState state = buildWithClocks(session, WALL_TIME_MS + 2_000, 3_000);
        assertHeading(state, 270f);
        assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                session, state, 220.0, 5f, 3_100), 270f);
        assertHeading(buildWithClocks(session, WALL_TIME_MS + 3_000, 4_000), 270f);
    }

    @Test
    public void arrivalLabelsUseWallTimeWhileHeadingUsesElapsedTime() {
        NavigationSession session = session(NavigationRoutingMode.STRAIGHT_LINE);
        // The target is about 352 m away at 3 m/s, so arrival is about 117 seconds ahead.
        NavState state = buildWithClocks(session, WALL_TIME_MS, 1_000);
        assertTrue(state.routeStatus.progress.destinationLine.contains(
                NavigationTextFormatter.formatEta(WALL_TIME_MS + 117_000)));
        assertHeading(state, 84f);
    }

    private static NavState buildWithClocks(NavigationSession session, long wallTime, long elapsed) {
        return NavigationSessionResourceAdapter.buildState(session, TestNavigationTextResources.metric(),
                NavState.NO_DEADLINE, new NavigationDisplayTime(wallTime, elapsed),
                null, 180.0, 5f, null, true, true);
    }

    @Test
    public void routedNavigationAndHeadingRefreshUseRouteInsteadOfGpsOrCompass() {
        for (NavigationRoutingMode mode : new NavigationRoutingMode[] {
                NavigationRoutingMode.BROUTER, NavigationRoutingMode.ROUND_TRIP}) {
            NavigationSession session = session(mode);
            applyRoute(session, new LatLon(0, 0), false);
            NavState state = build(session, 180.0, 1_000L);
            assertHeading(state, 90f);
            assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                    session, state, 220.0, 5f, 2_000L), 90f);
            // Route geometry still supplies the heading between infrequent GPS fixes.
            assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                    session, state, 240.0, 5f, 60_000L), 90f);
        }
    }

    @Test
    public void straightLineNavigationUsesAccurateGpsThenLiveCompassWhenAccuracyDegrades() {
        NavigationSession session = session(NavigationRoutingMode.STRAIGHT_LINE);
        NavState state = build(session, 180.0, 1_000L);
        assertHeading(state, 84f);
        session.getLastFilteredLocation().setBearingAccuracyDegrees(40f);
        assertHeading(build(session, 200.0, 2_000L), 200f);
        assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                session, state, 220.0, 5f, 2_100L), 220f);
    }

    @Test
    public void routeStartApproachUsesGpsAndCompassInsteadOfTheRouteSegment() {
        NavigationSession session = session(NavigationRoutingMode.BROUTER);
        applyRoute(session, new LatLon(0, 0.001), false);
        assertTrue(session.components.routeState.isBeelineGuidanceActive());
        assertHeading(build(session, 180.0, 1_000L), 84f);
        session.getLastFilteredLocation().setBearingAccuracyDegrees(40f);
        NavState state = build(session, 200.0, 2_000L);
        assertHeading(state, 200f);
        assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                session, state, 220.0, 5f, 2_100L), 220f);
    }

    @Test
    public void nativeBeelineUsesGpsAndCompassThenReturnsToRouteHeadingAtItsTarget() {
        NavigationSession session = session(NavigationRoutingMode.BROUTER);
        applyRoute(session, new LatLon(0, 0), true);
        evaluateRoute(session, 1_000L);
        assertTrue(session.components.routeState.isBeelineGuidanceActive());
        assertHeading(build(session, 180.0, 1_000L), 84f);
        session.getLastFilteredLocation().setBearingAccuracyDegrees(40f);
        assertHeading(build(session, 200.0, 2_000L), 200f);

        NavigationLocation reached = fix(61_000L);
        reached.setLongitude(0.001);
        accept(session, reached);
        evaluateRoute(session, 61_000L);
        assertFalse(session.components.routeState.isBeelineGuidanceActive());
        assertHeading(build(session, 220.0, 61_000L), 0f);
    }

    @Test
    public void stoppingOnRouteRetainsHeadingUntilTheExistingCompassTurnGateActivates() {
        NavigationSessionHeadingResolver resolver = new NavigationSessionHeadingResolver(
                new NavigationSessionLocationState());
        NavigationLocation location = fix(1_000L);
        assertEquals(90.0, resolver.selectHeading(location, false, 180.0, 5f,
                1_000L, 90.0, false).headingDegrees, 0.0);
        assertEquals(90.0, resolver.selectHeading(location, true, 180.0, 5f,
                2_000L, 0.0, false).headingDegrees, 0.0);
        resolver.selectHeading(location, true, 220.0, 5f, 3_000L, 0.0, false);
        assertEquals(220.0, resolver.selectHeading(location, true, 220.0, 5f,
                4_000L, 0.0, false).headingDegrees, 0.0);
        assertEquals(225.0, resolver.selectHeading(location, true, 225.0, 5f,
                4_100L, 0.0, false).headingDegrees, 0.0);
        assertEquals(0.0, resolver.selectHeading(location, false, 225.0, 5f,
                5_000L, 0.0, false).headingDegrees, 0.0);
    }

    @Test
    public void missingOrInvalidRouteHeadingUsesCompassWithoutTakingGpsHeading() {
        NavigationSessionHeadingResolver resolver = new NavigationSessionHeadingResolver(
                new NavigationSessionLocationState());
        NavigationLocation location = fix(1_000L);
        assertEquals(180.0, resolver.selectHeading(location, false, 180.0, 5f,
                1_000L, null, false).headingDegrees, 0.0);
        assertEquals(220.0, resolver.selectHeading(location, false, 220.0, 5f,
                2_000L, Double.NaN, false).headingDegrees, 0.0);
    }

    @Test
    public void routedAndRoundTripSnapshotsAndHeadingRefreshesShareConfirmedGpsFallback() {
        for (NavigationRoutingMode mode : new NavigationRoutingMode[] {
                NavigationRoutingMode.BROUTER, NavigationRoutingMode.ROUND_TRIP}) {
            NavigationSession session = session(mode);
            applyRoute(session, new LatLon(0, 0), false);
            session.getLastFilteredLocation().setBearing(270f);
            NavState initial = build(session, 180.0, 1_000L);
            assertHeading(initial, 90f);
            assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                    session, initial, 220.0, 5f, 2_000L), 90f);
            NavigationLocation backwards = fix(3_000);
            backwards.setLongitude(-0.00015);
            backwards.setBearing(270f);
            accept(session, backwards);
            NavState reversed = build(session, 180.0, 3_000);
            assertHeading(reversed, 270f);
            assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                    session, reversed, 220.0, 5f, 3_100), 270f);
            assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                    session, reversed, 220.0, 5f, 14_000), 90f);
            assertTrue(session.pause());
            assertTrue(session.resume());
            assertHeading(build(session, 180.0, 3_200), 90f);
        }
    }

    @Test
    public void fullSnapshotsAndHeadingRefreshesUseRoadsideSmoothing() {
        NavigationSession session = session(NavigationRoutingMode.BROUTER);
        GeoJsonRoute offset = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0),
                new LatLon(0, 0.0001), new LatLon(0.000036, 0.0001),
                new LatLon(0.000036, 0.001), session.currentRequest.destination), Collections.emptyList(), 200, 444);
        applyRoute(session, offset);
        NavigationLocation shifted = fix(3_000);
        shifted.setLatitude(0.000018);
        shifted.setLongitude(0.0001);
        accept(session, shifted);
        NavState state = build(session, 180.0, 3_000);
        float heading = state.routeStatus.compassState.displayMode.headingDegrees;
        assertEquals(90f, heading, 10f);
        assertHeading(NavigationSessionResourceAdapter.withDisplayHeading(
                session, state, 220.0, 5f, 3_100), heading);
    }

    private static NavigationSession session(NavigationRoutingMode mode) {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(mode, "trekking", null, "Destination",
                new LatLon(0.003, 0.001), Collections.emptyList(),
                mode == NavigationRoutingMode.ROUND_TRIP ? 1_000 : 0));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000L));
        accept(session, fix(1_000L));
        return session;
    }

    private static void accept(NavigationSession session, NavigationLocation location) {
        assertFalse(session.components.locationState.onRawLocationChanged(
                location, location.getTime(), false).isDropped());
        assertFalse(session.components.locationState.isLikelyStationary());
    }

    private static void applyRoute(NavigationSession session, LatLon start, boolean beeline) {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(start, new LatLon(0, 0.001), new LatLon(0.003, 0.001)),
                beeline ? Collections.singletonList(new VoiceHint(0, 16, 0, 111, 0))
                        : Collections.emptyList(), 200, 444);
        applyRoute(session, route);
    }

    private static void applyRoute(NavigationSession session, GeoJsonRoute route) {
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(1, 1,
                session.currentRequest.routingMode, new LatLon(0, 0), Collections.emptyList(),
                session.currentRequest.destination, "trekking", null, Collections.emptyList(), 1_000);
        session.components.routeState.applyRouteResult(TestNavigationTextResources.metric(), request,
                route, session.getLastFilteredLocation(), 3f, false, 1_000L);
    }

    private static void evaluateRoute(NavigationSession session, long nowMs) {
        session.components.routeState.evaluateLocation(session.getLastFilteredLocation(),
                3f, false, 5f, 84.0, nowMs, 0L);
    }

    private static NavState build(NavigationSession session, double compassHeading, long nowMs) {
        return NavigationSessionResourceAdapter.buildState(session, TestNavigationTextResources.metric(),
                NavState.NO_DEADLINE, nowMs, null, compassHeading, 5f);
    }

    private static void assertHeading(NavState state, float expectedHeading) {
        assertEquals(expectedHeading, state.routeStatus.compassState.displayMode.headingDegrees, 0.1f);
    }

    private static NavigationLocation fix(long nowMs) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(nowMs, nowMs);
        location.setLatitude(0);
        location.setLongitude(0);
        location.setAccuracy(5f);
        location.setSpeed(3f);
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        location.setBearing(84f);
        location.setBearingAccuracyDegrees(12f);
        return location;
    }
}
