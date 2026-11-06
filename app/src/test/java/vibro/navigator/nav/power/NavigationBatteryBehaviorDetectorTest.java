package vibro.navigator.nav.power;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationBatteryBehaviorDetectorTest {
    private static final long MINUTE_MS = 60_000L;
    private static final int UNKNOWN = Integer.MIN_VALUE;
    private final NavigationBatteryBehaviorDetector detector = new NavigationBatteryBehaviorDetector();

    @Test
    public void standardUnitsNeedSustainedIndependentPercentageAndCurrentSupport() {
        NavigationBatteryBehavior behavior = learn(3_000_000, -10_000, -600_000);
        assertEquals(NavigationBatteryBehavior.State.INFERRED, behavior.state);
        assertEquals(1, behavior.counterMultiplier);
        assertEquals(-1, behavior.dischargeDirection);
        assertEquals(1, behavior.currentMultiplier);
        assertEquals(-1, behavior.currentDischargeSign);
        assertTrue(behavior.describe().contains("not_independently_verified"));
    }

    @Test
    public void increasingMilliampHourAccumulatorIsDiscoveredWithoutDeviceIdentity() {
        NavigationBatteryBehavior behavior = learn(100, 10, -600);
        assertEquals(NavigationBatteryBehavior.State.INFERRED, behavior.state);
        assertEquals(1_000, behavior.counterMultiplier);
        assertEquals(1, behavior.dischargeDirection);
        assertEquals(1_000, behavior.currentMultiplier);
    }

    @Test
    public void signedCountersAndReversedCurrentSignsArePreserved() {
        NavigationBatteryBehavior behavior = learn(-100_000, 10_000, 600_000);
        assertEquals(NavigationBatteryBehavior.State.INFERRED, behavior.state);
        assertEquals(1, behavior.counterMultiplier);
        assertEquals(1, behavior.dischargeDirection);
        assertEquals(1, behavior.currentDischargeSign);
    }

    @Test
    public void counterAndCurrentScalesAreResolvedIndependently() {
        assertEquals(1_000, learn(3_000_000, -10_000, -600).currentMultiplier);
        detector.reset();
        NavigationBatteryBehavior behavior = learn(100, 10, -600_000);
        assertEquals(1_000, behavior.counterMultiplier);
        assertEquals(1, behavior.currentMultiplier);
        detector.reset();
        assertEquals(-1, learn(3_000, -10, -600).dischargeDirection);
    }

    @Test
    public void currentAgreementAloneCannotEstablishAbsoluteUnits() {
        NavigationBatteryBehavior behavior = null;
        for (int i = 0; i <= 8; i++) {
            behavior = detector.record(observation(100 + i * 10, -600, 80, i * MINUTE_MS));
        }
        assertEquals(NavigationBatteryBehavior.State.LEARNING, behavior.state);
        assertEquals(0, behavior.counterMultiplier);
    }

    @Test
    public void oneGaugeJumpAndClusteredCallbacksCannotCalibrate() {
        detector.record(observation(100, -600, 80, 0));
        detector.record(observation(110, -600, 80, MINUTE_MS));
        detector.record(observation(120, -600, 80, 2 * MINUTE_MS));
        assertEquals(NavigationBatteryBehavior.State.LEARNING,
                detector.record(observation(130, -600, 77, 3 * MINUTE_MS)).state);
        detector.reset();
        for (int i = 0; i < 4; i++) {
            assertEquals(NavigationBatteryBehavior.State.LEARNING,
                    detector.record(observation(100 + i, -600, 80 - i, i * 1_000L)).state);
        }
    }

    @Test
    public void ambiguousCapacityAndContradictoryCurrentStayUnavailable() {
        assertEquals(NavigationBatteryBehavior.State.AMBIGUOUS, learn(100, 1, -60).state);
        detector.reset();
        assertEquals(NavigationBatteryBehavior.State.AMBIGUOUS, learn(100, 10, -6_000_000).state);
        detector.reset();
        assertEquals(NavigationBatteryBehavior.State.AMBIGUOUS, learn(100, 300, -18_000).state);
    }

    @Test
    public void missingOrZeroCurrentDoesNotBecomeVerifiedZeroConsumption() {
        NavigationBatteryBehavior behavior = learn(100, 10, UNKNOWN);
        assertEquals(NavigationBatteryBehavior.State.INFERRED, behavior.state);
        assertEquals(0, behavior.currentMultiplier);
        detector.reset();
        assertEquals(0, learn(100, 10, 0).currentMultiplier);
    }

    @Test
    public void counterReversalInvalidatesEvenInsideIndependentSpacing() {
        NavigationBatteryBehavior learned = learn(100, 10, -600);
        NavigationBatteryBehavior reset = detector.record(observation(129, -600, 77, 3 * MINUTE_MS + 1));
        assertTrue(reset.epoch > learned.epoch);
        assertEquals("counter_direction_changed", reset.reason);
        assertFalse(reset.isInferred());
    }

    @Test
    public void chargingUnknownPowerUnsupportedTimeAndLongGapsBreakTheEpoch() {
        NavigationBatteryBehavior learned = learn(100, 10, -600);
        NavigationBatteryBehavior charging = detector.record(new NavigationBatteryObservation(129, 600, 77,
                4 * MINUTE_MS, NavigationBatteryObservation.PowerState.CHARGING));
        assertTrue(charging.epoch > learned.epoch);
        assertEquals(NavigationBatteryBehavior.State.UNAVAILABLE, charging.state);
        assertEquals("power_state_unknown", detector.record(new NavigationBatteryObservation(130, -600, 77,
                5 * MINUTE_MS, NavigationBatteryObservation.PowerState.UNKNOWN)).reason);
        assertEquals("unsupported_observation", detector.record(observation(UNKNOWN, -600, 77, 6 * MINUTE_MS)).reason);
        detector.reset();
        learned = learn(100, 10, -600);
        assertTrue(detector.record(observation(140, -600, 76, 20 * MINUTE_MS)).epoch > learned.epoch);
    }

    @Test
    public void duplicateReadsCannotAdvanceLearningAndClockRegressionResets() {
        NavigationBatteryObservation sample = observation(100, -600, 80, MINUTE_MS);
        NavigationBatteryBehavior first = detector.record(sample);
        for (int i = 0; i < 10; i++) {
            assertSame(first, detector.record(sample));
        }
        assertEquals("observation_time_discontinuity", detector.record(observation(101, -600, 80, 0)).reason);
    }

    @Test
    public void missingInitialPercentageCanRecoverButAnInferredGaugeJumpCannotBridge() {
        detector.record(observation(90, -600, -1, 0));
        NavigationBatteryBehavior learned = null;
        for (int i = 0; i < 4; i++) {
            learned = detector.record(observation(100 + 10 * i, -600, 80 - i, (i + 1) * MINUTE_MS));
        }
        assertTrue(learned.isInferred());
        NavigationBatteryBehavior reset = detector.record(observation(1_000, -600, 76, 5 * MINUTE_MS));
        assertEquals("counter_jump", reset.reason);
        assertTrue(reset.epoch > learned.epoch);
    }

    @Test
    public void candidateScaleMustRejectAnImpossibleClusteredJumpBeforeLearning() {
        detector.record(observation(100, -600, 80, 0));
        detector.record(observation(140, -600, 80, 1));
        NavigationBatteryBehavior behavior = null;
        for (int i = 1; i <= 3; i++) {
            behavior = detector.record(observation(140 + 10 * i, -600, 80 - i, i * MINUTE_MS));
        }
        assertEquals(NavigationBatteryBehavior.State.AMBIGUOUS, behavior.state);
        assertEquals(0, behavior.counterMultiplier);
    }

    private NavigationBatteryBehavior learn(int start, int step, int current) {
        NavigationBatteryBehavior behavior = null;
        for (int i = 0; i < 4; i++) {
            behavior = detector.record(observation(start + i * step, current, 80 - i, i * MINUTE_MS));
        }
        return behavior;
    }

    private static NavigationBatteryObservation observation(int counter, int current, int level, long time) {
        return new NavigationBatteryObservation(counter, current, level, time,
                NavigationBatteryObservation.PowerState.DISCHARGING);
    }
}
