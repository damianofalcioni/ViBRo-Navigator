package vibro.navigator.nav.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.android.intent.AndroidInitialRouteResult;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.brouter.BRouterRouteException;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionInitialRoute;
import vibro.navigator.nav.session.NavigationSessionResourceAdapter;

@RunWith(RobolectricTestRunner.class)
public class NavigationServiceInitialRouteTest {

    private static final String MISSING_TILE_ERROR = "datafile E10_N45.rd5 not found";
    private final NavigationSessionInitialRoute initialRoute = new NavigationSessionInitialRoute();
    private final NavigationSession session = new NavigationSession();
    private final TestNavigationTextResources text = TestNavigationTextResources.metric();
    private final List<Boolean> results = new ArrayList<>();

    @Test
    public void missingMapResultIsDeliveredOnceAfterLocationAndRouteFailure() {
        initializeStart();
        assertFalse(initialRoute.detectMissingData(session));
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(results.isEmpty());

        applyFailure(1_000, BRouterRouteException.serviceUnavailable("BRouter service not available"));
        assertFalse(initialRoute.detectMissingData(session));
        applyFailure(20_000, BRouterRouteException.fromTextResponse(MISSING_TILE_ERROR));
        assertTrue(initialRoute.detectMissingData(session));
        assertFalse(initialRoute.detectMissingData(session));
        initialRoute.finish(true);
        initialRoute.finish(false);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(Collections.singletonList(true), results);
    }

    @Test
    public void replacingWithPhoneRouteDetachesPendingCarResult() {
        initializeStart();
        initialRoute.replace(AndroidInitialRouteResult.fromIntent(new Intent()));
        applyFailure(1_000, BRouterRouteException.fromTextResponse(MISSING_TILE_ERROR));
        assertFalse(initialRoute.detectMissingData(session));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(Collections.singletonList(false), results);
    }

    @Test
    public void queuedMissingMapResultCannotAffectAReplacementRequest() {
        var commands = new NavigationServiceCommandHandler(
                intent -> session.loadRequest(AndroidNavigationRequestIntentContract.fromIntent(intent)),
                () -> assertTrue(NavigationSessionResourceAdapter.start(session, text, 1_000)),
                session::stop, () -> { }, () -> { });
        var cleanup = new ArrayDeque<Runnable>();
        Intent car = startIntent(45.6);
        AndroidInitialRouteResult.putInto(car, results::add);
        commands.handle(car, 0, 1);
        session.setActiveLocationProviders("gps");
        applyFailure(1_000, BRouterRouteException.fromTextResponse(MISSING_TILE_ERROR));
        commands.reportInitialRoute(session, cleanup::add);
        assertEquals(1, cleanup.size());

        commands.handle(startIntent(45.7), 0, 2);
        cleanup.remove().run();
        assertTrue(session.isStarted());
        assertEquals(45.7, session.currentNavigationRequest().destination.lat, 0);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(Collections.singletonList(false), results);
    }

    private static Intent startIntent(double destinationLatitude) {
        Intent intent = new Intent(NavigationService.ACTION_START);
        AndroidNavigationRequestIntentContract.putInto(intent, request(destinationLatitude));
        return intent;
    }

    private static NavigationRequest request(double destinationLatitude) {
        return new NavigationRequest("car-fast", "Test", new LatLon(destinationLatitude, 10.5), Collections.emptyList());
    }

    private void initializeStart() {
        Intent intent = new Intent();
        AndroidInitialRouteResult.putInto(intent, results::add);
        initialRoute.replace(AndroidInitialRouteResult.fromIntent(intent));
        session.loadRequest(request(45.6));
        assertTrue(NavigationSessionResourceAdapter.start(session, text, 1_000));
        session.setActiveLocationProviders("gps");
    }

    private void applyFailure(long now, BRouterRouteException failure) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setLatitude(45.5);
        fix.setLongitude(10.5);
        fix.setAccuracy(5);
        fix.setTime(now, now);
        NavigationSessionResourceAdapter.onRawLocationChanged(session, text, fix, now, 1_000, false);
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, now);
        assertNotNull(snapshot);
        assertTrue(NavigationSessionResourceAdapter.applyRouteFailure(session, text, snapshot, failure));
    }
}
