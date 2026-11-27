package vibro.navigator.nav.session;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.simulation.NavigationSimulationConfig;
import vibro.navigator.nav.simulation.SimulationTestDriver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NavigationSimulationArrivalTest {
    private static final LatLon ORIGIN = new LatLon(0, 0);
    private static final LatLon STOP = new LatLon(0, 0.0005);
    private static final LatLon DESTINATION = new LatLon(0, 0.001);

    @Test
    public void requiredStopBetweenVerticesWaitsForNormalArrivalConfirmation() {
        NavigationSession session = session(NavigationRoutingMode.BROUTER);
        SimulationTestDriver driver = driver(session);
        NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(),
                driver.source.currentFix(), driver.nowMs());
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, driver.nowMs());
        assertNotNull(snapshot);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(ORIGIN, DESTINATION), Collections.emptyList(), 20, 111);
        NavigationSessionResourceAdapter.applyRouteResult(session, TestNavigationTextResources.metric(), snapshot, route, driver.nowMs());
        for (int i = 0; i < 80; i++) { driver.advance(1000); }
        assertTrue(session.components.routeState.remainingIntermediateStops(Collections.singletonList(STOP)).isEmpty());
        assertTrue(session.components.routeState.isDestinationReached());
    }

    @Test
    public void roundTripMustDepartBeforeConfirmingTheReturnToItsStart() {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(NavigationRoutingMode.ROUND_TRIP, "car", null, null, null,
                Collections.emptyList(), 1000));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 100000));
        SimulationTestDriver driver = driver(session);
        NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(),
                driver.source.currentFix(), driver.nowMs());
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, driver.nowMs());
        assertNotNull(snapshot);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(ORIGIN, STOP, new LatLon(0.0005, 0.0005), ORIGIN),
                Collections.emptyList(), 20, 190);
        NavigationSessionResourceAdapter.applyRouteResult(session, TestNavigationTextResources.metric(), snapshot, route, driver.nowMs());
        for (int i = 0; i < 80; i++) { driver.advance(1000); }
        assertTrue(session.components.routeState.isDestinationReached());
        assertEquals(0, session.getLastFilteredLocation().getLongitude(), 0.00001);
    }

    @Test
    public void straightLineVisitsStopAndConfirmsDestinationThroughNormalFilter() {
        NavigationSession session = session(NavigationRoutingMode.STRAIGHT_LINE);
        SimulationTestDriver driver = driver(session);
        for (int i = 0; i < 80; i++) { driver.advance(1000); }
        assertEquals(1, session.components.straightLineState.nextStopIndexForDisplay());
        assertTrue(session.components.straightLineState.isDestinationReachedForDisplay());
        assertEquals(DESTINATION.lon, session.getLastFilteredLocation().getLongitude(), 0.00001);
    }

    @Test
    public void routedNavigationWaitsAtIntermediateAndDestinationForIndependentArrivalEvidence() {
        NavigationSession session = session(NavigationRoutingMode.BROUTER);
        SimulationTestDriver driver = driver(session);
        NavigationLocation seed = driver.source.currentFix();
        assertNotNull(seed);
        NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(), seed, driver.nowMs());
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, driver.nowMs());
        assertNotNull(snapshot);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(ORIGIN, STOP, DESTINATION), Collections.emptyList(), 20, 111);
        NavigationSessionResourceAdapter.applyRouteResult(session, TestNavigationTextResources.metric(), snapshot, route, driver.nowMs());
        for (int i = 0; i < 80; i++) { driver.advance(1000); }
        assertTrue(session.components.routeState.remainingIntermediateStops(Collections.singletonList(STOP)).isEmpty());
        assertTrue(session.components.routeState.isDestinationReached());
    }

    @Test
    public void simulatorFollowsReturnLegWhenLaterStopWasPassedOnOutboundLeg() {
        LatLon laterStop = new LatLon(0.002, 0);
        LatLon firstStop = new LatLon(0.004, 0);
        LatLon eastCorner = new LatLon(firstStop.lat, 0.004);
        LatLon southCorner = new LatLon(laterStop.lat, eastCorner.lon);
        LatLon end = new LatLon(laterStop.lat, -0.002);
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest("car", "End", end, Arrays.asList(firstStop, laterStop)));
        var resources = TestNavigationTextResources.metric();
        assertTrue(NavigationSessionResourceAdapter.start(session, resources, 100_000));
        SimulationTestDriver driver = driver(session);
        NavigationSessionResourceAdapter.onRawLocationChanged(session, resources,
                driver.source.currentFix(), driver.nowMs());
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, driver.nowMs());
        assertNotNull(snapshot);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(ORIGIN, laterStop, firstStop,
                eastCorner, southCorner, laterStop, end), Collections.emptyList(), 180, 1_800);
        NavigationSessionResourceAdapter.applyRouteResult(session, resources, snapshot, route, driver.nowMs());

        double firstBarrier = NavigationSessionSimulation.path(session).barrier.alongTrackMeters;
        for (int tick = 0; tick < 100 && NavigationSessionSimulation.path(session).barrier.position == firstStop; tick++) {
            driver.advance(1_000);
        }
        var nextBarrier = NavigationSessionSimulation.path(session).barrier;
        assertEquals(laterStop, nextBarrier.position);
        assertTrue(nextBarrier.alongTrackMeters > firstBarrier);
        driver.advance(1_000);
        NavigationLocation nextFix = driver.source.currentFix();
        assertEquals(firstStop.lat, nextFix.getLatitude(), 0.000001);
        assertTrue(nextFix.getLongitude() > firstStop.lon);
        for (int tick = 0; tick < 4; tick++) {
            driver.advance(1_000);
        }
        var nextStop = NavigationSessionResourceAdapter.buildState(session, resources, NavState.NO_DEADLINE, driver.nowMs(),
                null, null, null).routeStatus.progress.nextStop;
        assertNotNull(nextStop);
        assertEquals(resources.getString(R.string.format_stop_label, 2), nextStop.label());
        assertTrue(nextStop.distanceMeters() > 500);
    }

    private static NavigationSession session(NavigationRoutingMode mode) {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(mode, "trekking", "End", DESTINATION, Collections.singletonList(STOP)));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 100000));
        return session;
    }

    @Test
    public void simulationContinuesAlongTheRoadAfterReturningFromAnOffRoadStop() {
        LatLon junction = new LatLon(0, 0.001);
        LatLon stop = new LatLon(0.0008, junction.lon);
        LatLon end = new LatLon(0, 0.003);
        var session = new NavigationSession();
        session.loadRequest(new NavigationRequest("trekking", "End", end, List.of(stop)));
        var resources = TestNavigationTextResources.metric();
        assertTrue(NavigationSessionResourceAdapter.start(session, resources, 100_000));
        var driver = driver(session, NavigationSimulationConfig.DEFAULT_SPEED_MPS);
        NavigationSessionResourceAdapter.onRawLocationChanged(session, resources, driver.source.currentFix(),
                driver.nowMs());
        var request = session.prepareRouteRequest(true, driver.nowMs());
        assertNotNull(request);
        var route = new GeoJsonRoute(List.of(ORIGIN, junction, stop, junction, end), List.of(
                new VoiceHint(1, RouteStartApproach.BEELINE_COMMAND, 0, 89, 0),
                new VoiceHint(2, RouteStartApproach.BEELINE_COMMAND, 0, 89, 0)), 200, 510);
        NavigationSessionResourceAdapter.applyRouteResult(session, resources, request, route, driver.nowMs());

        boolean returned = false;
        for (int tick = 0; tick < 80 && !NavigationSessionSimulation.path(session).reached; tick++) {
            driver.advance(1_000);
            var path = NavigationSessionSimulation.path(session);
            if (path.minimumRouteSegmentIndex >= 3) {
                returned = true;
                assertTrue("The simulator stays near the road after rejoining it",
                        driver.source.currentFix().getLatitude() < 0.0001);
            }
        }
        assertTrue(returned);
        assertTrue(NavigationSessionSimulation.path(session).reached);
    }

    private static SimulationTestDriver driver(NavigationSession session) {
        return driver(session, 10);
    }

    private static SimulationTestDriver driver(NavigationSession session, float speedMps) {
        SimulationTestDriver driver = new SimulationTestDriver(() -> NavigationSessionSimulation.path(session),
                fix -> NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(),
                        fix, fix.getElapsedRealtimeMs()));
        driver.source.configure(true, new NavigationSimulationConfig(speedMps, ORIGIN));
        driver.source.start();
        return driver;
    }
}
