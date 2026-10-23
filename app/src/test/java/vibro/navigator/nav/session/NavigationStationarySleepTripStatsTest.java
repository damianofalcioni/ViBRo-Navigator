package vibro.navigator.nav.session;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationMotionTrigger;
import vibro.navigator.nav.location.NavigationStationarySleep;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationStationarySleepTripStatsTest {
    @Test
    public void repeatedFallbackChecksPreserveSleepTimeWhileResettingMotionEvidence() {
        Fixture fixture = new Fixture();
        int revision = fixture.session.components.locationState.motionRevision();
        fixture.sleep.beginSleep(22_000);
        fixture.wake(52_000);
        var stale = NavigationSessionResourceAdapter.onRawLocationChanged(fixture.session,
                TestNavigationTextResources.metric(), stop(22_000), 52_000, 1_000);
        assertTrue(stale.isDropped());
        fixture.accept(52_000);
        assertEquals(51_000, fixture.stationaryMs());
        assertTrue(fixture.session.components.locationState.motionRevision() > revision);
        fixture.accept(54_000);
        assertTrue(fixture.accept(56_000));
        fixture.sleep.beginSleep(56_000);
        fixture.wake(86_000);
        fixture.accept(86_000);
        assertEquals(85_000, fixture.stationaryMs());
    }

    @Test
    public void ordinaryStallAfterSleepWakeDoesNotReceiveUnobservedTimeCredit() {
        Fixture fixture = new Fixture();
        fixture.sleep.beginSleep(22_000);
        fixture.wake(47_000);
        fixture.accept(70_000);
        assertEquals(46_000, fixture.stationaryMs());
        assertEquals(0d, fixture.session.components.tripStatsTracker.snapshot().travelledDistanceMeters, 0d);
    }

    @Test
    public void newSessionDiscardsAnUnconsumedSleepBoundary() {
        Fixture fixture = new Fixture();
        fixture.sleep.beginSleep(22_000);
        fixture.wake(52_000);
        NavigationSessionResourceAdapter.start(fixture.session, TestNavigationTextResources.metric(), 100_000);
        fixture.accept(100_000);
        fixture.accept(103_000);
        assertEquals(3_000, fixture.stationaryMs());
    }

    private static NavigationLocation stop(long nowMs) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(nowMs, nowMs);
        location.setAccuracy(5);
        location.setSpeed(0);
        return location;
    }

    private static final class Fixture {
        private final NavigationSession session = new NavigationSession();
        private final NavigationStationarySleep sleep = new NavigationStationarySleep(new Trigger(), () -> { });

        Fixture() {
            var text = TestNavigationTextResources.metric();
            session.loadRequest(new NavigationRequest("trekking", "Destination", new LatLon(0, 0.01), Collections.emptyList()));
            NavigationSessionResourceAdapter.start(session, text, 1_000);
            accept(1_000);
            var snapshot = session.prepareRouteRequest(true, 1_000);
            NavigationSessionResourceAdapter.applyRouteResult(session, text, snapshot,
                    new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, 0.01)),
                            Collections.emptyList(), 800, 1113), 1_000);
            boolean ready = false;
            for (long nowMs = 4_000; nowMs <= 22_000; nowMs += 3_000) {
                ready = accept(nowMs);
            }
            assertTrue(ready);
        }

        boolean accept(long nowMs) {
            NavigationLocation raw = stop(nowMs);
            var result = NavigationSessionResourceAdapter.onRawLocationChanged(session,
                    TestNavigationTextResources.metric(), raw, nowMs, 1_000);
            assertFalse(result.isDropped());
            return sleep.observe(raw, session.getLastFilteredLocation(), session.isLikelyStationaryForOrientation(), nowMs);
        }

        void wake(long nowMs) {
            sleep.beginCheck();
            NavigationSessionResourceAdapter.resumeAfterStationarySleep(session, nowMs);
        }

        long stationaryMs() {
            return session.components.tripStatsTracker.snapshot().stationaryDurationMs;
        }
    }

    private static final class Trigger implements NavigationMotionTrigger {
        @Override public boolean arm(Runnable onMotion) { return true; }
        @Override public void cancel() { }
    }
}
