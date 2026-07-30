package vibro.navigator.android.sensor;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.orientation.HeadingAccuracyStatus;

public class AndroidHeadingSensorValueFormatterTest {
    @Test
    public void describeOrientationValue_usesInjectedElapsedRealtimeForAge() {
        String value = AndroidHeadingSensorValueFormatter.describeOrientationValue(
                new float[]{45f, 1f, 2f},
                HeadingAccuracyStatus.HIGH,
                1_000L,
                1_250L
        );

        assertTrue(value.contains("age=250ms"));
    }

    @Test
    public void describeOrientationValue_clampsNegativeAge() {
        String value = AndroidHeadingSensorValueFormatter.describeOrientationValue(
                new float[]{45f, 1f, 2f},
                HeadingAccuracyStatus.HIGH,
                1_000L,
                900L
        );

        assertTrue(value.contains("age=0ms"));
    }
}
