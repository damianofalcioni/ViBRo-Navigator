package vibro.navigator.nav.session;

import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NavigationSpeculativeRouteReuseTest {
    @Test
    public void expiredCandidateCannotBePromotedWhenConfirmationArrives() {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest("trekking", "Destination", new LatLon(0, 0.01), Collections.emptyList()));
        NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 10_000);
        NavigationLocation fix = location(0);
        fix.setTime(10_000, 10_000);
        fix.setAccuracy(5);
        session.components.locationState.onRawLocationChanged(fix, 10_000);
        var snapshot = session.speculativeRoutes().prepareRequest(false, 10_000,
                NavigationRouteRecalculationReason.ROUTE_DEVIATION);
        assertNotNull(snapshot);
        session.components.speculativeRouteState.deferResult(snapshot,
                NavigationSessionRouteStateTestSupport.routeWithoutHints(), 10_000);
        assertEquals(NavigationSessionSpeculativeRoutes.Confirmation.NONE,
                session.speculativeRoutes().confirmRecalculation(26_000));
        assertFalse(session.isRouteCalculationInProgress());
    }
    @Test
    public void deferredCandidateSurvivesNearbyFixesButExpiresOrMovesOutOfRange() {
        NavigationSpeculativeRouteState state = new NavigationSpeculativeRouteState();
        NavigationRouteRequestSnapshot snapshot = new NavigationRouteRequestSnapshot(1, 1,
                NavigationRoutingMode.BROUTER, new LatLon(0, 0), Collections.emptyList(),
                new LatLon(0, 0.01), "trekking", false, null, Collections.emptyList(), 0, 0, true);
        state.onRouteRequestPrepared(snapshot, 10_000);
        GeoJsonRoute route = NavigationSessionRouteStateTestSupport.routeWithoutHints();
        assertTrue(state.deferResult(snapshot, route, 10_000));
        assertTrue(state.canReuse(location(0.0001), 12_000));
        assertTrue(state.canReuse(location(0.0002), 15_000));
        assertFalse(state.canReuse(location(0.001), 15_000));
        assertFalse(state.canReuse(location(0), 26_000));
        state.cancelUnconfirmed();
        assertFalse(state.canReuse(location(0), 15_000));
    }

    private static NavigationLocation location(double latitude) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(latitude);
        return location;
    }
}
