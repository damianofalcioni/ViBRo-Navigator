package vibro.navigator.intent;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class IncomingLocationPlacementTest {
    private static final String DESTINATION = "Destination";

    @Test
    public void blankDestinationTakesPriorityOverStops() {
        assertEquals(-1, IncomingLocationPlacement.targetStopIndex("", Arrays.asList("Stop", "")));
        assertEquals(-1, IncomingLocationPlacement.targetStopIndex(" \t ", Collections.emptyList()));
    }

    @Test
    public void firstEmptyStopWinsInDisplayedOrder() {
        assertEquals(1, IncomingLocationPlacement.targetStopIndex(DESTINATION,
                Arrays.asList("Stop 1", "", "", "Stop 4")));
        assertEquals(0, IncomingLocationPlacement.targetStopIndex(DESTINATION,
                Arrays.asList(" \t ", "Stop 2", "")));
    }

    @Test
    public void allFilledStopsRequireAppending() {
        assertEquals(0, IncomingLocationPlacement.targetStopIndex(DESTINATION, Collections.emptyList()));
        assertEquals(2, IncomingLocationPlacement.targetStopIndex(DESTINATION,
                Arrays.asList("Stop 1", "Stop 2")));
    }
}
