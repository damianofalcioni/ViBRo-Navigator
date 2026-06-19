package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RoundTripDistanceInputTest {
    @Test
    public void parseDistanceMeters_usesKilometersInMetricMode() {
        assertEquals(Integer.valueOf(15000), RoundTripDistanceInput.parseDistanceMeters("15", false));
    }

    @Test
    public void parseDistanceMeters_usesMilesInImperialMode() {
        assertEquals(Integer.valueOf(16093), RoundTripDistanceInput.parseDistanceMeters("10", true));
    }

    @Test
    public void parseBRouterRadiusMeters_convertsAverageRoundTripDistanceToCircleRadius() {
        assertEquals(Integer.valueOf(2387), RoundTripDistanceInput.parseBRouterRadiusMeters("15", false));
        assertEquals(Integer.valueOf(2561), RoundTripDistanceInput.parseBRouterRadiusMeters("10", true));
    }

    @Test
    public void parseDistanceMeters_rejectsInvalidValues() {
        assertNull(RoundTripDistanceInput.parseDistanceMeters("", false));
        assertNull(RoundTripDistanceInput.parseDistanceMeters("0", false));
        assertNull(RoundTripDistanceInput.parseDistanceMeters("bad", false));
        assertNull(RoundTripDistanceInput.parseBRouterRadiusMeters("bad", false));
    }
}
