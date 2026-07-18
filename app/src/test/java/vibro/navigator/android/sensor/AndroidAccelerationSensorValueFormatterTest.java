package vibro.navigator.android.sensor;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.orientation.HeadingAccuracyStatus;

public class AndroidAccelerationSensorValueFormatterTest {
    @Test
    public void describeValue_formatsAccelerationAxesMagnitudeAndAge() {
        String value = AndroidAccelerationSensorValueFormatter.describeValue(
                new float[]{1f, 2f, 2f},
                HeadingAccuracyStatus.HIGH,
                1_000L,
                1_250L
        );

        assertTrue(value.contains("x=1.000m/s^2"));
        assertTrue(value.contains("y=2.000m/s^2"));
        assertTrue(value.contains("z=2.000m/s^2"));
        assertTrue(value.contains("mag=3.000m/s^2"));
        assertTrue(value.contains("age=250ms"));
    }

    @Test
    public void describeValue_clampsNegativeAge() {
        String value = AndroidAccelerationSensorValueFormatter.describeValue(
                new float[]{1f, 0f, 0f},
                HeadingAccuracyStatus.HIGH,
                1_000L,
                900L
        );

        assertTrue(value.contains("age=0ms"));
    }
}
