package vibro.navigator.nav.session;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.location.NavigationMotionTrigger;
import vibro.navigator.nav.location.NavigationStationarySleep;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationStationaryRestartIntegrationTest {
    @Test
    public void slowWalkingCannotSleepAndReverseRestartRetainsRapidDirectionChecks() {
        Fixture fixture = new Fixture();
        NavigationLocationUpdateResult result = null;
        for (int index = 1; index <= 40; index++) {
            result = fixture.accept(1_000L + index * 3_000L, index * 3.6d, 1.2f);
            assertFalse(fixture.readyToSleep);
        }
        assertTrue(result.getSuggestedUpdateIntervalMs() > 3_000L);
        fixture.confirmStop();
        fixture.sleep.beginSleep(151_000L);
        fixture.trigger.callback.run();
        assertFalse(fixture.sleep.isSleeping());
        boolean recalculated = false;
        for (int index = 1; index <= 10; index++) {
            result = fixture.accept(160_000L + index * 3_000L, 144d - index * 3.6d, 1.2f);
            assertFalse(fixture.readyToSleep);
            if (!result.shouldRecalculateRoute()) {
                assertTrue(result.getSuggestedUpdateIntervalMs() > 0L);
                assertTrue(result.getSuggestedUpdateIntervalMs() <= 3_000L);
            }
            recalculated |= result.shouldRecalculateRoute();
        }
        assertTrue(recalculated);
    }

    @Test
    public void fallbackDetectsRelocationDespiteMissingMotionTrigger() {
        Fixture fixture = new Fixture();
        fixture.confirmStop();
        fixture.sleep.beginSleep(151_000L);
        fixture.sleep.beginCheck();
        fixture.accept(181_000L, 154d, 0f);
        assertFalse(fixture.readyToSleep);
        fixture.accept(184_000L, 154d, 0f);
        fixture.accept(187_000L, 154d, 0f);
        assertFalse(fixture.readyToSleep);
    }

    private static final class Fixture {
        private final NavigationSession session = new NavigationSession();
        private final Trigger trigger = new Trigger();
        private final NavigationStationarySleep sleep = new NavigationStationarySleep(trigger,
                () -> NavigationSessionResourceAdapter.restartDepartureMonitoring(session));
        private boolean readyToSleep;

        Fixture() {
            var text = TestNavigationTextResources.metric();
            session.loadRequest(new NavigationRequest("trekking", "Destination",
                    new LatLon(0d, 0.1d), Collections.emptyList()));
            NavigationSessionResourceAdapter.start(session, text, 1_000L);
            accept(1_000L, 0d, 0f);
            var request = session.prepareRouteRequest(true, 1_000L);
            NavigationSessionResourceAdapter.applyRouteResult(session, text, request,
                    new GeoJsonRoute(Arrays.asList(new LatLon(0d, 0d), new LatLon(0d, 0.1d)),
                            Collections.emptyList(), 8_000L, 11_132d), 1_000L);
        }

        NavigationLocationUpdateResult accept(long time, double distance, float speed) {
            NavigationLocation location = new NavigationLocation("gps");
            location.setTime(time, time);
            location.setLongitude(distance / 111_320d);
            location.setAccuracy(5f);
            location.setSpeed(speed);
            NavigationLocationUpdateResult result = NavigationSessionResourceAdapter.onRawLocationChanged(
                    session, TestNavigationTextResources.metric(), location, time, 3_000L);
            assertFalse(result.isDropped());
            readyToSleep = sleep.observe(location, session.getLastFilteredLocation(),
                    session.isLikelyStationaryForOrientation(), time);
            return result;
        }

        void confirmStop() {
            for (long time = 124_000L; time <= 151_000L; time += 3_000L) {
                accept(time, 144d, 0f);
            }
            assertTrue(session.isLikelyStationaryForOrientation());
            assertTrue(readyToSleep);
        }
    }

    private static final class Trigger implements NavigationMotionTrigger {
        private Runnable callback;
        @Override public boolean arm(Runnable onMotion) { callback = onMotion; return true; }
        @Override public void cancel() { callback = null; }
    }
}
