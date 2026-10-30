package vibro.navigator.nav.session;

import java.util.Collections;
import java.util.ArrayList;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationMotionState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.time.NavigationDisplayTime;
import static org.junit.Assert.*;

public class NavigationDisplayFreshnessTest {
    private static final LatLon TARGET = new LatLon(0, 1_000.0 / 111_320);

    @Test
    public void expiryUsesElapsedTimeAndPreservesUnknownInsteadOfManufacturingAStop() {
        assertEquals(2, snapshot(11_000).speedMps, 0);
        NavigationDisplaySnapshot expired = snapshot(11_001);
        assertEquals(0, expired.speedMps, 0);
        assertTrue(Float.isNaN(expired.displaySpeedMps));
        assertEquals(NavigationMotionState.UNKNOWN, expired.motionState);
        assertFalse(expired.likelyStationary);
        assertFalse(expired.hasFreshLocation());
    }

    @Test
    public void futureObservationCannotSupplyDisplaySpeedOrAnApproachEta() {
        NavigationDisplaySnapshot future = snapshot(999);
        assertEquals(0, future.speedMps, 0);
        assertTrue(NavigationRouteStartApproachText.buildLine(future, TARGET).contains("--"));
    }

    @Test
    public void beelineTextCannotReplaceExpiredSpeedWithDefaultWalkingTime() {
        assertFalse(NavigationRouteStartApproachText.buildLine(snapshot(1_000), TARGET).contains("--"));
        assertTrue(NavigationRouteStartApproachText.buildLine(snapshot(120_000), TARGET).contains("--"));
    }

    @Test
    public void aDelayedStraightLineNotificationAlsoSuppressesItsLiveEta() {
        StraightLineBeelineGuidance guidance = new StraightLineBeelineGuidance();
        guidance.evaluate(new ArrayList<>(), TARGET, fix(1_000, 0), 2, false, 5, null, 1_000, 0);
        var result = guidance.evaluate(new ArrayList<>(), TARGET,
                fix(4_000, -10), 2, false, 5, null, 18_000, 0);
        assertEquals(1, result.turnEvents.size());
        assertTrue(Double.isNaN(result.turnEvents.get(0).timeSeconds));
        assertEquals(3_000, result.getSuggestedUpdateIntervalMs());
    }

    @Test
    public void straightLineDisplayAndDetailsExpireAndRecoverWithoutErasingTheLocation() {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(NavigationRoutingMode.STRAIGHT_LINE, null,
                "Target", TARGET, Collections.emptyList()));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        session.setActiveLocationProviders("gps");
        for (int i = 0; i < 4; i++) {
            accept(session, fix(1_000 + i * 3_000, i * 6));
        }
        assertFalse(destinationLine(session, 10_000).contains("--"));
        assertTrue(destinationLine(session, 120_000).contains("--"));
        assertTrue(NavigationSessionResourceAdapter.buildCurrentDirectionDetails(
                session, TestNavigationTextResources.metric(), 120_000).get(0).contains("--"));
        assertEquals(10_000, session.getLastFilteredLocation().getElapsedRealtimeOrTimeMs());
        accept(session, fix(123_000, 24));
        accept(session, fix(126_000, 30));
        assertFalse(destinationLine(session, 126_000).contains("--"));
    }

    private static NavigationDisplaySnapshot snapshot(long elapsedMs) {
        return NavigationDisplaySnapshot.builder(TestNavigationTextResources.metric())
                .location(fix(1_000, 0), 2, 2, false, 5)
                .timing(-1, new NavigationDisplayTime(1_800_000_000_000L, elapsedMs)).build();
    }

    private static String destinationLine(NavigationSession session, long elapsedMs) {
        return NavigationSessionResourceAdapter.buildState(session, TestNavigationTextResources.metric(),
                -1, elapsedMs, null, null, null).routeStatus.progress.destinationLine;
    }

    private static void accept(NavigationSession session, NavigationLocation location) {
        assertFalse(NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(),
                location, location.getElapsedRealtimeOrTimeMs(), 3_000).isDropped());
    }

    static NavigationLocation fix(long time, double east) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLongitude(east / 111_320);
        location.setAccuracy(5);
        location.setSpeed(2);
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        return location;
    }
}
