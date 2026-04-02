package com.vibenavigator.nav.route;

import com.vibenavigator.geo.LatLon;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PolylineIndexTest {

    @Test
    public void pointAtDistance_beforeStart_returnsFirstPoint() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.001)
        ));

        LatLon point = index.pointAtDistance(-5.0);

        assertNotNull(point);
        assertEquals(0.0, point.lat, 0.0);
        assertEquals(0.0, point.lon, 0.0);
    }

    @Test
    public void pointAtDistance_insideSegment_interpolatesPoint() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.001)
        ));

        LatLon point = index.pointAtDistance(55.0);

        assertNotNull(point);
        assertEquals(0.0, point.lat, 1e-6);
        assertEquals(0.000495, point.lon, 0.00005);
    }

    @Test
    public void pointAtDistance_afterEnd_returnsLastPoint() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.001)
        ));

        LatLon point = index.pointAtDistance(500.0);

        assertNotNull(point);
        assertEquals(0.0, point.lat, 0.0);
        assertEquals(0.001, point.lon, 0.0);
    }
}
