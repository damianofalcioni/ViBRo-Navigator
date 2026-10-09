package vibro.navigator.nav.orientation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StationaryCompassHeadingGateTest {
    private final StationaryCompassHeadingGate gate = new StationaryCompassHeadingGate();

    @Test
    public void initialSampleAndSmallJitterDoNotChangeHeading() {
        assertFalse(gate.accept(90.0, 5f, 1_000L));
        assertFalse(gate.accept(119.0, 5f, 2_000L));
        assertFalse(gate.accept(92.0, 5f, 3_000L));
    }

    @Test
    public void substantialTurnMustRemainStableForQuarterSecond() {
        gate.accept(90.0, 5f, 1_000L);
        assertFalse(gate.accept(120.0, 5f, 2_000L));
        assertFalse(gate.accept(123.0, 5f, 2_249L));
        assertTrue(gate.accept(125.0, 5f, 2_250L));
        assertTrue(gate.accept(129.0, 5f, 2_300L));
        assertTrue(gate.accept(90.0, 5f, 2_400L));
    }

    @Test
    public void briefSpikeOrUnstableLargeChangesDoNotQualify() {
        gate.accept(0.0, 5f, 1_000L);
        gate.accept(90.0, 5f, 2_000L);
        assertFalse(gate.accept(5.0, 5f, 2_200L));
        gate.accept(90.0, 5f, 4_000L);
        assertFalse(gate.accept(180.0, 5f, 4_200L));
        assertFalse(gate.accept(180.0, 5f, 4_449L));
        assertTrue(gate.accept(180.0, 5f, 4_450L));
    }

    @Test
    public void wrapAroundUsesShortestAngularDifference() {
        gate.accept(350.0, 5f, 1_000L);
        assertFalse(gate.accept(10.0, 5f, 2_000L));
        assertFalse(gate.accept(20.0, 5f, 3_000L));
        assertTrue(gate.accept(20.0, 5f, 4_000L));
    }

    @Test
    public void uncertaintyRaisesTheTurnThreshold() {
        gate.accept(0.0, 25f, 1_000L);
        assertFalse(gate.accept(40.0, 25f, 2_000L));
        assertFalse(gate.accept(50.0, 25f, 3_000L));
        assertTrue(gate.accept(50.0, 25f, 4_000L));
    }

    @Test
    public void poorOrMissingAccuracyCannotTriggerOrEstablishATurn() {
        assertFalse(gate.accept(0.0, null, 1_000L));
        assertFalse(gate.accept(90.0, 35f, 2_000L));
        assertFalse(gate.accept(90.0, 5f, 3_000L));
        assertFalse(gate.accept(95.0, 5f, 4_000L));
    }

    @Test
    public void missingSampleClearsPendingTurnAndReference() {
        gate.accept(0.0, 5f, 1_000L);
        gate.accept(90.0, 5f, 2_000L);
        assertFalse(gate.accept(null, 5f, 2_500L));
        assertFalse(gate.accept(90.0, 5f, 3_000L));
    }

    @Test
    public void longSampleGapCannotConfirmAPendingTurn() {
        gate.accept(0.0, 5f, 1_000L);
        gate.accept(90.0, 5f, 2_000L);
        assertFalse(gate.accept(90.0, 5f, 4_000L));
        assertTrue(gate.accept(90.0, 5f, 5_000L));
    }

    @Test
    public void resetStartsANewStationaryReference() {
        gate.accept(0.0, 5f, 1_000L);
        gate.reset();
        assertFalse(gate.accept(90.0, 5f, 2_000L));
        assertFalse(gate.accept(95.0, 5f, 3_000L));
    }

    @Test
    public void invalidValuesAreRejected() {
        assertFalse(gate.accept(Double.NaN, 5f, 1_000L));
        assertFalse(gate.accept(Double.POSITIVE_INFINITY, 5f, 1_000L));
        assertFalse(gate.accept(0.0, Float.NaN, 1_000L));
        assertFalse(gate.accept(0.0, -1f, 1_000L));
    }

    @Test
    public void activatedCompassResumesLiveAfterMissingOrPoorSamples() {
        gate.accept(90.0, 5f, 1_000L);
        gate.accept(130.0, 5f, 2_000L);
        assertTrue(gate.accept(130.0, 5f, 3_000L));
        assertFalse(gate.accept(null, 5f, 4_000L));
        assertFalse(gate.accept(132.0, 35f, 4_100L));
        assertTrue(gate.accept(132.0, 5f, 4_200L));
        gate.reset();
        assertFalse(gate.accept(133.0, 5f, 5_000L));
    }
}
