package com.vibenavigator.geo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeoMathTest {

    @Test
    public void distanceMeters_oneDegreeLongitudeAtEquator_isAbout111km() {
        double d = GeoMath.distanceMeters(0, 0, 0, 1);
        assertEquals(111_000, d, 1500);
    }

    @Test
    public void bearingDegrees_east_is90() {
        double b = GeoMath.bearingDegrees(0, 0, 0, 1);
        assertEquals(90.0, b, 1.0);
    }
}

