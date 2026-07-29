package vibro.navigator.nav.compass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.policy.NavigationSpeedBucket;

public class CompassRadiusResolverTest {
    private static final String GPS_PROVIDER = "gps";

    @Test
    public void resolve_ignoresRawSpeedWhenResolvedSpeedIsZero() {
        NavigationLocation noisySpeedLocation = new NavigationLocation(GPS_PROVIDER);
        noisySpeedLocation.setLatitude(0.0);
        noisySpeedLocation.setLongitude(0.0);
        noisySpeedLocation.setSpeed(1.2f);

        CompassRadiusResolver.State state = CompassRadiusResolver.resolve(
                1_000.0,
                noisySpeedLocation,
                0f,
                false,
                null,
                null,
                0L,
                null,
                0L
        );

        assertFalse(state.usingMovingScale);
        assertTrue(state.visibleRadiusMeters > 1_000f);
    }

    @Test
    public void resolve_whenStationaryFullRouteZoomDisabledReusesPreviousMovingRadius() {
        NavigationLocation stationaryLocation = movingLocation(0f);

        CompassRadiusResolver.State state = CompassRadiusResolver.resolve(
                2_000.0,
                stationaryLocation,
                0f,
                true,
                false,
                NavigationSpeedBucket.LOW,
                1_200f,
                240f,
                1_000L,
                null,
                2_000L
        );

        assertTrue(state.usingMovingScale);
        assertEquals(240f, state.visibleRadiusMeters, 0.01f);
        assertEquals(240f, state.movingScaleVisibleRadiusMeters, 0.01f);
        assertTrue(state.visibleRadiusMeters < state.fullRouteVisibleRadiusMeters);
    }

    @Test
    public void resolve_whenStationaryFullRouteZoomDisabledWithoutMovingRadiusFallsBackToFullRoute() {
        NavigationLocation stationaryLocation = movingLocation(0f);

        CompassRadiusResolver.State state = CompassRadiusResolver.resolve(
                2_000.0,
                stationaryLocation,
                0f,
                true,
                false,
                null,
                null,
                0L,
                null,
                0L
        );

        assertFalse(state.usingMovingScale);
        assertEquals(state.fullRouteVisibleRadiusMeters, state.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void resolve_usesAdaptiveMovingScaleHorizonForInitialBuckets() {
        assertMovingHorizon(39.9f, null, NavigationSpeedBucket.LOW, 30f);
        assertMovingHorizon(40f, null, NavigationSpeedBucket.MEDIUM, 45f);
        assertMovingHorizon(80f, null, NavigationSpeedBucket.HIGH, 60f);
    }

    @Test
    public void resolve_keepsMovingScaleBucketAroundHysteresisBoundaries() {
        assertMovingHorizon(42.9f, NavigationSpeedBucket.LOW, NavigationSpeedBucket.LOW, 30f);
        assertMovingHorizon(43f, NavigationSpeedBucket.LOW, NavigationSpeedBucket.MEDIUM, 45f);
        assertMovingHorizon(83.9f, NavigationSpeedBucket.MEDIUM, NavigationSpeedBucket.MEDIUM, 45f);
        assertMovingHorizon(84f, NavigationSpeedBucket.MEDIUM, NavigationSpeedBucket.HIGH, 60f);
    }

    @Test
    public void resolve_replacesRememberedRadiusWhenCurrentMovingSpeedIsReliable() {
        float speedMps = 20f;
        NavigationLocation location = movingLocation(speedMps);

        CompassRadiusResolver.State state = CompassRadiusResolver.resolve(
                5_000.0,
                location,
                speedMps,
                false,
                NavigationSpeedBucket.LOW,
                90f,
                90f,
                1_000L,
                null,
                1_000L
        );

        assertEquals(NavigationSpeedBucket.MEDIUM, state.movingScaleSpeedBucket);
        assertEquals(900f, state.movingScaleVisibleRadiusMeters, 0.01f);
    }

    private static void assertMovingHorizon(
            float speedKmh,
            NavigationSpeedBucket previousBucket,
            NavigationSpeedBucket expectedBucket,
            float expectedHorizonSeconds
    ) {
        float speedMps = speedKmh / 3.6f;
        NavigationLocation location = movingLocation(speedMps);

        CompassRadiusResolver.State state = CompassRadiusResolver.resolve(
                2_000.0,
                location,
                speedMps,
                false,
                previousBucket,
                null,
                null,
                0L,
                null,
                0L
        );

        assertEquals(expectedBucket, state.movingScaleSpeedBucket);
        assertEquals(expectedHorizonSeconds, state.movingScaleHorizonSeconds, 0.01f);
        assertEquals(
                Math.max(90f, speedMps * expectedHorizonSeconds),
                state.movingScaleVisibleRadiusMeters,
                0.01f
        );
    }

    private static NavigationLocation movingLocation(float speedMps) {
        NavigationLocation location = new NavigationLocation(GPS_PROVIDER);
        location.setLatitude(0.0);
        location.setLongitude(0.0);
        location.setSpeed(speedMps);
        return location;
    }
}
