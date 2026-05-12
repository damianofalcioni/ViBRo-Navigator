package vibro.navigator.nav.guidance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.location.Location;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

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
                NavigationTurnEvent.Type.INITIAL,
                state.onRouteApplied(route, polylineIndex, location(0.0, 0.0), 5f, 5f).get(0).type
        );

        NavigationTurnState.Progress progress = state.evaluate(
                route,
                polylineIndex,
                120.0,
                0,
                5f,
                5f,
                1_000L,
                0L
        );

        assertEquals(1, progress.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.PASSED, progress.turnEvents.get(0).type);
        assertTrue(progress.suggestedUpdateIntervalMs >= NavigationUpdateScheduler.bounds().min);
    }

    @Test
    public void evaluate_emitsApproachingIntermediateArrivalBeforeLaterTurn() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003),
                        new LatLon(0.0, 0.004)
                ),
                Arrays.asList(
                        new VoiceHint(1, 5, 0, 0.0, 0),
                        new VoiceHint(3, 5, 0, 0.0, 0)
                ),
                100.0,
                444.0
        );
        PolylineIndex polylineIndex = new PolylineIndex(route.track);

        state.onRouteApplied(
                route,
                polylineIndex,
                Collections.singletonList(new LatLon(0.0, 0.002)),
                location(0.0, 0.0),
                20f,
                5f
        );

        NavigationTurnState.Progress progress = state.evaluate(
                route,
                polylineIndex,
                120.0,
                1,
                20f,
                5f,
                1_000L,
                0L
        );

        assertEquals(NavigationTurnEvent.Type.PASSED, progress.turnEvents.get(0).type);
        assertEquals(101, progress.turnEvents.get(progress.turnEvents.size() - 1).hint.command);

        state.onIntermediateDestinationReached(2);
        NavigationTurnState.Progress afterReached = state.evaluate(
                route,
                polylineIndex,
                polylineIndex.distanceAtPointIndex(2),
                2,
                20f,
                5f,
                2_000L,
                0L
        );

        for (NavigationTurnEvent event : afterReached.turnEvents) {
            assertTrue(event.hint.command != 101);
        }
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
