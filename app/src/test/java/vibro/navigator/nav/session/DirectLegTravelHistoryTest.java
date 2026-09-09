package vibro.navigator.nav.session;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import static org.junit.Assert.*;

public class DirectLegTravelHistoryTest {
    @Test
    public void unfinishedNativeBeelineKeepsFixesThenPlansFromItsTarget() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, 0.001),
                new LatLon(0, 0.003)), Collections.singletonList(new VoiceHint(0, 16, 0, 111, 0)), 60, 333);
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(1, 1, new LatLon(0, 0),
                Collections.emptyList(), new LatLon(0, 0.003), "trekking", null, Collections.emptyList());
        state.applyRouteResult(TestNavigationTextResources.metric(), request, route, fix(0, 0, 1_000), 3, 500);
        accept(state, fix(0.0003, 0.0003, 4_000));
        accept(state, fix(0.0004, 0.0006, 7_000));
        NavigationRouteHistory history = state.historyForExport();
        List<LatLon> connector = history.recalculationBridgeSegmentsSnapshot().get(0);
        assertEquals(3, connector.size());
        assertEquals(0.0006, connector.get(2).lon, 0.0000001);
        assertEquals(0.001, history.remainingRoute().track.get(0).lon, 0.0000001);
        assertTrue(history.entryMeters() > 100);
        accept(state, fix(0, 0.001, 10_000));
        assertEquals(1, history.recalculationBridgeSegmentsSnapshot().size());
        assertEquals(0.001, history.remainingRoute().track.get(0).lon, 0.0000001);
    }

    private static void accept(NavigationSessionRouteState state, NavigationLocation fix) {
        NavigationRouteEvaluation evaluation = state.evaluateLocation(fix, 3, 10, 90.0, fix.getTime(), 0);
        assertFalse(evaluation.shouldRecalculateRoute());
        state.recordRecalculationFixPath(fix, evaluation, false);
    }

    private static NavigationLocation fix(double lat, double lon, long time) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(time);
        location.setAccuracy(10);
        return location;
    }
}
