package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.hardware.SensorManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class StationaryOrientationAdvisorTest {

    private final StationaryOrientationAdvisor advisor = new StationaryOrientationAdvisor();

    @Test
    public void evaluate_returnsRightTurnForClockwiseAdjustmentAfterDwell() {
        StationaryOrientationAdvisor.Evaluation evaluation = advisor.evaluate(
                0.0f,
                1_000L,
                10.0,
                sample(350.0, 0.0, 0.0, SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 5_500L),
                6_000L
        );

        assertEquals(StationaryOrientationAdvisor.Outcome.NOTIFY, evaluation.outcome);
        assertNotNull(evaluation.decision);
        assertTrue(evaluation.decision.turnRight());
        assertEquals(20.0, evaluation.decision.absoluteTurnDegrees(), 0.001);
    }

    @Test
    public void evaluate_waitsForDwellBeforeNotifying() {
        StationaryOrientationAdvisor.Evaluation evaluation = advisor.evaluate(
                0.0f,
                2_000L,
                90.0,
                sample(20.0, 0.0, 0.0, SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 2_500L),
                6_000L
        );

        assertEquals(StationaryOrientationAdvisor.Outcome.WAITING_FOR_DWELL, evaluation.outcome);
        assertNull(evaluation.decision);
    }

    @Test
    public void evaluate_returnsMovingWhenUserIsMoving() {
        StationaryOrientationAdvisor.Evaluation evaluation = advisor.evaluate(
                1.2f,
                1_000L,
                90.0,
                sample(20.0, 0.0, 0.0, SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 5_500L),
                6_000L
        );

        assertEquals(StationaryOrientationAdvisor.Outcome.MOVING, evaluation.outcome);
        assertNull(evaluation.decision);
    }

    @Test
    public void evaluate_waitsWhenGeomagneticAccuracyIsTooLow() {
        StationaryOrientationAdvisor.Evaluation evaluation = advisor.evaluate(
                0.0f,
                1_000L,
                90.0,
                sample(20.0, 0.0, 0.0, SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM, 5_500L),
                6_000L
        );

        assertEquals(StationaryOrientationAdvisor.Outcome.WAITING_FOR_CALIBRATION, evaluation.outcome);
        assertNull(evaluation.decision);
    }

    @Test
    public void evaluate_allowsVerticalPhoneOrientation() {
        StationaryOrientationAdvisor.Evaluation evaluation = advisor.evaluate(
                0.0f,
                1_000L,
                90.0,
                sample(20.0, 30.0, 0.0, SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 5_500L),
                6_000L
        );

        assertEquals(StationaryOrientationAdvisor.Outcome.NOTIFY, evaluation.outcome);
        assertNotNull(evaluation.decision);
    }

    @Test
    public void evaluate_marksMostlyAlignedWithoutNotification() {
        StationaryOrientationAdvisor.Evaluation evaluation = advisor.evaluate(
                0.0f,
                1_000L,
                12.0,
                sample(2.0, 0.0, 0.0, SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 5_500L),
                6_000L
        );

        assertEquals(StationaryOrientationAdvisor.Outcome.ALIGNED, evaluation.outcome);
        assertNull(evaluation.decision);
    }

    private static GeomagneticOrientationMonitor.Sample sample(
            double headingDegrees,
            double pitchDegrees,
            double rollDegrees,
            int accuracy,
            long elapsedRealtimeMs
    ) {
        return new GeomagneticOrientationMonitor.Sample(
                headingDegrees,
                pitchDegrees,
                rollDegrees,
                accuracy,
                elapsedRealtimeMs
        );
    }
}
