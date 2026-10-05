package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

public class NavigationHeadingAcquisitionIntegrationTest {
    @Test
    public void routedSixtySecondPollingPersistsThroughRepeatedPoorHeadings() {
        Fixture fixture = new Fixture(NavigationRoutingMode.BROUTER);
        fixture.warmUp();
        assertEquals(60_000L, fixture.lastResult.getSuggestedUpdateIntervalMs());
        for (int i = 0; i < 3; i++) {
            fixture.accept(60_000L, 40f, 100f);
            assertFalse(fixture.lastResult.isDropped());
            assertEquals(60_000L, fixture.lastResult.getSuggestedUpdateIntervalMs());
        }
        fixture.accept(60_000L, 5f, 5f);
        assertEquals(60_000L, fixture.lastResult.getSuggestedUpdateIntervalMs());
    }

    @Test
    public void routedSixtySecondPollingPersistsWithoutBearingAfterLongGaps() {
        Fixture fixture = new Fixture(NavigationRoutingMode.BROUTER);
        fixture.warmUp();
        for (int i = 0; i < 3; i++) {
            fixture.accept(60_000L, null, 5f);
            assertFalse(fixture.lastResult.isDropped());
            assertEquals(60_000L, fixture.lastResult.getSuggestedUpdateIntervalMs());
        }
    }

    @Test
    public void straightLinePollingPersistsThroughRepeatedPoorHeadings() {
        Fixture fixture = new Fixture(NavigationRoutingMode.STRAIGHT_LINE);
        fixture.warmUp();
        assertEquals(10_000L, fixture.lastResult.getSuggestedUpdateIntervalMs());
        for (int i = 0; i < 3; i++) {
            fixture.accept(10_000L, 40f, 100f);
            assertEquals(10_000L, fixture.lastResult.getSuggestedUpdateIntervalMs());
        }
        fixture.accept(10_000L, 5f, 5f);
        assertEquals(10_000L, fixture.lastResult.getSuggestedUpdateIntervalMs());
    }

    private static final class Fixture {
        private final NavigationTextResources text = TestNavigationTextResources.metric();
        private final NavigationSession session = new NavigationSession();
        private long nowMs = 1_000L;
        private double longitude;
        private NavigationLocationUpdateResult lastResult;

        private Fixture(NavigationRoutingMode mode) {
            session.loadRequest(new NavigationRequest(mode, "trekking", "Destination",
                    new LatLon(0.0, 0.1), Collections.emptyList()));
            assertTrue(NavigationSessionResourceAdapter.start(session, text, nowMs));
            accept(0L, 5f, 5f);
            if (mode == NavigationRoutingMode.BROUTER) {
                NavigationRouteRequestSnapshot request = session.prepareRouteRequest(true, nowMs);
                assertNotNull(request);
                NavigationSessionResourceAdapter.applyRouteResult(session, text, request,
                        new GeoJsonRoute(Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.1)),
                                Collections.emptyList(), 6_000.0, 11_132.0), nowMs);
                assertTrue(session.hasActiveRoute());
            }
        }

        private void warmUp() {
            // Advance beyond both startup fast polling and the post-maneuver interval ramp.
            for (int i = 0; i < 40; i++) {
                accept(3_000L, 5f, 5f);
            }
        }

        private void accept(long elapsedMs, Float bearingAccuracy, float positionAccuracy) {
            nowMs += elapsedMs;
            longitude += 0.00004;
            NavigationLocation location = new NavigationLocation("gps");
            location.setTime(nowMs, nowMs);
            location.setLatitude(0.0);
            location.setLongitude(longitude);
            location.setSpeed(2f);
            if (bearingAccuracy != null) {
                location.setBearing(90f);
                location.setBearingAccuracyDegrees(bearingAccuracy);
            }
            location.setAccuracy(positionAccuracy);
            lastResult = NavigationSessionResourceAdapter.onRawLocationChanged(
                    session, text, location, nowMs, Math.max(3_000L, elapsedMs));
        }
    }
}
