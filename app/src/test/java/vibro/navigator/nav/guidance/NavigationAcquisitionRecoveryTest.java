package vibro.navigator.nav.guidance;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class NavigationAcquisitionRecoveryTest {
    private final NavigationAcquisitionRecovery recovery = new NavigationAcquisitionRecovery();

    @Test
    public void reliableLongLegObservationsRefreshTheGapAnchorBeforeApproachTightening() {
        assertEquals(30_000, observe(30_000, 1_000));
        assertEquals(30_000, observe(60_000, 31_000));
        assertEquals(30_000, observe(60_000, 61_000));
        assertEquals(60_000, observe(60_000, 91_000));
        assertEquals(60_000, observe(60_000, 151_000));
        assertEquals(60_000, observe(60_000, 211_000));
        assertEquals(30_000, observe(30_000, 271_000));
        assertEquals(20_000, observe(20_000, 301_000));
    }

    @Test
    public void expectedSparseGapUsesThePreviousCapWhenTheNewObservationTightensIt() {
        assertEquals(30_000, observe(30_000, 1_000));
        assertEquals(20_000, observe(20_000, 31_000));
        assertEquals(12_000, observe(12_000, 51_000));
    }

    @Test
    public void alternatingQualityCannotRestoreLongIntervalsAfterOneGoodFix() {
        assertEquals(30_000, observe(30_000, 1_000));
        assertEquals(3_000, observe(3_000, 4_000));
        assertEquals(3_000, observe(30_000, 7_000));
        assertEquals(3_000, observe(3_000, 10_000));
        assertEquals(3_000, observe(30_000, 13_000));
    }

    @Test
    public void independentRecoveryRestoresBucketsOnlyAfterTheirIntervalsHaveElapsed() {
        observe(3_000, 1_000);
        assertEquals(3_000, observe(60_000, 4_000));
        assertEquals(3_000, observe(60_000, 7_000));
        assertEquals(5_000, observe(60_000, 10_000));
        assertEquals(5_000, observe(60_000, 13_000));
        assertEquals(8_000, observe(60_000, 16_000));
        assertEquals(12_000, observe(60_000, 24_000));
        assertEquals(20_000, observe(60_000, 36_000));
        assertEquals(30_000, observe(60_000, 56_000));
        assertEquals(60_000, observe(60_000, 86_000));
    }

    @Test
    public void duplicateClusteredStaleAndChangedProviderFixesCannotCompleteRecovery() {
        observe(3_000, 1_000);
        assertEquals(3_000, observe(60_000, 4_000));
        assertEquals(3_000, observe(60_000, 4_000));
        assertEquals(3_000, observe(60_000, 4_001));
        assertEquals(3_000, recovery.limit(60_000, fix("gps", 4_000), 20_000));
        assertEquals(3_000, recovery.limit(60_000, fix("network", 21_000), 21_000));
        assertEquals(3_000, recovery.limit(60_000, fix("gps", 24_000), 24_000));
        assertEquals(3_000, observe(60_000, 27_000));
        assertEquals(5_000, observe(60_000, 30_000));
    }

    @Test
    public void resetDiscardsAnEarlierVerificationWindowAndReliableLongLegsStaySparse() {
        observe(3_000, 1_000);
        recovery.reset();
        assertEquals(60_000, observe(60_000, 31_000));
    }

    private long observe(long cap, long time) {
        return recovery.limit(cap, fix("gps", time), time);
    }

    private static NavigationLocation fix(String provider, long time) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setTime(time, time);
        return location;
    }
}
