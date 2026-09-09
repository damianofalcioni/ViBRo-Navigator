package vibro.navigator.nav.session;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import static org.junit.Assert.*;

public class NavigationBeelineRecoveryRequestsTest {
    private static final LatLon DESTINATION = new LatLon(0.001, 0.003);
    private static final LatLon STOP = new LatLon(0.001, 0.002);

    @Test
    public void pendingRequestKeepsCompassAndFailedRequestKeepsRouteAndHistory() {
        NavigationSession session = session(Collections.emptyList());
        GeoJsonRoute original = session.components.routeState.currentRoute();
        NavigationRouteRequestSnapshot request = prepare(session);
        assertTrue(session.isRouteCalculationInProgress());
        NavState display = NavigationSessionResourceAdapter.buildState(session, TestNavigationTextResources.metric(),
                NavState.NO_DEADLINE, 4_000, null, null, null);
        assertNotNull(display.routeStatus.compassState);
        assertFalse(session.components.routeRequestManager.isVisibleRouteCalculationInProgress());
        assertTrue(session.speculativeRoutes().ignoreUnconfirmedRouteFailure(request, new Exception("No route")));
        assertSame(original, session.components.routeState.currentRoute());
        assertNull(session.components.routeRequestManager.getLastRouteFailure());
        assertFalse(session.isRouteCalculationInProgress());
        assertTrue(session.components.routeState.historyForExport().hasPendingRerouteFixPath());
    }

    @Test
    public void usableResultReplacesBeelineAndPreservesAllRequestedStops() {
        NavigationSession session = session(Collections.singletonList(STOP));
        NavigationRouteRequestSnapshot request = prepare(session);
        assertEquals("trekking", request.profile);
        assertEquals(1, request.intermediates.size());
        GeoJsonRoute replacement = route(Arrays.asList(new LatLon(0, 0), STOP, DESTINATION));
        assertFalse(session.speculativeRoutes().handleUnconfirmedRouteResult(request, replacement, 3_000, 4_000));
        NavigationSessionResourceAdapter.applyRouteResult(session, TestNavigationTextResources.metric(),
                request, replacement, 3_000, 4_000);
        assertSame(replacement, session.components.routeState.currentRoute());
        assertEquals(1, session.components.routeState.remainingIntermediateStops(Collections.emptyList()).size());
        assertNull(session.components.routeRequestManager.getLastRouteFailure());
    }

    @Test
    public void unsuitableCandidateDoesNotEnterExportOrChangeCurrentRoute() {
        NavigationSession session = session(Collections.emptyList());
        GeoJsonRoute original = session.components.routeState.currentRoute();
        GeoJsonRoute candidate = route(Arrays.asList(new LatLon(0.0007, 0.0007), DESTINATION));
        assertTrue(session.speculativeRoutes().handleUnconfirmedRouteResult(prepare(session), candidate, 3_000, 4_000));
        assertSame(original, session.components.routeState.currentRoute());
        String gpx = session.buildCurrentRouteGpx(TestNavigationTextResources.metric());
        assertFalse(gpx.contains("lat=\"0.000700\" lon=\"0.000700\""));
    }

    @Test
    public void pauseResumeAndTargetChangesMakeCandidateObsolete() {
        NavigationSession session = session(Collections.emptyList());
        NavigationRouteRequestSnapshot request = prepare(session);
        assertTrue(session.pause());
        assertTrue(session.resume());
        observe(session, 4_000);
        assertTrue(session.speculativeRoutes().handleUnconfirmedRouteResult(request, usableRoute(), 3_000, 4_000));
        NavigationRouteRequestSnapshot next = prepare(session, 4_000);
        session.components.routeState.beelineRecovery().setTarget(new LatLon(0.002, 0));
        assertTrue(session.speculativeRoutes().handleUnconfirmedRouteResult(next, usableRoute(), 3_000, 4_000));
    }

    @Test
    public void staleFixAndChangedPlanCannotAcceptCandidate() {
        NavigationSession session = session(Collections.emptyList());
        assertTrue(session.speculativeRoutes().handleUnconfirmedRouteResult(prepare(session), usableRoute(), 3_000, 20_000));
        observe(session, 21_000);
        NavigationRouteRequestSnapshot request = session.speculativeRoutes().prepareRequest(true, 21_000,
                NavigationRouteRecalculationReason.BEELINE_RECOVERY);
        session.loadRequest(new NavigationRequest("trekking", "New destination", new LatLon(1, 1), Collections.emptyList()));
        assertTrue(session.speculativeRoutes().handleUnconfirmedRouteResult(request, usableRoute(), 21_000, 22_000));
    }

    @Test
    public void explicitQueuedRequestSupersedesTheCandidateAndOrdinaryConfirmationCannotPromoteIt() {
        NavigationSession session = session(Collections.emptyList());
        NavigationRouteRequestSnapshot recovery = prepare(session);
        assertNull(session.prepareRouteRequest(true, 5_000));
        assertTrue(session.speculativeRoutes().handleUnconfirmedRouteResult(recovery, usableRoute(), 3_000, 5_000));
        assertNotNull(session.consumePendingRouteRecalculation());
        observe(session, 6_000);
        assertNotNull(prepare(session, 6_000));
        assertEquals(NavigationSessionSpeculativeRoutes.Confirmation.NONE,
                session.speculativeRoutes().confirmRecalculation());
        assertFalse(session.isRouteCalculationInProgress());
    }

    private static NavigationSession session(List<LatLon> stops) {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest("trekking", "Destination", DESTINATION, stops));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setLatitude(0);
        fix.setLongitude(0);
        fix.setTime(1_000, 1_000);
        fix.setAccuracy(20);
        fix.setSpeed(1.4f);
        session.components.locationState.onRawLocationChanged(fix, 1_000);
        NavigationRouteRequestSnapshot request = session.prepareRouteRequest(true, 1_000);
        assertNotNull(request);
        NavigationSessionResourceAdapter.applyRouteResult(session, TestNavigationTextResources.metric(), request,
                route(Arrays.asList(new LatLon(0.001, 0), STOP, DESTINATION)), 1_000, 2_000);
        observe(session, 3_000);
        return session;
    }

    private static void observe(NavigationSession session, long nowMs) {
        NavigationLocation location = session.getLastFilteredLocation();
        NavigationRouteEvaluation evaluation = session.components.routeState.evaluateLocation(location, 1.4f,
                false, 20, null, nowMs, 0, false);
        session.components.routeState.recordRecalculationFixPath(location, evaluation, session.isRouteCalculationInProgress());
    }

    private static NavigationRouteRequestSnapshot prepare(NavigationSession session) {
        return prepare(session, 3_000);
    }

    private static NavigationRouteRequestSnapshot prepare(NavigationSession session, long nowMs) {
        NavigationRouteRequestSnapshot snapshot = session.speculativeRoutes().prepareRequest(true, nowMs,
                NavigationRouteRecalculationReason.BEELINE_RECOVERY);
        assertNotNull(snapshot);
        return snapshot;
    }

    private static GeoJsonRoute usableRoute() {
        return route(Arrays.asList(new LatLon(0, 0), DESTINATION));
    }

    private static GeoJsonRoute route(List<LatLon> points) {
        return new GeoJsonRoute(points, Collections.emptyList(), 300, 400);
    }
}
