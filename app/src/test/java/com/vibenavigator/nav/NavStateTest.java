package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.R;
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
                0,
                1f,
                false,
                20f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 222.0)),
                context
        );

        assertTrue(state.nextLine.contains("111 m"));
        assertTrue(state.afterNextLine.contains("111 m"));
    }

    @Test
    public void from_formatsSecondHintRelativeToFirstHint() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003)
                ),
                Arrays.asList(
                        new VoiceHint(1, 2, 0, 0.0, 0),
                        new VoiceHint(2, 3, 0, 0.0, 0)
                ),
                Arrays.asList(0.0, 20.0, 45.0, 75.0),
                75.0,
                333.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                0,
                0,
                0f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 333.0)),
                context
        );

        assertTrue(state.nextLine.contains("111 m"));
        assertTrue(state.nextLine.contains("20 s"));
        assertTrue(state.afterNextLine.contains("111 m"));
        assertTrue(state.afterNextLine.contains("25 s"));
    }

    @Test
    public void from_showsUnavailableHintTimeWhenSameSegmentSpeedIsUnavailable() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                Arrays.asList(0.0, 45.0),
                45.0,
                111.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                0,
                0,
                0f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 111.0)),
                context
        );

        assertTrue(state.nextLine.contains("45 s"));
    }

    @Test
    public void from_usesRouteTrackTimesWhenHintIsAfterCurrentSegment() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.singletonList(new VoiceHint(2, 2, 0, 0.0, 0)),
                Arrays.asList(0.0, 20.0, 45.0),
                45.0,
                222.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                0,
                0,
                0f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 222.0)),
                context
        );

        assertTrue(state.nextLine.contains("45 s"));
    }

    @Test
    public void from_showsUnavailableHintTimeWhenNoSpeedAndNoTrackTimesExist() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                0.0,
                111.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                0,
                0,
                0f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 111.0)),
                context
        );

        assertTrue(state.nextLine.contains("--"));
    }

    @Test
    public void from_usesHybridEtaForDestinationProgress() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.0001),
                        new LatLon(0.0, 0.001)
                ),
                Collections.emptyList(),
                Arrays.asList(0.0, 10.0, 30.0),
                30.0,
                111.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                2f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 111.0)),
                context
        );

        assertTrue(state.destinationLine.contains("Destination"));
        assertTrue(state.destinationLine.contains("111 m"));
        assertTrue(state.destinationLine.contains("26 s"));
    }

    @Test
    public void from_usesHybridEtaForNextIntermediateStopProgress() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.0001),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.emptyList(),
                Arrays.asList(0.0, 10.0, 30.0, 55.0),
                55.0,
                222.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                2f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Arrays.asList(
                        new NavTarget("Stop 1", 111.0),
                        new NavTarget("Destination", 222.0)
                ),
                context
        );

        assertTrue(state.stopProgressBlock.contains("Stop 1"));
        assertTrue(state.stopProgressBlock.contains("111 m"));
        assertTrue(state.stopProgressBlock.contains("26 s"));
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
        android.location.Location movingLocation = locationAt(48.2000, 16.3600);
        movingLocation.setSpeed(2.5f);

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                2.5f,
                false,
                5f,
                movingLocation,
                null,
                90.0,
                8.0f,
                null,
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
        assertTrue(state.compassState.movingScaleActive);
        assertEquals(13f, state.compassState.routeThresholdMeters, 0.01f);
        assertTrue(state.compassState.passedRoutePoints.size() >= 1);
    }

    @Test
    public void from_usesProvidedCompassAccuracyForCompassOverlayAndThreshold() {
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
        android.location.Location movingLocation = locationAt(48.2000, 16.3600);
        movingLocation.setSpeed(2.5f);

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                2.5f,
                2.5f,
                false,
                5f,
                9f,
                movingLocation,
                null,
                90.0,
                8.0f,
                null,
                null,
                0L,
                null,
                null,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 130.0)),
                context
        );

        assertNotNull(state.compassState);
        assertEquals(9f, state.compassState.accuracyRadiusMeters, 0.01f);
        assertEquals(17f, state.compassState.routeThresholdMeters, 0.01f);
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
                1,
                2f,
                false,
                5f,
                locationAt(0.0, 0.0013),
                null,
                0.0,
                null,
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
                0,
                0f,
                true,
                5f,
                locationAt(0.0, 0.0),
                null,
                0.0,
                null,
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
        assertFalse(state.compassState.movingScaleActive);
        assertEquals(0f, state.compassState.routeThresholdMeters, 0.01f);
        assertTrue(state.compassState.routePoints.size() >= 4);
    }

    @Test
    public void from_whenMovingUsesSmoothedSixtySecondRadiusWithoutUpperCap() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.03),
                        new LatLon(0.0, 0.06),
                        new LatLon(0.0, 0.09)
                ),
                Collections.emptyList(),
                6_000.0,
                9_999.0
        );

        NavState stationaryState = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                0f,
                true,
                5f,
                locationAt(0.0, 0.0),
                null,
                0.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 9_999.0)),
                context
        );

        android.location.Location movingLocation = locationAt(0.0, 0.0);
        movingLocation.setSpeed(20f);
        NavState movingState = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                20f,
                false,
                5f,
                movingLocation,
                null,
                0.0,
                null,
                stationaryState.compassState.visibleRadiusMeters,
                null,
                1_000L,
                NavState.NO_DEADLINE,
                1_000L,
                Collections.singletonList(new NavTarget("Destination", 9_999.0)),
                context
        );

        assertNotNull(movingState.compassState);
        assertTrue(movingState.compassState.visibleRadiusMeters > 600f);
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
                0,
                0.4f,
                true,
                5f,
                locationAt(0.0, 0.0),
                null,
                0.0,
                null,
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

    @Test
    public void from_whenMovingWithoutReliableSpeedReusesPreviousReliableMovingRadius() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.01),
                        new LatLon(0.0, 0.02)
                ),
                Collections.emptyList(),
                2_000.0,
                2_222.0
        );

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                3f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                0.0,
                null,
                null,
                240f,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 2_222.0)),
                context
        );

        assertNotNull(state.compassState);
        assertEquals(240f, state.compassState.visibleRadiusMeters, 0.01f);
        assertEquals(4.0f, state.compassState.referenceSpeedMps, 0.01f);
    }

    @Test
    public void from_formatsGpsStatusWithSpeedElevationAccuracyAndSatelliteCount() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(48.2000, 16.3600),
                        new LatLon(48.2010, 16.3610)
                ),
                Collections.emptyList(),
                120.0,
                140.0
        );
        android.location.Location location = locationAt(48.2000, 16.3600);
        location.setAltitude(245.4d);
        location.setBearing(182.2f);
        location.setBearingAccuracyDegrees(9.4f);

        NavState state = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                4.5f,
                false,
                5.2f,
                location,
                7,
                90.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget("Destination", 140.0)),
                context
        );

        assertEquals("16 km/h ↑245 m 182° • ±5 m 9° • (7)", state.gpsStatusLine);
    }

    @Test
    public void displayStatusBlock_prefersNoticeOverProgressContent() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.emptyList(),
                Arrays.asList(0.0, 15.0, 30.0),
                30.0,
                222.0
        );

        NavState baseState = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                2f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Arrays.asList(
                        new NavTarget("Stop 1", 111.0),
                        new NavTarget("Destination", 222.0)
                ),
                context
        );

        NavState state = NavState.withNotice(baseState, "Rerouting around blockage");

        assertEquals("Rerouting around blockage", state.displayStatusBlock());
    }

    @Test
    public void displayStatusBlock_combinesDestinationAndStopProgress() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.emptyList(),
                Arrays.asList(0.0, 15.0, 30.0),
                30.0,
                222.0
        );

        NavState baseState = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                2f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Arrays.asList(
                        new NavTarget("Stop 1", 111.0),
                        new NavTarget("Destination", 222.0)
                ),
                context
        );

        assertEquals(
                baseState.destinationLine + "\n" + baseState.stopProgressBlock,
                baseState.displayStatusBlock()
        );
    }

    @Test
    public void displayStatusBlock_showsPauseNoticeInsteadOfCombinedProgress() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.emptyList(),
                Arrays.asList(0.0, 15.0, 30.0),
                30.0,
                222.0
        );

        NavState baseState = NavState.from(
                route,
                new com.vibenavigator.nav.route.PolylineIndex(route.track),
                0.0,
                -1,
                0,
                2f,
                false,
                5f,
                locationAt(0.0, 0.0),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Arrays.asList(
                        new NavTarget("Stop 1", 111.0),
                        new NavTarget("Destination", 222.0)
                ),
                context
        );

        NavState state = NavState.withPauseState(context, baseState, true);

        assertEquals(context.getString(R.string.nav_paused_notice), state.displayStatusBlock());
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
