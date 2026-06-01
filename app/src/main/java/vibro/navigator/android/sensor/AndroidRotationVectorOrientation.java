package vibro.navigator.android.sensor;

import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class AndroidRotationVectorOrientation {
    final double headingDegrees;
    final double pitchDegrees;
    final double rollDegrees;
    @Nullable
    final Double headingAccuracyDegrees;

    private AndroidRotationVectorOrientation(
            double headingDegrees,
            double pitchDegrees,
            double rollDegrees,
            @Nullable Double headingAccuracyDegrees
    ) {
        this.headingDegrees = headingDegrees;
        this.pitchDegrees = pitchDegrees;
        this.rollDegrees = rollDegrees;
        this.headingAccuracyDegrees = headingAccuracyDegrees;
    }

    @NonNull
    static AndroidRotationVectorOrientation fromRotationVector(@NonNull float[] rotationVector) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);
        return new AndroidRotationVectorOrientation(
                (Math.toDegrees(orientation[0]) + 360.0) % 360.0,
                Math.toDegrees(orientation[1]),
                Math.toDegrees(orientation[2]),
                headingAccuracyDegrees(rotationVector)
        );
    }

    @Nullable
    private static Double headingAccuracyDegrees(@NonNull float[] rotationVector) {
        if (rotationVector.length <= 4) {
            return null;
        }
        float headingAccuracyRadians = rotationVector[4];
        if (!Float.isFinite(headingAccuracyRadians) || headingAccuracyRadians < 0f) {
            return null;
        }
        return Math.toDegrees(headingAccuracyRadians);
    }
}
