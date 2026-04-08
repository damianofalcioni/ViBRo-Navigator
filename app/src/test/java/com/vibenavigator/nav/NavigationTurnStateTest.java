package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.location.Location;

import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationTurnStateTest {

    @Test
    public void onRouteApplied_buildsInitialTurnEventAndAdvanceMarksTurnPassed() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = routeWithHint();
        PolylineIndex polylineIndex = new PolylineIndex(route.track);

        state.reset();
        assertEquals(
                NavigationSession.TurnEvent.Type.INITIAL,
                state.onRouteApplied(route, polylineIndex, location(0.0, 0.0), 5f, 5f).get(0).type
        );

        NavigationTurnState.Progress progress = state.evaluate(
                route,
                polylineIndex,
                120.0,
                5f,
                5f,
                1_000L,
                0L
        );

        assertEquals(1, progress.turnEvents.size());
        assertEquals(NavigationSession.TurnEvent.Type.PASSED, progress.turnEvents.get(0).type);
        assertTrue(progress.suggestedUpdateIntervalMs >= NavigationUpdateScheduler.bounds().min);
    }

    private static GeoJsonRoute routeWithHint() {
        return new GeoJsonRoute(
                Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001)),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                60.0,
                111.0
        );
    }

    private static Location location(double lat, double lon) {
        Location location = new Location("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }
}
