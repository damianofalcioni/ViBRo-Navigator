package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;

import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;

import org.junit.Test;

import java.util.Arrays;

public class RouteTimeEstimatorTest {

    @Test
    public void estimateSecondsToTrackPoint_usesBrouterTimeForCurrentSegmentWhenLiveSpeedUnavailable() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Arrays.asList(),
                Arrays.asList(0.0, 45.0),
                45.0,
                111.0
        );

        Double seconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                route,
                new PolylineIndex(route.track),
                0.0,
                0,
                1,
                0f
        );

        assertEquals(45.0, seconds, 0.01);
    }

    @Test
    public void estimateSecondsToTrackPoint_combinesLiveCurrentSegmentWithBrouterLaterSegments() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.0001),
                        new LatLon(0.0, 0.001)
                ),
                Arrays.asList(),
                Arrays.asList(0.0, 10.0, 30.0),
                30.0,
                111.0
        );

        Double seconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                route,
                new PolylineIndex(route.track),
                0.0,
                0,
                2,
                2f
        );

        assertEquals(25.56, seconds, 0.25);
    }
}
