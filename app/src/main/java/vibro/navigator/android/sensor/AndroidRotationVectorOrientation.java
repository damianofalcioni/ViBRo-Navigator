package vibro.navigator.android.sensor;

import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class AndroidRotationVectorOrientation {
    static final RotationVectorMath SENSOR_MANAGER_MATH = new RotationVectorMath() {
        @Override
        public void getRotationMatrixFromVector(
                @NonNull float[] rotationMatrix,
                @NonNull float[] rotationVector
        ) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
        }

        @Override
        public void getOrientation(@NonNull float[] rotationMatrix, @NonNull float[] orientation) {
            SensorManager.getOrientation(rotationMatrix, orientation);
        }
    };

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
        return fromRotationVector(rotationVector, SENSOR_MANAGER_MATH);
    }

    @NonNull
    static AndroidRotationVectorOrientation fromRotationVector(
            @NonNull float[] rotationVector,
            @NonNull RotationVectorMath rotationVectorMath
    ) {
        float[] rotationMatrix = new float[9];
        rotationVectorMath.getRotationMatrixFromVector(rotationMatrix, rotationVector);
        float[] orientation = new float[3];
        rotationVectorMath.getOrientation(rotationMatrix, orientation);
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

    interface RotationVectorMath {
        void getRotationMatrixFromVector(
                @NonNull float[] rotationMatrix,
                @NonNull float[] rotationVector
        );

        void getOrientation(@NonNull float[] rotationMatrix, @NonNull float[] orientation);
    }
}
