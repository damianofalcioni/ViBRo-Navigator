package vibro.navigator.android.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;

import org.junit.Test;

public class AndroidRotationVectorOrientationTest {
    @Test
    public void fromRotationVector_convertsOrientationRadiansAndHeadingAccuracy() {
        AndroidRotationVectorOrientation orientation =
                AndroidRotationVectorOrientation.fromRotationVector(new float[]{
                        0f,
                        0f,
                        0f,
                        1f,
                        (float) Math.toRadians(12.5)
                }, new FakeRotationVectorMath(
                        Math.toRadians(-90.0),
                        Math.toRadians(5.0),
                        Math.toRadians(-2.5)
                ));

        assertEquals(270.0, orientation.headingDegrees, 0.0001);
        assertEquals(5.0, orientation.pitchDegrees, 0.0001);
        assertEquals(-2.5, orientation.rollDegrees, 0.0001);
        assertEquals(12.5, orientation.headingAccuracyDegrees, 0.0001);
    }

    @Test
    public void fromRotationVector_ignoresMissingHeadingAccuracy() {
        AndroidRotationVectorOrientation orientation =
                AndroidRotationVectorOrientation.fromRotationVector(
                        new float[]{0f, 0f, 0f, 1f},
                        FakeRotationVectorMath.IDENTITY
                );

        assertNull(orientation.headingAccuracyDegrees);
    }

    @Test
    public void fromRotationVector_ignoresInvalidHeadingAccuracy() {
        AndroidRotationVectorOrientation orientation =
                AndroidRotationVectorOrientation.fromRotationVector(
                        new float[]{0f, 0f, 0f, 1f, -1f},
                        FakeRotationVectorMath.IDENTITY
                );

        assertNull(orientation.headingAccuracyDegrees);
    }

    private static final class FakeRotationVectorMath implements AndroidRotationVectorOrientation.RotationVectorMath {
        private static final FakeRotationVectorMath IDENTITY = new FakeRotationVectorMath(0f, 0f, 0f);

        private final float headingRadians;
        private final float pitchRadians;
        private final float rollRadians;

        private FakeRotationVectorMath(double headingRadians, double pitchRadians, double rollRadians) {
            this.headingRadians = (float) headingRadians;
            this.pitchRadians = (float) pitchRadians;
            this.rollRadians = (float) rollRadians;
        }

        @Override
        public void getRotationMatrixFromVector(
                @NonNull float[] rotationMatrix,
                @NonNull float[] rotationVector
        ) {
            rotationMatrix[0] = 1f;
        }

        @Override
        public void getOrientation(@NonNull float[] rotationMatrix, @NonNull float[] orientation) {
            orientation[0] = headingRadians;
            orientation[1] = pitchRadians;
            orientation[2] = rollRadians;
        }
    }
}
