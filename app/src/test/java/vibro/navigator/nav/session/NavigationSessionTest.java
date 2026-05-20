package vibro.navigator.nav.session;


import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.location.NavigationLocationUpdateResult;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationSessionTest {
    private static final String DESTINATION = "Destination";
    private static final String TREKKING_PROFILE = "trekking";

    @Test
    public void buildState_marksPausedSessionsAndClearsPauseStateOnResume() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        ));

        assertTrue(session.start(context, 0L));
        assertTrue(session.pause());

        NavState pausedState = session.buildState(
                context,
                NavState.NO_DEADLINE,
                0L,
                null,
                null,
                null
        );

        assertTrue(pausedState.pauseStatus.paused);
        assertTrue(pausedState.routeStatus.progress.detailBlock.contains(context.getString(R.string.nav_paused_notice)));
        assertTrue(session.resume());

        NavState resumedState = session.buildState(
                context,
                NavState.NO_DEADLINE,
                0L,
                null,
                null,
                null
        );

        assertFalse(resumedState.pauseStatus.paused);
    }

    @Test
    public void prepareRouteRequest_excludesReachedIntermediateStops() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Arrays.asList(new LatLon(0.0, 0.001), new LatLon(0.0, 0.002))
        ));
        assertTrue(session.start(context, 0L));
        long nowMs = System.currentTimeMillis();
        session.onRawLocationChanged(context, location(0.0, 0.0, nowMs), nowMs);
        NavigationRouteRequestSnapshot firstSnapshot = session.prepareRouteRequest(true, nowMs);
        assertNotNull(firstSnapshot);
        assertEquals(2, firstSnapshot.intermediates.size());
        session.applyRouteResult(context, firstSnapshot, routeWithoutHints(), 500L);

        session.onRawLocationChanged(context, location(0.0, 0.001, nowMs + 2_000L, 120f), nowMs + 2_000L);
        NavigationRouteRequestSnapshot secondSnapshot = session.prepareRouteRequest(true, nowMs + 3_000L);

        assertNotNull(secondSnapshot);
        assertEquals(1, secondSnapshot.intermediates.size());
        assertEquals(0.002, secondSnapshot.intermediates.get(0).lon, 0.0);
    }

    @Test
    public void buildState_includesAcceptedFixCountInGpsStatus() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        ));
        assertTrue(session.start(context, 0L));
        long nowMs = System.currentTimeMillis();

        session.onRawLocationChanged(context, location(0.0, 0.0, nowMs), nowMs);
        session.onRawLocationChanged(context, location(0.0, 0.0001, nowMs + 1_000L), nowMs + 1_000L);

        NavState state = session.buildState(
                context,
                NavState.NO_DEADLINE,
                nowMs + 1_000L,
                7,
                null,
                null
        );

        assertTrue(state.gpsStatus.statusLine.contains("(7) #2"));
    }

    @Test
    public void onRawLocationChanged_resumesFastPollingAfterLongAcceptedFixGap() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                TREKKING_PROFILE,
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        ));
        long nowMs = System.currentTimeMillis();
        assertTrue(session.start(context, nowMs));

        session.onRawLocationChanged(context, location(0.0, 0.0, nowMs), nowMs);
        NavigationRouteRequestSnapshot snapshot = session.prepareRouteRequest(true, nowMs);
        assertNotNull(snapshot);
        session.applyRouteResult(context, snapshot, routeWithoutHints(), nowMs);
        for (int i = 1; i <= 5; i++) {
            long sampleTimeMs = nowMs + i * 1_000L;
            session.onRawLocationChanged(context, location(0.0, i * 0.0001, sampleTimeMs), sampleTimeMs);
        }

        long resumedTimeMs = nowMs + 21_000L;
        NavigationLocationUpdateResult result = session.onRawLocationChanged(
                context,
                location(0.0, 0.0006, resumedTimeMs),
                resumedTimeMs
        );

        assertFalse(result.isDropped());
        assertEquals(1_000L, result.getSuggestedUpdateIntervalMs());
    }

    @NonNull
    private static GeoJsonRoute routeWithoutHints() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003)
                ),
                Collections.emptyList(),
                180.0,
                333.0
        );
    }

    @NonNull
    private static Location location(double lat, double lon, long timeMs) {
        return location(lat, lon, timeMs, 5f);
    }

    @NonNull
    private static Location location(double lat, double lon, long timeMs, float accuracyMeters) {
        Location location = new Location("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(accuracyMeters);
        return location;
    }
}
