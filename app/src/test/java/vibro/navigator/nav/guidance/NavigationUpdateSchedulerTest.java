package vibro.navigator.nav.guidance;

import static org.junit.Assert.assertEquals;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NavigationUpdateSchedulerTest {

    private final NavigationUpdateScheduler scheduler = new NavigationUpdateScheduler();

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

        assertEquals(1000L, intervalMs);
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

        assertEquals(1000L, intervalMs);
    }

    @Test
    public void suggestUpdateInterval_usesRouteEndWhenNoNextHintRemains() {
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

        assertEquals(30000L, intervalMs);
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
        assertEquals(1000L, NavigationUpdateScheduler.bucketInterval(500L));
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

    private static GeoJsonRoute route(List<LatLon> track, List<VoiceHint> voiceHints) {
        return new GeoJsonRoute(track, voiceHints, 120.0, 111.0);
    }
}
