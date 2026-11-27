package vibro.navigator.nav.simulation;

import org.junit.Test;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NavigationSimulationInputTest {
    @Test
    public void acceptsLocalizedSpeedAndConvertsImperialUnits() {
        assertEquals(5f, NavigationSimulationInput.parseSpeed("18,0", false, Locale.ITALIAN), 0.001);
        assertEquals(10f, NavigationSimulationInput.parseSpeed("22.36936", true, Locale.US), 0.001);
        assertEquals("18", NavigationSimulationInput.formatSpeed(5, false, Locale.ITALIAN));
        assertEquals(5f, NavigationSimulationInput.parseSpeed("18.0", false, Locale.ITALIAN), 0.001);
    }

    @Test
    public void rejectsInvalidOutOfRangeAndTrailingSpeedInput() {
        assertNull(NavigationSimulationInput.parseSpeed("18 km/h", false, Locale.US));
        assertNull(NavigationSimulationInput.parseSpeed("-1", false, Locale.US));
        assertNull(NavigationSimulationInput.parseSpeed("301", false, Locale.US));
        assertNull(NavigationSimulationInput.parseSpeed("NaN", false, Locale.US));
        assertNull(NavigationSimulationInput.parseSpeed("", false, Locale.US));
        assertEquals(0f, NavigationSimulationInput.parseSpeed("0", false, Locale.US), 0);
    }

    @Test
    public void coordinateInputIsStrictAndSupportsNegativeAndBoundaryCoordinates() {
        assertEquals(-90, NavigationSimulationConfig.parsePosition(" -90, +180 ").lat, 0);
        assertNull(NavigationSimulationConfig.parsePosition(""));
        assertNull(NavigationSimulationConfig.parsePosition("91, 2"));
        assertNull(NavigationSimulationConfig.parsePosition("48, 181"));
        assertNull(NavigationSimulationConfig.parsePosition("x48, 16"));
        assertNull(NavigationSimulationConfig.parsePosition("--48, 16"));
        assertNull(NavigationSimulationConfig.parsePosition("48, 16 trailing"));
        assertEquals(-16.2, NavigationSimulationConfig.parsePosition("48.2, -16.2").lon, 0);
    }
}
