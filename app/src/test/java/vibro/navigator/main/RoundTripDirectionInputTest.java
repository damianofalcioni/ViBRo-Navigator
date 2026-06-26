package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RoundTripDirectionInputTest {
    @Test
    public void parseDirectionDegrees_acceptsOnlyCompassBearingRange() {
        assertEquals(Integer.valueOf(0), RoundTripDirectionInput.parseDirectionDegrees("0"));
        assertEquals(Integer.valueOf(359), RoundTripDirectionInput.parseDirectionDegrees("359"));
        assertEquals(Integer.valueOf(42), RoundTripDirectionInput.parseDirectionDegrees("42\u00b0"));
        assertNull(RoundTripDirectionInput.parseDirectionDegrees(""));
        assertNull(RoundTripDirectionInput.parseDirectionDegrees("-1"));
        assertNull(RoundTripDirectionInput.parseDirectionDegrees("360"));
        assertNull(RoundTripDirectionInput.parseDirectionDegrees("12.5"));
        assertNull(RoundTripDirectionInput.parseDirectionDegrees("north"));
    }

    @Test
    public void formatHeadingDegrees_roundsAndNormalizesSensorHeading() {
        assertEquals("0", RoundTripDirectionInput.formatHeadingDegrees(359.6));
        assertEquals("45", RoundTripDirectionInput.formatHeadingDegrees(44.6));
        assertEquals("315", RoundTripDirectionInput.formatHeadingDegrees(-45.0));
    }
}
