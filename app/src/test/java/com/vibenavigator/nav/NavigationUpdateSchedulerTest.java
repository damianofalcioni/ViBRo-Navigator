package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;

import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NavigationUpdateSchedulerTest {

    private final NavigationUpdateScheduler scheduler = new NavigationUpdateScheduler();

    @Test
    public void suggestUpdateInterval_keepsFastPollingDuringWarmupWindow() {
        long intervalMs = scheduler.suggestUpdateInterval(
                1_000L,
                2_000L,
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                new PolylineIndex(Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001))),
                0,
                0.0,
                5f
        );

        assertEquals(1000L, intervalMs);
    }

    @Test
    public void suggestUpdateInterval_scalesWithDistanceToNextHintAfterWarmup() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.001),
                new LatLon(0.0, 0.002)
        ));
        long intervalMs = scheduler.suggestUpdateInterval(
                5_000L,
                1_000L,
                Collections.singletonList(new VoiceHint(2, 0, 0, 0.0, 0)),
                index,
                0,
                0.0,
                10f
        );

        assertEquals(5559L, intervalMs);
    }

    @Test
    public void suggestUpdateInterval_usesMinimumValueForVeryImminentHint() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.001)
        ));
        long intervalMs = scheduler.suggestUpdateInterval(
                5_000L,
                1_000L,
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                index,
                0,
                40.0,
                10f
        );

        assertEquals(1000L, intervalMs);
    }

    @Test
    public void suggestUpdateInterval_clampsToMaxValue() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.01)
        ));
        long intervalMs = scheduler.suggestUpdateInterval(
                10_000L,
                1_000L,
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                index,
                0,
                0.0,
                1f
        );

        assertEquals(60000L, intervalMs);
    }
}
