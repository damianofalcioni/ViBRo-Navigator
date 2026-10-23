package vibro.navigator.nav.session;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class NavigationDepartureMonitorTest {
    @Test
    public void stopAndUncertainDepartureStayFastUntilThreeForwardObservationsThenRamp() {
        NavigationDepartureMonitor monitor = new NavigationDepartureMonitor();
        assertEquals(3_000, monitor.interval(60_000, true, false, 1_000));
        assertEquals(3_000, monitor.interval(60_000, false, false, 4_000));
        assertEquals(3_000, monitor.interval(60_000, false, true, 7_000));
        assertEquals(3_000, monitor.interval(60_000, false, true, 7_100));
        assertEquals(3_000, monitor.interval(60_000, false, true, 10_000));
        assertEquals(3_000, monitor.interval(60_000, false, true, 13_000));
        assertEquals(5_000, monitor.interval(60_000, false, true, 16_000));
        assertEquals(8_000, monitor.interval(60_000, false, true, 21_000));
        assertEquals(3_000, monitor.interval(60_000, true, false, 24_000));
    }

    @Test
    public void gapsAndBackwardProgressCannotCompleteDepartureAndFasterRequestsArePreserved() {
        NavigationDepartureMonitor monitor = new NavigationDepartureMonitor();
        monitor.interval(60_000, false, true, 1_000);
        monitor.interval(60_000, false, true, 20_000);
        assertEquals(3_000, monitor.interval(60_000, false, true, 23_000));
        assertEquals(3_000, monitor.interval(60_000, false, false, 26_000));
        assertEquals(1_000, monitor.interval(1_000, true, false, 27_000));
        assertEquals(-1, monitor.interval(-1, true, false, 28_000));
    }
}
