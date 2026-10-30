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
import vibro.navigator.nav.route.VoiceHint;
import static org.junit.Assert.*;

public class NavigationEtaReliabilityIntegrationTest {
    @Test
    public void unknownCorrectionsCannotSupplyLiveSpeedForTurnAlerts() {
        NavigationSession session = start();
        accept(session, fix(4_000, 0, 0), 0);
        for (int i = 2; i <= 4; i++) {
            long time = 1_000 + i * 3_000L;
            NavigationLocationUpdateResult result = accept(session, fix(time, i % 2 == 0 ? 12 : 0, 0), 0);
            assertFalse(session.components.locationState.isMotionKnown(time));
            assertTrue("Unknown corrections must retain route-model timing", result.turnEvents.isEmpty());
        }
    }

    @Test
    public void constantDeliveryDelayCannotChangeLiveEtaOrAlertTiming() {
        compareDeliveries(new long[] {1_000}, 3_000);
    }

    @Test
    public void changingDeliveryDelayCannotChangeLiveEtaOrAlertTiming() {
        compareDeliveries(new long[] {500, 1_500, 0, 1_000}, 3_000);
    }

    @Test
    public void sparseDeliveryDelayCannotChangeLiveEtaOrAlertTiming() {
        compareDeliveries(new long[] {1_000, 2_000, 0}, 12_000);
    }

    private static void compareDeliveries(long[] delays, long intervalMs) {
        NavigationSession immediate = start();
        NavigationSession delayed = start();
        for (int i = 1; i <= 12; i++) {
            long time = 1_000 + i * intervalMs;
            NavigationLocation location = fix(time, i * intervalMs / 1_000.0, 1);
            NavigationLocationUpdateResult expected = accept(immediate, location, 0);
            long delay = delays[(i - 1) % delays.length];
            NavigationLocationUpdateResult actual = accept(delayed, location, delay);
            assertFalse(actual.isDropped());
            assertEquals("Delivery must not change the alert opportunity", expected.turnEvents.size(), actual.turnEvents.size());
            for (int event = 0; event < expected.turnEvents.size(); event++) {
                assertEquals(expected.turnEvents.get(event).type, actual.turnEvents.get(event).type);
                assertEquals(expected.turnEvents.get(event).timeSeconds, actual.turnEvents.get(event).timeSeconds, 0.0001);
            }
            assertEquals(display(immediate, time), display(delayed, time + delay));
            assertEquals(NavigationSessionResourceAdapter.buildCurrentDirectionDetails(
                            immediate, TestNavigationTextResources.metric(), time),
                    NavigationSessionResourceAdapter.buildCurrentDirectionDetails(
                            delayed, TestNavigationTextResources.metric(), time + delay));
        }
    }

    private static String display(NavigationSession session, long time) {
        return NavigationSessionResourceAdapter.buildState(session, TestNavigationTextResources.metric(),
                time + 3_000, time, null, null, null).routeStatus.progress.destinationLine;
    }

    private static NavigationSession start() {
        NavigationSession session = new NavigationSession();
        LatLon target = new LatLon(0, 1_000.0 / 111_320);
        session.loadRequest(new NavigationRequest("trekking", "Target", target, Collections.emptyList()));
        assertTrue(NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 1_000));
        session.setActiveLocationProviders("gps");
        accept(session, fix(1_000, 0, 0), 0);
        var request = session.prepareRouteRequest(true, 1_000);
        NavigationSessionResourceAdapter.applyRouteResult(session, TestNavigationTextResources.metric(), request,
                new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, 30.0 / 111_320), target),
                        Collections.singletonList(new VoiceHint(1, 2, 90, 0, 0)), 1_000, 1_000), 1_000);
        return session;
    }

    private static NavigationLocationUpdateResult accept(NavigationSession session, NavigationLocation location, long delay) {
        return NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(),
                location, location.getElapsedRealtimeOrTimeMs() + delay, 3_000);
    }

    private static NavigationLocation fix(long time, double east, float speed) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLongitude(east / 111_320);
        location.setAccuracy(5);
        location.setSpeed(speed);
        return location;
    }
}
