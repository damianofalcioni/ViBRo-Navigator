package vibro.navigator.android.sensor;

import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import vibro.navigator.nav.orientation.HeadingAccuracyStatus;

public class AndroidHeadingSensorRotationVectorValueFormatterTest {
    @Test
    public void describeRotationVectorValue_usesInjectedElapsedRealtimeForAge() {
        String value = AndroidHeadingSensorValueFormatter.describeRotationVectorValue(
                new float[]{0f, 0f, 0f, 1f, (float) Math.toRadians(7.5)},
                HeadingAccuracyStatus.HIGH,
                2_000L,
                2_450L,
                new IdentityRotationVectorMath()
        );

        assertTrue(value.contains("age=450ms"));
    }

    private static final class IdentityRotationVectorMath implements AndroidRotationVectorOrientation.RotationVectorMath {
        @Override
        public void getRotationMatrixFromVector(
                @NonNull float[] rotationMatrix,
                @NonNull float[] rotationVector
        ) {
            rotationMatrix[0] = 1f;
        }

        @Override
        public void getOrientation(@NonNull float[] rotationMatrix, @NonNull float[] orientation) {
            orientation[0] = 0f;
            orientation[1] = 0f;
            orientation[2] = 0f;
        }
    }
}
