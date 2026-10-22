package vibro.navigator.nav.location;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LegacyLocationAcquisitionPolicyTest {
    @Test
    public void stableGpsThrottlesNetworkOnlyAfterThreeFixesAcrossTenSeconds() {
        LegacyLocationAcquisitionPolicy policy = new LegacyLocationAcquisitionPolicy();
        record(policy, 1_000L, 20f);
        record(policy, 6_000L, 20f);
        assertEquals(3_000L, policy.networkIntervalMs(6_000L, 3_000L));
        record(policy, 11_000L, 20f);
        assertEquals(30_000L, policy.networkIntervalMs(11_000L, 3_000L));
        assertEquals(26_000L, policy.recoveryDeadlineMs(3_000L));
    }

    @Test
    public void intermediateAccuracyAndOnePoorFixDoNotCauseOscillation() {
        LegacyLocationAcquisitionPolicy policy = stablePolicy();
        record(policy, 14_000L, 45f);
        assertEquals(30_000L, policy.networkIntervalMs(14_000L, 3_000L));
        record(policy, 17_000L, 32f);
        record(policy, 20_000L, 45f);
        assertEquals(30_000L, policy.networkIntervalMs(20_000L, 3_000L));
        record(policy, 23_000L, 45f);
        assertEquals(3_000L, policy.networkIntervalMs(23_000L, 3_000L));
    }

    @Test
    public void networkProgressCannotHideGpsSilence() {
        LegacyLocationAcquisitionPolicy policy = stablePolicy();
        NavigationLocation network = fix(25_000L, 10f);
        network.setProvider("network");
        policy.record(network, 25_000L, 3_000L);
        assertEquals(3_000L, policy.networkIntervalMs(26_000L, 3_000L));
        assertEquals(-1L, policy.recoveryDeadlineMs(3_000L));
    }

    @Test
    public void slowGpsCadenceUsesItsOwnDeadlineAndDoesNotAccelerateNetwork() {
        LegacyLocationAcquisitionPolicy policy = stablePolicy();
        assertEquals(60_000L, policy.networkIntervalMs(11_000L, 60_000L));
        assertEquals(76_000L, policy.recoveryDeadlineMs(60_000L));
        assertEquals(60_000L, policy.networkIntervalMs(75_000L, 60_000L));
    }

    @Test
    public void cachedDuplicateAndFutureFixesCannotEstablishHealth() {
        LegacyLocationAcquisitionPolicy policy = new LegacyLocationAcquisitionPolicy();
        NavigationLocation cached = fix(1_000L, 5f);
        policy.record(cached, 10_000L, 3_000L);
        policy.record(cached, 11_000L, 3_000L);
        policy.record(fix(20_000L, 5f), 12_000L, 3_000L);
        assertEquals(3_000L, policy.networkIntervalMs(12_000L, 3_000L));
        assertEquals(-1L, policy.recoveryDeadlineMs(3_000L));
    }

    @Test
    public void longGapAndResetRequireNewEvidence() {
        LegacyLocationAcquisitionPolicy policy = stablePolicy();
        record(policy, 30_000L, 20f);
        assertEquals(3_000L, policy.networkIntervalMs(30_000L, 3_000L));
        policy.reset();
        assertEquals(-1L, policy.recoveryDeadlineMs(3_000L));
    }

    private static LegacyLocationAcquisitionPolicy stablePolicy() {
        LegacyLocationAcquisitionPolicy policy = new LegacyLocationAcquisitionPolicy();
        record(policy, 1_000L, 20f);
        record(policy, 6_000L, 20f);
        record(policy, 11_000L, 20f);
        return policy;
    }

    private static void record(LegacyLocationAcquisitionPolicy policy, long time, float accuracy) {
        policy.record(fix(time, accuracy), time, 3_000L);
    }

    private static NavigationLocation fix(long time, float accuracy) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLatitude(48.2);
        location.setLongitude(16.37);
        location.setAccuracy(accuracy);
        return location;
    }
}
