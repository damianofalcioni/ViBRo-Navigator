package vibro.navigator.nav.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class NavigationInstructionMetricCycleTest {
    private static final String DISTANCE_PREFIX = "↰ 20m";
    @Test
    public void cyclesDistanceAndTimeWithoutRestartingForCountdownUpdates() {
        NavigationInstructionMetricCycle cycle = new NavigationInstructionMetricCycle();
        NavigationInstructionParts parts = NavigationInstructionParts.fromLine("↰ 20m - 5s - Turn left");
        assertEquals(DISTANCE_PREFIX, cycle.prefix(parts, 1000L));
        assertEquals(DISTANCE_PREFIX, cycle.prefix(parts, 3999L));
        assertEquals("↰ 5s", cycle.prefix(parts, 4000L));
        parts = NavigationInstructionParts.fromLine("↰ 18m - 4s - Turn left");
        assertEquals("↰ 4s", cycle.prefix(parts, 5000L));
        assertEquals(2000L, cycle.nextSwitchDelayMs(5000L));
        assertEquals("↰ 18m", cycle.prefix(parts, 7000L));
        parts = NavigationInstructionParts.fromLine("↱ 18m - 4s - Turn right");
        assertEquals("↱ 18m", cycle.prefix(parts, 8000L));
        cycle.reset();
        assertEquals("↱ 18m", cycle.prefix(parts, 20000L));
    }

    @Test
    public void preservesUnitsAndInstructionSeparatorsAndHandlesPlainMessages() {
        NavigationInstructionParts parts = NavigationInstructionParts.fromLine("↻3 1.2 km - 2 min - Take exit 3 - towards the station");
        assertEquals("↻3 1.2 km", parts.distancePrefix);
        assertEquals("↻3 2 min", parts.timePrefix);
        assertEquals("Take exit 3 - towards the station", parts.instruction);
        parts = NavigationInstructionParts.fromLine("20m - 5s - Turn left");
        assertEquals("5s", parts.timePrefix);
        parts = NavigationInstructionParts.fromLine("20 m - 5 s - Turn left");
        assertEquals("5 s", parts.timePrefix);
        parts = NavigationInstructionParts.fromLine("Arrive");
        assertEquals("Arrive", parts.instruction);
        assertEquals("", parts.distancePrefix);
        assertFalse(parts.alternatesMetrics());
    }

    @Test
    public void unavailableTimeKeepsDistanceVisible() {
        NavigationInstructionParts parts = NavigationInstructionParts.fromLine("↰ 20m - -- - Turn left");
        assertFalse(parts.alternatesMetrics());
        NavigationInstructionMetricCycle cycle = new NavigationInstructionMetricCycle();
        assertEquals(DISTANCE_PREFIX, cycle.prefix(parts, 1000L));
        assertEquals(DISTANCE_PREFIX, cycle.prefix(parts, 4000L));
    }
}
