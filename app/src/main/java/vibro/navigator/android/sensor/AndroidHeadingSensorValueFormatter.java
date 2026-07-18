package vibro.navigator.android.sensor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

final class AndroidHeadingSensorValueFormatter {
    private AndroidHeadingSensorValueFormatter() {
    }

    @NonNull
    static String describeRotationVectorValue(
            @Nullable float[] rotationVector,
            int accuracy,
            long elapsedRealtimeMs,
            long nowElapsedRealtimeMs
    ) {
        if (rotationVector == null || elapsedRealtimeMs < 0L) {
            return "value=waiting for sample";
        }

        AndroidRotationVectorOrientation orientation =
                AndroidRotationVectorOrientation.fromRotationVector(rotationVector);
        long ageMs = Math.max(0L, nowElapsedRealtimeMs - elapsedRealtimeMs);

        return String.format(
                Locale.US,
                "value=heading=%.0fdeg pitch=%.0fdeg roll=%.0fdeg headingAcc=%s acc=%s age=%dms raw=%s",
                orientation.headingDegrees,
                orientation.pitchDegrees,
                orientation.rollDegrees,
                describeHeadingAccuracy(rotationVector),
                AndroidSensorValueFormat.accuracyLabel(accuracy),
                ageMs,
                AndroidSensorValueFormat.formatVector(rotationVector)
        );
    }

    @NonNull
    static String describeOrientationValue(
            @Nullable float[] orientationValues,
            int accuracy,
            long elapsedRealtimeMs,
            long nowElapsedRealtimeMs
    ) {
        if (orientationValues == null || elapsedRealtimeMs < 0L) {
            return "value=waiting for sample";
        }
        if (orientationValues.length < 3) {
            return String.format(
                    Locale.US,
                    "value=invalid acc=%s raw=%s",
                    AndroidSensorValueFormat.accuracyLabel(accuracy),
                    AndroidSensorValueFormat.formatVector(orientationValues)
            );
        }

        long ageMs = Math.max(0L, nowElapsedRealtimeMs - elapsedRealtimeMs);

        return String.format(
                Locale.US,
                "value=heading=%.0fdeg pitch=%.0fdeg roll=%.0fdeg acc=%s age=%dms raw=%s",
                (orientationValues[0] + 360.0) % 360.0,
                orientationValues[1],
                orientationValues[2],
                AndroidSensorValueFormat.accuracyLabel(accuracy),
                ageMs,
                AndroidSensorValueFormat.formatVector(orientationValues)
        );
    }

    @NonNull
    private static String describeHeadingAccuracy(@NonNull float[] values) {
        if (values.length <= 4) {
            return "missing";
        }
        float headingAccuracyRadians = values[4];
        if (!Float.isFinite(headingAccuracyRadians)) {
            return "invalid";
        }
        if (headingAccuracyRadians < 0f) {
            return "unreliable";
        }
        return String.format(Locale.US, "%.1fdeg", Math.toDegrees(headingAccuracyRadians));
    }
}
