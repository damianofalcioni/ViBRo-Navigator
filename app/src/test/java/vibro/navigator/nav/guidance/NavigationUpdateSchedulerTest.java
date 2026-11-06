package vibro.navigator.nav.guidance;

import static org.junit.Assert.assertEquals;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.location.NavigationLocation;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NavigationUpdateSchedulerTest {

    private final NavigationUpdateScheduler scheduler = new NavigationUpdateScheduler();

    @Test
    public void anAcceleratingRoutedApproachRequestsVerificationBeforeTheTurnCrossing() {
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0.01, 0)),
                Collections.singletonList(new VoiceHint(1, 1, 0, 0, 90)), 1100, 1112);
        PolylineIndex index = new PolylineIndex(route.track);
        NavigationTurnState turns = new NavigationTurnState();
        turns.onRouteApplied(route, index, Collections.emptyList(), null, 1, 5);
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(100_000, 100_000);
        location.setSpeed(10);
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        turns.recordLocationQuality(location, 5);
        NavigationTurnState.Progress decision = turns.evaluate(route, index, index.totalLengthMeters() - 100,
                0, 1, Float.NaN, true, 100_000, 0, false);
        assertEquals(3_000, decision.suggestedUpdateIntervalMs);
        assertEquals(0, decision.turnEvents.size());
    }

    @Test
    public void movingQualityRecoveryWaitsForIndependentFixesAndRampsAfterVerification() {
        GeoJsonRoute route = route(Arrays.asList(new LatLon(0, 0), new LatLon(0, 0.01)),
                Collections.emptyList());
        PolylineIndex index = new PolylineIndex(route.track);
        assertEquals(30_000, qualityInterval(route, index, 1_000, 5));
        assertEquals(3_000, qualityInterval(route, index, 4_000, 130));
        assertEquals(3_000, qualityInterval(route, index, 7_000, 5));
        assertEquals(3_000, qualityInterval(route, index, 7_001, 5));
        assertEquals(3_000, qualityInterval(route, index, 10_000, 5));
        assertEquals(5_000, qualityInterval(route, index, 13_000, 5));
        scheduler.resetPostManeuverIntervalRamp();
        assertEquals(30_000, qualityInterval(route, index, 16_000, 5));
    }

    private long qualityInterval(GeoJsonRoute route, PolylineIndex index, long time, float accuracy) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        scheduler.recordLocationQuality(location, accuracy);
        return scheduler.suggestUpdateInterval(time, 0, route, index, null, 300d, 0, 0, 1.5f);
    }

    @Test
    public void suggestUpdateInterval_keepsFastPollingDuringWarmupWindow() {
        GeoJsonRoute route = route(
                Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001)),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0))
        );
        long intervalMs = scheduler.suggestUpdateInterval(
                1_000L,
                2_000L,
                route,
                new PolylineIndex(route.track),
                0,
                0.0,
                0,
                5f
        );

        assertEquals(3000L, intervalMs);
    }

    @Test
    public void suggestUpdateInterval_scalesWithDistanceToNextHintAfterWarmup() {
        GeoJsonRoute route = route(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.singletonList(new VoiceHint(2, 0, 0, 0.0, 0))
        );
        PolylineIndex index = new PolylineIndex(route.track);
        long intervalMs = scheduler.suggestUpdateInterval(
                5_000L,
                1_000L,
                route,
                index,
                0,
                0.0,
                1,
                10f
        );

        assertEquals(5000L, intervalMs);
    }

    @Test
    public void suggestUpdateInterval_usesMinimumValueForVeryImminentHint() {
        GeoJsonRoute route = route(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0))
        );
        PolylineIndex index = new PolylineIndex(route.track);
        long intervalMs = scheduler.suggestUpdateInterval(
                5_000L,
                1_000L,
                route,
                index,
                0,
                40.0,
                0,
                10f
        );

        assertEquals(3000L, intervalMs);
    }

    @Test
    public void suggestUpdateInterval_capsRouteEndChecksWhenArrivalIsWithinGuardWindow() {
        GeoJsonRoute route = route(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.emptyList()
        );
        PolylineIndex index = new PolylineIndex(route.track);
        long intervalMs = scheduler.suggestUpdateInterval(
                5_000L,
                1_000L,
                route,
                index,
                null,
                null,
                0.0,
                0,
                0f
        );

        assertEquals(20000L, intervalMs);
    }

    @Test
    public void suggestUpdateInterval_capsModerateEtaChecksBeforeNextHint() {
        GeoJsonRoute route = route(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0))
        );
        PolylineIndex index = new PolylineIndex(route.track);
        long intervalMs = scheduler.suggestUpdateInterval(
                5_000L,
                1_000L,
                route,
                index,
                0,
                0.0,
                0,
                0.7f
        );

        assertEquals(20000L, intervalMs);
    }

    @Test
    public void suggestUpdateInterval_clampsToMaxValue() {
        GeoJsonRoute route = route(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.01)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0))
        );
        PolylineIndex index = new PolylineIndex(route.track);
        long intervalMs = scheduler.suggestUpdateInterval(
                10_000L,
                1_000L,
                route,
                index,
                0,
                0.0,
                0,
                1f
        );

        assertEquals(60000L, intervalMs);
    }

    @Test
    public void bucketInterval_snapsToNearestBucket() {
        assertEquals(5000L, NavigationUpdateScheduler.bucketInterval(5_559L));
        assertEquals(8000L, NavigationUpdateScheduler.bucketInterval(7_200L));
        assertEquals(12000L, NavigationUpdateScheduler.bucketInterval(11_100L));
    }

    @Test
    public void bucketInterval_clampsBeforeBucketing() {
        assertEquals(3000L, NavigationUpdateScheduler.bucketInterval(500L));
        assertEquals(60000L, NavigationUpdateScheduler.bucketInterval(90_000L));
    }

    @Test
    public void suggestUpdateInterval_usesRouteTimesWhenHintIsAfterCurrentSegment() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.singletonList(new VoiceHint(2, 0, 0, 0.0, 0)),
                Arrays.asList(0.0, 20.0, 40.0),
                40.0,
                222.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        long intervalMs = scheduler.suggestUpdateInterval(
                5_000L,
                1_000L,
                route,
                index,
                0,
                0.0,
                0,
                0f
        );

        assertEquals(8000L, intervalMs);
    }

    @Test
    public void suggestDirectTargetUpdateInterval_usesSameBucketsAsRouteUpdates() {
        long intervalMs = scheduler.suggestDirectTargetUpdateInterval(
                10_000L,
                1_000L,
                111.0
        );

        assertEquals(20000L, intervalMs);
    }

    @Test
    public void suggestDirectTargetUpdateInterval_keepsFastPollingDuringWarmup() {
        long intervalMs = scheduler.suggestDirectTargetUpdateInterval(
                1_000L,
                2_000L,
                111.0
        );

        assertEquals(3000L, intervalMs);
    }

    @Test
    public void applyPostManeuverIntervalRamp_holdsCurrentBucketUntilElapsed() {
        assertEquals(3000L, scheduler.applyPostManeuverIntervalRamp(20_000L, true, 1_000L));
        assertEquals(3000L, scheduler.applyPostManeuverIntervalRamp(20_000L, false, 3_999L));
        assertEquals(5000L, scheduler.applyPostManeuverIntervalRamp(20_000L, false, 4_000L));
        assertEquals(5000L, scheduler.applyPostManeuverIntervalRamp(20_000L, false, 8_999L));
        assertEquals(8000L, scheduler.applyPostManeuverIntervalRamp(20_000L, false, 9_000L));
    }

    private static GeoJsonRoute route(List<LatLon> track, List<VoiceHint> voiceHints) {
        return new GeoJsonRoute(track, voiceHints, 120.0, 111.0);
    }
}
