package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassBlockedArea;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;

public class NavigationSessionBlockedCompassDisplayTest extends NavigationSessionRouteStateTestSupport {
    @Test
    public void buildState_exposesBlockedPointsAsCompassAreasWithPerPointRadius() {
        NavigationTextResources textResources = TestNavigationTextResources.metric();
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        );
        NavigationLocation currentLocation = location(0.0, 0.0, 1_000L);
        state.applyRouteResult(
                textResources,
                snapshot(request),
                routeWithoutHints(),
                currentLocation,
                0f,
                true,
                500L
        );

        List<NogoPoint> blockedPoints = state.addBlockedPointsAhead(currentLocation, 10_000L);
        NavState navState = state.buildState(
                textResources,
                currentLocation,
                0f,
                true,
                5f,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                10_000L,
                false,
                null,
                null
        );

        assertFalse(blockedPoints.isEmpty());
        assertNotNull(navState.routeStatus.compassState);
        assertEquals(blockedPoints.size(), navState.routeStatus.compassState.blockedAreas.size());
        assertBlockedAreaRadius(blockedPoints.get(0), navState.routeStatus.compassState.blockedAreas.get(0));
    }

    private static void assertBlockedAreaRadius(NogoPoint point, CompassBlockedArea area) {
        assertEquals(point.radiusMeters, area.radiusMeters, 0.01);
    }
}
