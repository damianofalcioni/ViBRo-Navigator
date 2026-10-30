package vibro.navigator.nav.guidance;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import static org.junit.Assert.*;

public class NavigationAcquisitionDiagnosticsTest {
    private final NavigationUpdateScheduler scheduler = new NavigationUpdateScheduler();
    private final GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, .01)),
            Collections.emptyList(), 1_000, 1_112);
    private final PolylineIndex index = new PolylineIndex(route.track);

    @Test
    public void qualityDeteriorationAndRecoveryExplainTheActualLimitingCap() {
        assertEquals(30_000, interval(1_000, 5));
        assertEquals("quality_margin", scheduler.intervalReason());
        assertEquals(3_000, interval(4_000, 130));
        assertEquals("quality_margin", scheduler.intervalReason());
        assertEquals(3_000, interval(7_000, 5));
        assertEquals("quality_recovery", scheduler.intervalReason());
        assertEquals(3_000, scheduler.applyPostManeuverIntervalRamp(60_000, true, 7_000));
        assertEquals("post_maneuver_ramp", scheduler.intervalReason());
    }

    @Test
    public void fastChecksAndMissingRouteAreDistinctAndResetClearsThePriorCause() {
        scheduler.suggestUpdateInterval(1_000, 2_000, route, index, 0, 0, 0, 1);
        assertEquals("fast_checks", scheduler.intervalReason());
        scheduler.suggestUpdateInterval(3_000, 0, null, null, 0, 0, 0, 1);
        assertEquals("route_unavailable", scheduler.intervalReason());
        scheduler.resetPostManeuverIntervalRamp();
        assertEquals("not_evaluated", scheduler.intervalReason());
    }

    private long interval(long now, float accuracy) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(now, now);
        scheduler.recordLocationQuality(location, accuracy);
        return scheduler.suggestUpdateInterval(now, 0, route, index, null, 300d, 0, 0, 1.5f);
    }

    @Test
    public void theNearManeuverGuardIsDistinguishedFromUnrestrictedRouteTiming() {
        interval(1_000, 5);
        assertEquals(20_000, scheduler.suggestUpdateInterval(1_000, 0, route, index, null, 300d, 0, 0, 2));
        assertEquals("maneuver_guard", scheduler.intervalReason());
        scheduler.resetPostManeuverIntervalRamp();
        assertEquals(60_000, scheduler.suggestUpdateInterval(2_000, 0, route, index, null, 600d, 0, 0, 2));
        assertEquals("route_eta", scheduler.intervalReason());
    }
}
