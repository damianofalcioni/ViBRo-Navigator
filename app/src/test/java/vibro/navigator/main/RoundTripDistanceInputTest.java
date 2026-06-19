package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RoundTripDistanceInputTest {
    @Test
    public void parseDistanceMeters_usesMetersInMetricMode() {
        assertEquals(Integer.valueOf(15000), RoundTripDistanceInput.parseDistanceMeters("15000", false));
    }

    @Test
    public void parseDistanceMeters_usesMilesInImperialMode() {
        assertEquals(Integer.valueOf(16093), RoundTripDistanceInput.parseDistanceMeters("10", true));
    }

    @Test
    public void parseDistanceMeters_rejectsInvalidValues() {
        assertNull(RoundTripDistanceInput.parseDistanceMeters("", false));
        assertNull(RoundTripDistanceInput.parseDistanceMeters("0", false));
        assertNull(RoundTripDistanceInput.parseDistanceMeters("bad", false));
    }
}
