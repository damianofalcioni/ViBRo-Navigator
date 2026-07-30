package vibro.navigator.nav.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionResourceAdapter;

public class NavigationServiceRouteCallbackTest {
    private static final long NOW_MS = 10_000L;
    private static final NavigationTextResources TEXT_RESOURCES = TestNavigationTextResources.metric();

    @Test
    public void onRouteApplied_ignoresStaleRouteResultSideEffects() {
        NavigationSession session = sessionWithPreparedRouteRequest();
        NavigationRouteRequestSnapshot snapshot = requireSnapshot(session.prepareRouteRequest(true, NOW_MS));
        CountingTurnEventDispatcher turnEvents = new CountingTurnEventDispatcher();
        CountingFastLocationRequester fastLocationRequester = new CountingFastLocationRequester();
        CountingRunnable stateEmitter = new CountingRunnable();
        NavigationServiceRouteCallback callback = callback(
                session,
                turnEvents,
                fastLocationRequester,
                stateEmitter
        );
        session.stop();

        callback.onRouteApplied(snapshot, route(), NOW_MS);

        assertEquals(0, turnEvents.calls);
        assertEquals(0, fastLocationRequester.calls);
        assertEquals(0, stateEmitter.calls);
        assertFalse(session.hasActiveRoute());
    }

    @Test
    public void onRouteApplied_requestsFastLocationUpdatesForCurrentRoute() {
        NavigationSession session = sessionWithPreparedRouteRequest();
        NavigationRouteRequestSnapshot snapshot = requireSnapshot(session.prepareRouteRequest(true, NOW_MS));
        CountingTurnEventDispatcher turnEvents = new CountingTurnEventDispatcher();
        CountingFastLocationRequester fastLocationRequester = new CountingFastLocationRequester();
        CountingRunnable stateEmitter = new CountingRunnable();
        NavigationServiceRouteCallback callback = callback(
                session,
                turnEvents,
                fastLocationRequester,
                stateEmitter
        );

        callback.onRouteApplied(snapshot, route(), NOW_MS);

        assertEquals(1, turnEvents.calls);
        assertEquals(1, fastLocationRequester.calls);
        assertEquals(1, stateEmitter.calls);
    }

    @Test
    public void onSpeculativeRouteConfirmed_appliesDeferredRouteResult() {
        NavigationSession session = sessionWithActiveRoute();
        CountingTurnEventDispatcher turnEvents = new CountingTurnEventDispatcher();
        CountingFastLocationRequester fastLocationRequester = new CountingFastLocationRequester();
        CountingRunnable stateEmitter = new CountingRunnable();
        NavigationServiceRouteCallback callback = callback(
                session,
                turnEvents,
                fastLocationRequester,
                stateEmitter
        );

        NavigationLocationUpdateResult tentative = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                TEXT_RESOURCES,
                routeModeLocation(0.0003, 0.0, NOW_MS + 2_000L),
                NOW_MS + 2_000L
        );
        NavigationRouteRequestSnapshot speculative = requireSnapshot(session.speculativeRoutes().prepareRequest(
                false,
                NOW_MS + 2_000L,
                NavigationRouteRecalculationReason.ROUTE_DEVIATION
        ));
        callback.onRouteApplied(speculative, replacementRoute(), NOW_MS + 2_100L);

        assertTrue(tentative.shouldSpeculativelyRecalculateRoute());
        assertEquals(0, turnEvents.calls);
        assertEquals(0, fastLocationRequester.calls);
        assertEquals(1, stateEmitter.calls);

        NavigationLocationUpdateResult confirmed = NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                TEXT_RESOURCES,
                routeModeLocation(0.0003, 0.00005, NOW_MS + 3_000L),
                NOW_MS + 3_000L
        );
        callback.onSpeculativeRouteConfirmed(
                confirmed.getRerouteNotice(),
                session.speculativeRoutes().confirmRecalculation()
        );

        assertTrue(confirmed.shouldRecalculateRoute());
        assertEquals(1, turnEvents.calls);
        assertEquals(1, fastLocationRequester.calls);
        assertEquals(2, stateEmitter.calls);
    }

    @Test
    public void onRouteFailure_ignoresStaleRouteFailureSideEffects() {
        NavigationSession session = sessionWithPreparedRouteRequest();
        NavigationRouteRequestSnapshot snapshot = requireSnapshot(session.prepareRouteRequest(true, NOW_MS));
        CountingTurnEventDispatcher turnEvents = new CountingTurnEventDispatcher();
        CountingRunnable stateEmitter = new CountingRunnable();
        NavigationServiceRouteCallback callback = callback(
                session,
                turnEvents,
                new CountingFastLocationRequester(),
                stateEmitter
        );
        session.stop();

        callback.onRouteFailure(snapshot, new IllegalStateException("old route failed"));

        assertEquals(0, turnEvents.calls);
        assertEquals(0, stateEmitter.calls);
        assertFalse(session.hasActiveRoute());
    }

    @NonNull
    private static NavigationSession sessionWithPreparedRouteRequest() {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(48.2, 16.2),
                Collections.emptyList()
        ));
        NavigationSessionResourceAdapter.start(session, TEXT_RESOURCES, NOW_MS);
        NavigationSessionResourceAdapter.onRawLocationChanged(session, TEXT_RESOURCES, location(), NOW_MS);
        return session;
    }

    @NonNull
    private static NavigationSession sessionWithActiveRoute() {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        ));
        NavigationSessionResourceAdapter.start(session, TEXT_RESOURCES, NOW_MS);
        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                TEXT_RESOURCES,
                routeModeLocation(0.0, 0.0, NOW_MS),
                NOW_MS
        );
        NavigationRouteRequestSnapshot snapshot = requireSnapshot(session.prepareRouteRequest(true, NOW_MS));
        NavigationSessionResourceAdapter.applyRouteResult(
                session,
                TEXT_RESOURCES,
                snapshot,
                routeLine(),
                NOW_MS,
                NOW_MS
        );
        return session;
    }

    @NonNull
    private static NavigationServiceRouteCallback callback(
            @NonNull NavigationSession session,
            @NonNull CountingTurnEventDispatcher turnEvents,
            @NonNull CountingFastLocationRequester fastLocationRequester,
            @NonNull CountingRunnable stateEmitter
    ) {
        return new NavigationServiceRouteCallback(
                TEXT_RESOURCES,
                () -> false,
                session,
                NoOpNavigationOrientation.create(NOW_MS),
                new NoOpForegroundController(),
                turnEvents,
                fastLocationRequester,
                stateEmitter,
                pending -> {
                }
        );
    }

    @NonNull
    private static NavigationRouteRequestSnapshot requireSnapshot(
            @Nullable NavigationRouteRequestSnapshot snapshot
    ) {
        assertNotNull(snapshot);
        return snapshot;
    }

    @NonNull
    private static NavigationLocation location() {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(48.0);
        location.setLongitude(16.0);
        location.setTime(NOW_MS);
        location.setAccuracy(5f);
        return location;
    }

    @NonNull
    private static NavigationLocation routeModeLocation(double lat, double lon, long timeMs) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(5f);
        return location;
    }

    @NonNull
    private static GeoJsonRoute route() {
        return new GeoJsonRoute(
                Arrays.asList(new LatLon(48.0, 16.0), new LatLon(48.2, 16.2)),
                Collections.emptyList(),
                0.0,
                30_000.0
        );
    }

    @NonNull
    private static GeoJsonRoute routeLine() {
        return new GeoJsonRoute(
                Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.003)),
                Collections.emptyList(),
                0.0,
                333.0
        );
    }

    @NonNull
    private static GeoJsonRoute replacementRoute() {
        return new GeoJsonRoute(
                Arrays.asList(new LatLon(0.0003, 0.0), new LatLon(0.0003, 0.003)),
                Collections.emptyList(),
                0.0,
                333.0
        );
    }

    private static final class CountingTurnEventDispatcher
            implements NavigationServiceRouteCallback.TurnEventDispatcher {
        private int calls;

        @Override
        public void dispatch(@NonNull List<NavigationTurnEvent> turnEvents) {
            calls++;
        }
    }

    private static final class CountingFastLocationRequester
            implements NavigationServiceRouteCallback.RouteAppliedLocationRequester {
        private int calls;

        @Override
        public long requestFastLocationUpdates() {
            calls++;
            return NOW_MS;
        }
    }

    private static final class CountingRunnable implements Runnable {
        private int calls;

        @Override
        public void run() {
            calls++;
        }
    }
}
