package vibro.navigator.nav.session;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationDepartureIntegrationTest {
    @Test
    public void metadataPoorWalkingLeavesFastModeButStopAndBackwardDepartureRestoreIt() {
        NavigationSession session = new NavigationSession();
        var text = TestNavigationTextResources.metric();
        session.loadRequest(new NavigationRequest("trekking", "Destination", new LatLon(0, 0.1), Collections.emptyList()));
        NavigationSessionResourceAdapter.start(session, text, 1_000);
        accept(session, 1_000, 0, 0, 19);
        var request = session.prepareRouteRequest(true, 1_000);
        NavigationSessionResourceAdapter.applyRouteResult(session, text, request,
                new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, 0.1)),
                        Collections.emptyList(), 8_000, 11_132), 1_000);
        NavigationLocationUpdateResult result = null;
        for (int i = 1; i <= 40; i++) {
            result = accept(session, 1_000 + i * 3_000L, i * 3.6, 1.2f, 19);
        }
        assertFalse(session.isLikelyStationaryForOrientation());
        assertTrue(result.getSuggestedUpdateIntervalMs() > 3_000);
        accept(session, 124_000, 144, 0, 5);
        result = accept(session, 127_000, 144, 0, 5);
        assertTrue(session.isLikelyStationaryForOrientation());
        assertEquals(3_000, result.getSuggestedUpdateIntervalMs());
        boolean detectedWrongDirection = false;
        for (int i = 1; i <= 10; i++) {
            result = accept(session, 127_000 + i * 3_000L, 144 - i * 3.6, 1.2f, 5);
            detectedWrongDirection |= result.shouldRecalculateRoute();
            assertTrue(result.getSuggestedUpdateIntervalMs() <= 3_000);
        }
        assertTrue(detectedWrongDirection);
    }

    private static NavigationLocationUpdateResult accept(NavigationSession session, long time,
            double distance, float speed, float accuracy) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLongitude(distance / 111_320);
        location.setAccuracy(accuracy);
        location.setSpeed(speed);
        return NavigationSessionResourceAdapter.onRawLocationChanged(session,
                TestNavigationTextResources.metric(), location, time, 3_000);
    }
}
