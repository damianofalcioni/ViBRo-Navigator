package vibro.navigator.android.sensor;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.orientation.HeadingAccuracyStatus;

@RunWith(RobolectricTestRunner.class)
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
    public void describeRotationVectorValue_usesInjectedElapsedRealtimeForAge() {
        String value = AndroidHeadingSensorValueFormatter.describeRotationVectorValue(
                new float[]{0f, 0f, 0f, 1f, (float) Math.toRadians(7.5)},
                HeadingAccuracyStatus.HIGH,
                2_000L,
                2_450L
        );

        assertTrue(value.contains("age=450ms"));
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
