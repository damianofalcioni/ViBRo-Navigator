package vibro.navigator.nav.location;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NavigationGnssStatusTrackerTest {

    @Test
    public void countSatellitesUsedInFix_countsOnlyTrueFlags() {
        assertEquals(3, NavigationGnssStatusTracker.countSatellitesUsedInFix(
                true,
                false,
                true,
                false,
                true
        ));
    }
}
