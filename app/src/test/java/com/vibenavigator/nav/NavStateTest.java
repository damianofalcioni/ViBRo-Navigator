package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.VoiceHint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavStateTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void from_skipsHintsInsideAccuracyRadius() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.00005),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Arrays.asList(
                        new VoiceHint(1, 2, 0, 0.0, 0),
                        new VoiceHint(2, 5, 0, 0.0, 0),
                        new VoiceHint(3, 3, 0, 0.0, 0)
                ),
                180.0,
                222.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                0,
                1f,
                false,
                20f,
                locationAt(0.0, 0.0),
                45.0,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 222.0)),
                context
        );

        assertTrue(state.nextLine.contains("111 m"));
        assertTrue(state.afterNextLine.contains("222 m"));
    }

    @Test
    public void from_buildsCompassStateAroundCurrentLocation() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(48.2000, 16.3600),
                        new LatLon(48.2005, 16.3600),
                        new LatLon(48.2010, 16.3605)
                ),
                Collections.emptyList(),
                180.0,
                130.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                1f,
                false,
                5f,
                locationAt(48.2000, 16.3600),
                90.0,
                8.0f,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 130.0)),
                context
        );

        assertNotNull(state.compassState);
        assertEquals(90.0f, state.compassState.headingDegrees, 0.01f);
        assertEquals(8.0f, state.compassState.headingAccuracyDegrees, 0.01f);
        assertTrue(state.compassState.routePoints.size() >= 2);
        assertTrue(state.compassState.visibleRadiusMeters >= 90f);
        assertEquals(
                state.compassState.visibleRadiusMeters / 60f,
                state.compassState.referenceSpeedMps,
                0.01f
        );
        assertTrue(state.compassState.passedRoutePoints.size() >= 1);
    }

    @Test
    public void from_afterProgressSplitsPassedAndUpcomingCompassRoute() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003)
                ),
                Collections.emptyList(),
                240.0,
                333.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                140.0,
                -1,
                2f,
                false,
                5f,
                locationAt(0.0, 0.0013),
                0.0,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 333.0)),
                context
        );

        assertNotNull(state.compassState);
        assertTrue(state.compassState.passedRoutePoints.size() >= 2);
        assertTrue(state.compassState.routePoints.size() >= 2);
    }

    @Test
    public void from_whenStationaryFitsFullRouteInsideCompass() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.003),
                        new LatLon(0.0, 0.006),
                        new LatLon(0.0, 0.009)
                ),
                Collections.emptyList(),
                600.0,
                999.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0f,
                true,
                5f,
                locationAt(0.0, 0.0),
                0.0,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 999.0)),
                context
        );

        assertNotNull(state.compassState);
        assertTrue(state.compassState.visibleRadiusMeters > 1_000f);
        assertTrue(state.compassState.destinationWithinRadius);
        assertTrue(state.compassState.routePoints.size() >= 4);
    }

    @Test
    public void from_whenMovingUsesSmoothedSixtySecondRadius() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.003),
                        new LatLon(0.0, 0.006),
                        new LatLon(0.0, 0.009)
                ),
                Collections.emptyList(),
                600.0,
                999.0
        );

        NavState stationaryState = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0f,
                true,
                5f,
                locationAt(0.0, 0.0),
                0.0,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 999.0)),
                context
        );

        NavState movingState = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                2f,
                false,
                5f,
                locationAt(0.0, 0.0),
                0.0,
                null,
                stationaryState.compassState.visibleRadiusMeters,
                1_000L,
                NavState.NO_DEADLINE,
                1_000L,
                Collections.singletonList(new NavTarget("Destination", 999.0)),
                context
        );

        assertNotNull(movingState.compassState);
        assertTrue(movingState.compassState.visibleRadiusMeters > 120f);
        assertTrue(movingState.compassState.visibleRadiusMeters < stationaryState.compassState.visibleRadiusMeters);
        assertFalse(movingState.compassState.destinationWithinRadius);
        assertEquals(
                movingState.compassState.visibleRadiusMeters / 60f,
                movingState.compassState.referenceSpeedMps,
                0.01f
        );
    }

    @Test
    public void from_whenStationaryOverviewKeepsSanitizedActualSpeedForLegend() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.003),
                        new LatLon(0.0, 0.006)
                ),
                Collections.emptyList(),
                400.0,
                666.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0.4f,
                true,
                5f,
                locationAt(0.0, 0.0),
                0.0,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 666.0)),
                context
        );

        assertNotNull(state.compassState);
        assertEquals(1.0f, state.compassState.referenceSpeedMps, 0.01f);
    }

    @NonNull
    private static android.location.Location locationAt(double lat, double lon) {
        android.location.Location location = new android.location.Location("test");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(1L);
        return location;
    }
}
