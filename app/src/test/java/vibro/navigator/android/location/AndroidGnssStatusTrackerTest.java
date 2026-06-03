package vibro.navigator.android.location;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AndroidGnssStatusTrackerTest {

    @Test
    public void countSatellitesUsedInFix_countsOnlyTrueFlags() {
        assertEquals(3, AndroidGnssStatusTracker.countSatellitesUsedInFix(
                true,
                false,
                true,
                false,
                true
        ));
    }
}
