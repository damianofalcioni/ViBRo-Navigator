package vibro.navigator.nav.location;

import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyNetworkBackoffTest {
    private static final String GPS = "gps";
    private static final String NETWORK = "network";
    private final LegacyLocationAcquisitionPolicy policy = new LegacyLocationAcquisitionPolicy();

    @Test
    public void changingCoarseNetworkRemainsFastWhenItIsTheBetterSource() {
        for (int i = 0; i < 3; i++) {
            long time = 1_000 + i * 3_000L;
            policy.record(fix(GPS, time, 200), time, 3_000);
            NavigationLocation network = fix(NETWORK, time, 60);
            network.setLatitude(i * 5 / 111_320.0);
            policy.record(network, time, 3_000);
        }
        assertEquals(3_000, policy.networkIntervalMs(7_000, 3_000));
        policy.record(fix(GPS, 10_000, 40), 10_000, 3_000);
        assertEquals(30_000, policy.networkIntervalMs(10_000, 3_000));
        policy.record(fix(GPS, 13_000, 200), 13_000, 3_000);
        assertEquals(3_000, policy.networkIntervalMs(13_000, 3_000));
    }

    @Test
    public void usefulNetworkOverridesRetainedGnssStabilityOnTheFirstPoorFix() {
        for (long time : new long[] {1_000, 6_000, 11_000}) {
            policy.record(fix(GPS, time, 20), time, 3_000);
        }
        policy.record(fix(NETWORK, 12_000, 60), 12_000, 3_000);
        assertEquals(30_000, policy.networkIntervalMs(12_000, 3_000));
        policy.record(fix(GPS, 14_000, 200), 14_000, 3_000);
        assertEquals(3_000, policy.networkIntervalMs(14_000, 3_000));
    }

    @Test
    public void coarseNetworkBacksOffWithLivePoorGpsAndUsefulNetworkRestoresSampling() {
        policy.record(fix(GPS, 1_000, 60), 1_000, 3_000);
        for (int i = 1; i <= 3; i++) {
            long time = 1_000 + i * 3_000L;
            policy.record(fix(NETWORK, time, 100), time, 3_000);
        }
        assertEquals(30_000, policy.networkIntervalMs(10_000, 3_000));
        assertEquals(16_000, policy.recoveryDeadlineMs(3_000));
        policy.record(fix(NETWORK, 11_000, 25), 11_000, 3_000);
        assertEquals(3_000, policy.networkIntervalMs(11_000, 3_000));
    }

    @Test
    public void missingSilentOrInvalidGpsCannotLeaveCoarseNetworkThrottled() {
        for (int i = 1; i <= 3; i++) {
            policy.record(fix(NETWORK, i * 3_000L, 100), i * 3_000L, 3_000);
        }
        assertEquals(3_000, policy.networkIntervalMs(10_000, 3_000));
        policy.record(fix(GPS, 10_000, 60), 10_000, 3_000);
        assertEquals(30_000, policy.networkIntervalMs(10_000, 3_000));
        assertEquals(3_000, policy.networkIntervalMs(25_000, 3_000));
        policy.record(fix(GPS, 26_000, Float.NaN), 26_000, 3_000);
        assertEquals(3_000, policy.networkIntervalMs(26_000, 3_000));
    }

    @Test
    public void repeatedAccurateStationaryNetworkLocationsAreUsefulAndResetClearsBackoff() {
        policy.record(fix(GPS, 1_000, 60), 1_000, 3_000);
        for (int i = 1; i <= 4; i++) {
            policy.record(fix(NETWORK, i * 3_000L, 20), i * 3_000L, 3_000);
        }
        assertEquals(3_000, policy.networkIntervalMs(12_000, 3_000));
        policy.reset();
        assertEquals(3_000, policy.networkIntervalMs(13_000, 3_000));
    }

    private static NavigationLocation fix(String provider, long time, float accuracy) {
        NavigationLocation fix = new NavigationLocation(provider);
        fix.setTime(time, time);
        fix.setAccuracy(accuracy);
        return fix;
    }
}
