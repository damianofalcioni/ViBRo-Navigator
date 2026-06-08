package vibro.navigator.geo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void distanceMeters_antipodalCoordinates_staysFinite() {
        double distanceMeters = GeoMath.distanceMeters(0.0, 0.0, 0.0, 180.0);

        assertTrue(Double.isFinite(distanceMeters));
        assertEquals(20_015_000.0, distanceMeters, 2_000.0);
    }

    @Test
    public void latLonValidation_rejectsNonFiniteAndOutOfRangeCoordinates() {
        assertTrue(LatLon.isValidCoordinate(48.2082, 16.3738));
        assertFalse(LatLon.isValidCoordinate(Double.POSITIVE_INFINITY, 16.3738));
        assertFalse(LatLon.isValidCoordinate(91.0, 16.3738));
        assertFalse(LatLon.isValidCoordinate(48.2082, -181.0));
    }
}

