package vibro.navigator.nav.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import vibro.navigator.nav.location.NavigationLocation;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.compass.CompassRadiusTransition;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassStateInput;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.policy.NavigationSpeedBucket;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NavStateTest {
    private static final String DESTINATION = "Destination";
    private static final String DISTANCE_111_METERS = "111 m";
    private static final String STOP_1 = "Stop 1";

    private final NavigationTextResources context = TestNavigationTextResources.metric();

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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 222.0)),
                context
        );

        assertTrue(state.routeStatus.guidance.nextLine.contains(DISTANCE_111_METERS));
        assertTrue(state.routeStatus.guidance.afterNextLine.contains(DISTANCE_111_METERS));
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 333.0)),
                context
        );

        assertTrue(state.routeStatus.guidance.nextLine.contains(DISTANCE_111_METERS));
        assertTrue(state.routeStatus.guidance.nextLine.contains("20 s"));
        assertTrue(state.routeStatus.guidance.afterNextLine.contains(DISTANCE_111_METERS));
        assertTrue(state.routeStatus.guidance.afterNextLine.contains("25 s"));
    }

    @Test
    public void from_synthesizesArrivalHintAfterFinalManeuver() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Arrays.asList(
                        new VoiceHint(1, 2, 0, 0.0, 0),
                        new VoiceHint(2, 100, 0, 0.0, 0)
                ),
                Arrays.asList(0.0, 20.0, 45.0),
                45.0,
                222.0
        );

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 222.0)),
                context
        );

        assertTrue(state.routeStatus.guidance.nextLine.contains(DISTANCE_111_METERS));
        assertTrue(state.routeStatus.guidance.afterNextLine.contains(DISTANCE_111_METERS));
        assertTrue(state.routeStatus.guidance.afterNextLine.contains("25 s"));
        assertTrue(state.routeStatus.guidance.afterNextLine.contains(context.getString(R.string.direction_arrive)));
        assertFalse(state.routeStatus.guidance.nextLine.contains(context.getString(R.string.direction_arrive)));

        NavState afterFinalManeuver = from(
                route,
                new PolylineIndex(route.track),
                111.0,
                1,
                1,
                0f,
                false,
                5f,
                locationAt(0.0, 0.001),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget(DESTINATION, 222.0)),
                context
        );

        assertTrue(afterFinalManeuver.routeStatus.guidance.nextLine.contains(DISTANCE_111_METERS));
        assertTrue(afterFinalManeuver.routeStatus.guidance.nextLine.contains("25 s"));
        assertTrue(afterFinalManeuver.routeStatus.guidance.nextLine.contains(
                context.getString(R.string.direction_arrive)
        ));
        assertEquals("", afterFinalManeuver.routeStatus.guidance.afterNextLine);

        GeoJsonRoute nearDestinationRoute = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.emptyList(),
                Arrays.asList(0.0, 20.0),
                20.0,
                111.0
        );
        NavState nearDestinationState = from(
                nearDestinationRoute,
                new PolylineIndex(nearDestinationRoute.track),
                108.0,
                0,
                0,
                1.5f,
                false,
                10f,
                locationAt(0.0, 0.00097),
                null,
                45.0,
                null,
                null,
                null,
                0L,
                NavState.NO_DEADLINE,
                0L,
                Collections.singletonList(new NavTarget(DESTINATION, 111.0)),
                context
        );

        assertTrue(nearDestinationState.routeStatus.guidance.nextLine.contains("3 m"));
        assertTrue(nearDestinationState.routeStatus.guidance.nextLine.contains("2 s"));
        assertTrue(nearDestinationState.routeStatus.guidance.nextLine.contains(
                context.getString(R.string.direction_arrive)
        ));
        assertEquals("", nearDestinationState.routeStatus.guidance.afterNextLine);
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 111.0)),
                context
        );

        assertTrue(state.routeStatus.guidance.nextLine.contains("45 s"));
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 222.0)),
                context
        );

        assertTrue(state.routeStatus.guidance.nextLine.contains("45 s"));
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 111.0)),
                context
        );

        assertTrue(state.routeStatus.guidance.nextLine.contains("--"));
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 111.0)),
                context
        );

        assertTrue(state.routeStatus.progress.destinationLine.contains(DESTINATION));
        assertTrue(state.routeStatus.progress.destinationLine.contains(DISTANCE_111_METERS));
        assertTrue(state.routeStatus.progress.destinationLine.contains("26 s"));
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                        new NavTarget(STOP_1, 111.0),
                        new NavTarget(DESTINATION, 222.0)
                ),
                context
        );

        assertTrue(state.routeStatus.progress.stopProgressBlock.contains(STOP_1));
        assertTrue(state.routeStatus.progress.stopProgressBlock.contains(DISTANCE_111_METERS));
        assertTrue(state.routeStatus.progress.stopProgressBlock.contains("26 s"));
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
        NavigationLocation movingLocation = locationAt(48.2000, 16.3600);
        movingLocation.setSpeed(2.5f);

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 130.0)),
                context
        );

        assertNotNull(state.routeStatus.compassState);
        assertEquals(90.0f, state.routeStatus.compassState.displayMode.headingDegrees, 0.01f);
        assertEquals(8.0f, state.routeStatus.compassState.displayMode.headingAccuracyDegrees, 0.01f);
        assertTrue(state.routeStatus.compassState.routePoints.size() >= 2);
        assertTrue(state.routeStatus.compassState.radiusState.visibleRadiusMeters >= 90f);
        assertEquals(
                state.routeStatus.compassState.radiusState.movingScaleVisibleRadiusMeters / 30f,
                state.routeStatus.compassState.displayMode.referenceSpeedMps,
                0.01f
        );
        assertEquals(30f, state.routeStatus.compassState.displayMode.movingScaleHorizonSeconds, 0.01f);
        assertEquals(NavigationSpeedBucket.LOW, state.routeStatus.compassState.displayMode.movingScaleSpeedBucket);
        assertTrue(state.routeStatus.compassState.displayMode.movingScaleActive);
        assertEquals(13f, state.routeStatus.compassState.radiusState.routeThresholdMeters, 0.01f);
        assertTrue(state.routeStatus.compassState.passedRoutePoints.size() >= 1);
    }

    @Test
    public void from_keepsStationaryOrientationCueInCompassState() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.emptyList(),
                60.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);
        NavigationLocation currentLocation = locationAt(0.0, 0.0);
        NavCompassStateInput compassInput = NavCompassStateInput.builder(route, index, currentLocation)
                .routeProgress(0.0)
                .motion(0f, true, 5f)
                .heading(0.0, null)
                .orientationCue(new CompassOrientationCue(90f))
                .nowMs(0L)
                .build();

        NavState state = NavStateComposer.from(NavStateBuildInput.builder(context, route, index, currentLocation)
                .routeProgress(0.0, -1, 0)
                .motion(0f, 0f, true, 5f, 5f)
                .heading(0.0, null)
                .compass(compassInput)
                .targets(Collections.singletonList(new NavTarget(DESTINATION, 111.0)))
                .build());

        assertNotNull(state.routeStatus.compassState);
        assertNotNull(state.routeStatus.compassState.orientationCue);
        assertEquals(90f, state.routeStatus.compassState.orientationCue.targetHeadingDegrees, 0.01f);
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
        NavigationLocation movingLocation = locationAt(48.2000, 16.3600);
        movingLocation.setSpeed(2.5f);

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                false,
                Collections.singletonList(new NavTarget(DESTINATION, 130.0)),
                context
        );

        assertNotNull(state.routeStatus.compassState);
        assertEquals(9f, state.routeStatus.compassState.radiusState.accuracyRadiusMeters, 0.01f);
        assertEquals(17f, state.routeStatus.compassState.radiusState.routeThresholdMeters, 0.01f);
        assertEquals(17f, state.routeStatus.compassState.progressLabels.destinationReachedRadiusMeters, 0.01f);
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 333.0)),
                context
        );

        assertNotNull(state.routeStatus.compassState);
        assertTrue(state.routeStatus.compassState.passedRoutePoints.size() >= 2);
        assertTrue(state.routeStatus.compassState.routePoints.size() >= 2);
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 999.0)),
                context
        );

        assertNotNull(state.routeStatus.compassState);
        assertTrue(state.routeStatus.compassState.radiusState.visibleRadiusMeters > 1_000f);
        assertTrue(state.routeStatus.compassState.progressLabels.destinationWithinRadius);
        assertFalse(state.routeStatus.compassState.displayMode.movingScaleActive);
        assertEquals(13f, state.routeStatus.compassState.radiusState.routeThresholdMeters, 0.01f);
        assertTrue(state.routeStatus.compassState.routePoints.size() >= 4);
    }

    @Test
    public void from_whenMovingUsesSmoothedMovingScaleRadiusWithoutUpperCap() {
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

        NavState stationaryState = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 9_999.0)),
                context
        );

        NavigationLocation movingLocation = locationAt(0.0, 0.0);
        movingLocation.setSpeed(20f);
        NavState movingState = from(
                route,
                new PolylineIndex(route.track),
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
                stationaryState.routeStatus.compassState.radiusState.visibleRadiusMeters,
                null,
                1_000L,
                NavState.NO_DEADLINE,
                1_000L,
                Collections.singletonList(new NavTarget(DESTINATION, 9_999.0)),
                context
        );

        assertNotNull(movingState.routeStatus.compassState);
        assertTrue(movingState.routeStatus.compassState.radiusState.visibleRadiusMeters > 600f);
        assertTrue(movingState.routeStatus.compassState.radiusState.visibleRadiusMeters
                > movingState.routeStatus.compassState.radiusState.movingScaleVisibleRadiusMeters);
        assertTrue(movingState.routeStatus.compassState.radiusState.visibleRadiusMeters < stationaryState.routeStatus.compassState.radiusState.visibleRadiusMeters);
        assertFalse(movingState.routeStatus.compassState.progressLabels.destinationWithinRadius);
        assertEquals(
                movingState.routeStatus.compassState.radiusState.movingScaleVisibleRadiusMeters / 45f,
                movingState.routeStatus.compassState.displayMode.referenceSpeedMps,
                0.01f
        );
        assertEquals(45f, movingState.routeStatus.compassState.displayMode.movingScaleHorizonSeconds, 0.01f);
        assertEquals(
                NavigationSpeedBucket.MEDIUM,
                movingState.routeStatus.compassState.displayMode.movingScaleSpeedBucket
        );
    }

    @Test
    public void smoothVisibleRadiusMeters_contractsTowardMovingScaleQuickly() {
        float resolvedRadiusMeters = vibro.navigator.nav.compass.NavCompassStateFactory.smoothVisibleRadiusMeters(300f, 2_000f, 1_000L);

        assertTrue(resolvedRadiusMeters > 300f);
        assertTrue(resolvedRadiusMeters < 550f);
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 666.0)),
                context
        );

        assertNotNull(state.routeStatus.compassState);
        assertEquals(1.0f, state.routeStatus.compassState.displayMode.referenceSpeedMps, 0.01f);
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

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 2_222.0)),
                context
        );

        assertNotNull(state.routeStatus.compassState);
        assertEquals(240f, state.routeStatus.compassState.radiusState.visibleRadiusMeters, 0.01f);
        assertEquals(8.0f, state.routeStatus.compassState.displayMode.referenceSpeedMps, 0.01f);
        assertEquals(NavigationSpeedBucket.LOW, state.routeStatus.compassState.displayMode.movingScaleSpeedBucket);
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
        NavigationLocation location = locationAt(48.2000, 16.3600);
        location.setAltitude(245.4d);
        location.setBearing(182.2f);
        location.setBearingAccuracyDegrees(9.4f);

        NavState state = from(
                route,
                new PolylineIndex(route.track),
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
                Collections.singletonList(new NavTarget(DESTINATION, 140.0)),
                context
        );

        assertEquals("16 km/h ↑245 m • ±5 m • (7)", state.gpsStatus.statusLine);
        assertEquals("#0", state.gpsStatus.telemetry.acquiredFixCountText);
        assertEquals("182°", state.gpsStatus.telemetry.bearingText);
        assertEquals("9°", state.gpsStatus.telemetry.bearingAccuracyText);
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

        NavState baseState = from(
                route,
                new PolylineIndex(route.track),
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
                        new NavTarget(STOP_1, 111.0),
                        new NavTarget(DESTINATION, 222.0)
                ),
                context
        );

        NavState state = NavStateComposer.withNotice(baseState, "Rerouting around blockage");

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

        NavState baseState = from(
                route,
                new PolylineIndex(route.track),
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
                        new NavTarget(STOP_1, 111.0),
                        new NavTarget(DESTINATION, 222.0)
                ),
                context
        );

        assertEquals(
                baseState.routeStatus.progress.destinationLine + "\n" + baseState.routeStatus.progress.stopProgressBlock,
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

        NavState baseState = from(
                route,
                new PolylineIndex(route.track),
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
                        new NavTarget(STOP_1, 111.0),
                        new NavTarget(DESTINATION, 222.0)
                ),
                context
        );

        NavState state = NavStateResourceComposer.withPauseState(context, baseState, true);

        assertEquals(context.getString(R.string.nav_paused_notice), state.displayStatusBlock());
    }

    @NonNull
    private static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            int currentSegmentIndex,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @NonNull NavigationLocation currentLocation,
            Integer fixedSatelliteCount,
            Double headingDegrees,
            Float headingAccuracyDegrees,
            Float previousCompassVisibleRadiusMeters,
            Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull NavigationTextResources context
    ) {
        return from(
                route,
                index,
                alongTrackMeters,
                nextHintIdx,
                currentSegmentIndex,
                speedMps,
                speedMps,
                likelyStationary,
                accuracyMeters,
                accuracyMeters,
                currentLocation,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                null,
                null,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                false,
                targets,
                context
        );
    }

    @NonNull
    private static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            int currentSegmentIndex,
            float speedMps,
            float etaSpeedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @NonNull NavigationLocation currentLocation,
            Integer fixedSatelliteCount,
            Double headingDegrees,
            Float headingAccuracyDegrees,
            Float previousCompassVisibleRadiusMeters,
            Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull NavigationTextResources context
    ) {
        return from(
                route,
                index,
                alongTrackMeters,
                nextHintIdx,
                currentSegmentIndex,
                speedMps,
                etaSpeedMps,
                likelyStationary,
                accuracyMeters,
                accuracyMeters,
                currentLocation,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                null,
                null,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                false,
                targets,
                context
        );
    }

    @NonNull
    private static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            int currentSegmentIndex,
            float speedMps,
            float etaSpeedMps,
            boolean likelyStationary,
            float accuracyMeters,
            float compassAccuracyMeters,
            @NonNull NavigationLocation currentLocation,
            Integer fixedSatelliteCount,
            Double headingDegrees,
            Float headingAccuracyDegrees,
            Float previousCompassVisibleRadiusMeters,
            Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            CompassRouteGeometry compassRouteGeometry,
            CompassRadiusTransition compassRadiusTransition,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            boolean destinationReached,
            @NonNull List<NavTarget> targets,
            @NonNull NavigationTextResources context
    ) {
        NavCompassStateInput compassInput = NavCompassStateInput.builder(route, index, currentLocation)
                .routeProgress(alongTrackMeters)
                .motion(speedMps, likelyStationary, compassAccuracyMeters)
                .heading(headingDegrees, headingAccuracyDegrees)
                .radiusMemory(
                        previousCompassVisibleRadiusMeters,
                        previousReliableMovingCompassVisibleRadiusMeters,
                        compassRadiusUpdateDeltaMs
                )
                .geometry(compassRouteGeometry, compassRadiusTransition)
                .nowMs(nowMs)
                .build();
        return NavStateComposer.from(NavStateBuildInput.builder(context, route, index, currentLocation)
                .routeProgress(alongTrackMeters, nextHintIdx, currentSegmentIndex)
                .motion(speedMps, etaSpeedMps, likelyStationary, accuracyMeters, compassAccuracyMeters)
                .gps(fixedSatelliteCount)
                .heading(headingDegrees, headingAccuracyDegrees)
                .compass(compassInput)
                .timing(nextEvaluationDeadlineElapsedMs, nowMs)
                .destinationReached(destinationReached)
                .targets(targets)
                .build());
    }

    @NonNull
    private static NavigationLocation locationAt(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("test");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(1L);
        return location;
    }
}

