package vibro.navigator.android.sensor;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.orientation.HeadingAccuracyStatus;

@RunWith(RobolectricTestRunner.class)
public class AndroidHeadingSensorRotationVectorValueFormatterRobolectricTest {
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
}
