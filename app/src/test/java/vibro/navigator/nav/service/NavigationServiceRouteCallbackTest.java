package vibro.navigator.nav.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.location.NavigationLocation;

@RunWith(RobolectricTestRunner.class)
public class NavigationServiceRouteCallbackTest {
    private static final long NOW_MS = 10_000L;

    @Test
    public void onRouteApplied_ignoresStaleRouteResultSideEffects() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSession session = sessionWithPreparedRouteRequest(context);
        NavigationRouteRequestSnapshot snapshot = requireSnapshot(session.prepareRouteRequest(true, NOW_MS));
        CountingTurnEventDispatcher turnEvents = new CountingTurnEventDispatcher();
        CountingRunnable stateEmitter = new CountingRunnable();
        NavigationServiceRouteCallback callback = callback(context, session, turnEvents, stateEmitter);
        session.stop();

        callback.onRouteApplied(snapshot, route(), NOW_MS);

        assertEquals(0, turnEvents.calls);
        assertEquals(0, stateEmitter.calls);
        assertFalse(session.hasActiveRoute());
    }

    @Test
    public void onRouteFailure_ignoresStaleRouteFailureSideEffects() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSession session = sessionWithPreparedRouteRequest(context);
        NavigationRouteRequestSnapshot snapshot = requireSnapshot(session.prepareRouteRequest(true, NOW_MS));
        CountingTurnEventDispatcher turnEvents = new CountingTurnEventDispatcher();
        CountingRunnable stateEmitter = new CountingRunnable();
        NavigationServiceRouteCallback callback = callback(context, session, turnEvents, stateEmitter);
        session.stop();

        callback.onRouteFailure(snapshot, new IllegalStateException("old route failed"));

        assertEquals(0, turnEvents.calls);
        assertEquals(0, stateEmitter.calls);
        assertFalse(session.hasActiveRoute());
    }

    @NonNull
    private static NavigationSession sessionWithPreparedRouteRequest(@NonNull Context context) {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(48.2, 16.2),
                Collections.emptyList()
        ));
        session.start(context, NOW_MS);
        session.onRawLocationChanged(context, location(), NOW_MS);
        return session;
    }

    @NonNull
    private static NavigationServiceRouteCallback callback(
            @NonNull Context context,
            @NonNull NavigationSession session,
            @NonNull CountingTurnEventDispatcher turnEvents,
            @NonNull CountingRunnable stateEmitter
    ) {
        return new NavigationServiceRouteCallback(
                context,
                session,
                NoOpNavigationOrientation.create(NOW_MS),
                new NoOpForegroundController(),
                turnEvents,
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
    private static GeoJsonRoute route() {
        return new GeoJsonRoute(
                Arrays.asList(new LatLon(48.0, 16.0), new LatLon(48.2, 16.2)),
                Collections.emptyList(),
                0.0,
                30_000.0
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

    private static final class CountingRunnable implements Runnable {
        private int calls;

        @Override
        public void run() {
            calls++;
        }
    }
}
