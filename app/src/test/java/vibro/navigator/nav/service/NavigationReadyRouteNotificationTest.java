package vibro.navigator.nav.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationTurnEventDispatcher;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionResourceAdapter;
import vibro.navigator.nav.session.NavigationSessionSpeculativeRoutes;
import static org.junit.Assert.*;

public class NavigationReadyRouteNotificationTest {
    private static final String WARNING = "warning";
    @Test
    public void readyRoutePostsUsefulGuidanceImmediatelyWithoutACompetingWarning() {
        Fixture fixture = new Fixture(false);
        fixture.deliver();
        fixture.confirm();
        assertEquals(Collections.singletonList("turn"), fixture.foreground.posts);
        assertEquals(3, fixture.foreground.command);
        assertEquals(1, fixture.fastRequests);
        assertEquals("calculationMs=700 callbackWaitMs=100 confirmationWaitMs=200 applicationMs=0 totalMs=1050",
                fixture.snapshot.timing.applicationCompleted());
    }

    @Test
    public void readyRouteWithoutAnActionableInstructionRetainsTheOffRouteNotice() {
        Fixture fixture = new Fixture(true);
        fixture.deliver();
        fixture.confirm();
        assertEquals(Collections.singletonList(WARNING), fixture.foreground.posts);
        assertFalse("The route is ready; do not claim recalculation is pending",
                fixture.foreground.notice.routeRecalculationExpected);
        assertEquals(1, fixture.fastRequests);
    }

    @Test
    public void pendingRouteWarnsImmediatelyThenDeliversGuidanceWhenReady() {
        Fixture fixture = new Fixture(false);
        fixture.confirm();
        assertEquals(Collections.singletonList(WARNING), fixture.foreground.posts);
        assertTrue(fixture.foreground.notice.routeRecalculationExpected);
        assertEquals(0, fixture.fastRequests);
        fixture.deliver();
        assertEquals(Arrays.asList(WARNING, "turn"), fixture.foreground.posts);
        assertEquals("calculationMs=700 callbackWaitMs=500 confirmationWaitMs=0 applicationMs=0 totalMs=1250",
                fixture.snapshot.timing.applicationCompleted());
    }

    @Test
    public void pendingRouteFailureDoesNotConsumeOrReplaceTheWarning() {
        Fixture fixture = new Fixture(false);
        fixture.confirm();
        fixture.callback.onRouteFailure(fixture.snapshot, new IllegalStateException("routing unavailable"));
        assertEquals(Collections.singletonList(WARNING), fixture.foreground.posts);
        assertEquals(0, fixture.fastRequests);
    }

    @Test
    public void delayedConfirmedRouteCannotPostATurnFromAnExpiredFix() {
        Fixture fixture = new Fixture(false);
        fixture.confirm();
        fixture.nowMs = 26_000;
        fixture.snapshot.timing.resultReceived();
        fixture.callback.onRouteApplied(fixture.snapshot, Fixture.route(.0003), 12_100);
        assertEquals(Collections.singletonList(WARNING), fixture.foreground.posts);
        assertTrue(fixture.session.hasActiveRoute());
        assertEquals(1, fixture.fastRequests);
    }

    private static final class Fixture {
        private final NavigationSession session = new NavigationSession();
        private final Foreground foreground = new Foreground();
        private final NavigationServiceRouteCallback callback;
        private final NavigationRouteRequestSnapshot snapshot;
        private int fastRequests;
        private long nowMs = 12_000;
        private boolean confirmed;

        Fixture(boolean singleInstruction) {
            session.loadRequest(new NavigationRequest("trekking", "Target", new LatLon(0, .003), Collections.emptyList()));
            NavigationSessionResourceAdapter.start(session, TestNavigationTextResources.metric(), 10_000);
            accept(10_000, 0);
            NavigationRouteRequestSnapshot initial = session.prepareRouteRequest(true, 10_000);
            NavigationSessionResourceAdapter.applyRouteResult(session, TestNavigationTextResources.metric(), initial,
                    route(0), 10_000, 10_000);
            assertTrue(accept(12_000, .0003).shouldSpeculativelyRecalculateRoute());
            snapshot = session.speculativeRoutes().prepareRequest(false, 12_000,
                    NavigationRouteRecalculationReason.ROUTE_DEVIATION);
            assertNotNull(snapshot);
            snapshot.timing.start(() -> nowMs);
            nowMs = 12_700;
            snapshot.timing.calculationCompleted();
            NavigationTurnEventDispatcher events = new NavigationTurnEventDispatcher(foreground::sendImminentTurnNotification);
            callback = new NavigationServiceRouteCallback(TestNavigationTextResources.metric(), () -> singleInstruction,
                    session, NoOpNavigationOrientation.create(13_000), foreground, events::dispatch,
                    () -> { fastRequests++; nowMs += 50; return nowMs; }, () -> { }, pending -> { }, () -> nowMs);
        }

        void deliver() {
            nowMs = confirmed ? 13_200 : 12_800;
            snapshot.timing.resultReceived();
            callback.onRouteApplied(snapshot, route(.0003), 12_100);
        }

        void confirm() {
            nowMs = 13_000;
            NavigationLocationUpdateResult result = accept(13_000, .0003);
            assertTrue(result.shouldRecalculateRoute());
            NavigationSessionSpeculativeRoutes.Confirmation confirmation = session.speculativeRoutes().confirmRecalculation(13_000);
            assertNotEquals(NavigationSessionSpeculativeRoutes.Confirmation.NONE, confirmation);
            confirmed = true;
            callback.onSpeculativeRouteConfirmed(result.getRerouteNotice(), confirmation);
        }

        private NavigationLocationUpdateResult accept(long time, double north) {
            NavigationLocation location = new NavigationLocation("gps");
            location.setTime(time, time);
            location.setLatitude(north);
            location.setLongitude(time == 13_000 ? .0002 : 0);
            location.setAccuracy(5);
            return NavigationSessionResourceAdapter.onRawLocationChanged(session, TestNavigationTextResources.metric(), location, time);
        }

        private static GeoJsonRoute route(double north) {
            return new GeoJsonRoute(Arrays.asList(new LatLon(north, 0), new LatLon(north, .001), new LatLon(north, .003)),
                    Collections.singletonList(new VoiceHint(1, 3, 0, 222, -90)), 300, 333);
        }
    }

    private static final class Foreground extends NoOpForegroundController {
        private final List<String> posts = new ArrayList<>();
        private NavigationRerouteNotice notice;
        private int command;
        @Override public void sendOffRouteNotification(NavigationRerouteNotice notice) {
            posts.add(WARNING);
            this.notice = notice;
        }
        @Override public void sendImminentTurnNotification(VoiceHint hint, double distance, double time) {
            posts.add("turn");
            command = hint.command;
        }
    }
}
