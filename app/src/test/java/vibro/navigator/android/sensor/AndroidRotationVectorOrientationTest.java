package vibro.navigator.android.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AndroidRotationVectorOrientationTest {
    @Test
    public void fromRotationVector_readsIdentityOrientationAndHeadingAccuracy() {
        AndroidRotationVectorOrientation orientation =
                AndroidRotationVectorOrientation.fromRotationVector(new float[]{
                        0f,
                        0f,
                        0f,
                        1f,
                        (float) Math.toRadians(12.5)
                });

        assertEquals(0.0, orientation.headingDegrees, 0.0001);
        assertEquals(0.0, orientation.pitchDegrees, 0.0001);
        assertEquals(0.0, orientation.rollDegrees, 0.0001);
        assertEquals(12.5, orientation.headingAccuracyDegrees, 0.0001);
    }

    @Test
    public void fromRotationVector_ignoresMissingHeadingAccuracy() {
        AndroidRotationVectorOrientation orientation =
                AndroidRotationVectorOrientation.fromRotationVector(new float[]{0f, 0f, 0f, 1f});

        assertNull(orientation.headingAccuracyDegrees);
    }

    @Test
    public void fromRotationVector_ignoresInvalidHeadingAccuracy() {
        AndroidRotationVectorOrientation orientation =
                AndroidRotationVectorOrientation.fromRotationVector(new float[]{0f, 0f, 0f, 1f, -1f});

        assertNull(orientation.headingAccuracyDegrees);
    }
}
