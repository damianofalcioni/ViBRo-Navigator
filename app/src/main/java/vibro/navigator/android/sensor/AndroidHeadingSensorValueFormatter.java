package vibro.navigator.android.sensor;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import vibro.navigator.nav.orientation.HeadingAccuracyStatus;

final class AndroidHeadingSensorValueFormatter {
    private AndroidHeadingSensorValueFormatter() {
    }

    @NonNull
    static String describeRotationVectorValue(
            @Nullable float[] rotationVector,
            int accuracy,
            long elapsedRealtimeMs
    ) {
        if (rotationVector == null || elapsedRealtimeMs < 0L) {
            return "value=waiting for sample";
        }

        AndroidRotationVectorOrientation orientation =
                AndroidRotationVectorOrientation.fromRotationVector(rotationVector);
        long ageMs = Math.max(0L, SystemClock.elapsedRealtime() - elapsedRealtimeMs);

        return String.format(
                Locale.US,
                "value=heading=%.0fdeg pitch=%.0fdeg roll=%.0fdeg headingAcc=%s acc=%s age=%dms raw=%s",
                orientation.headingDegrees,
                orientation.pitchDegrees,
                orientation.rollDegrees,
                describeHeadingAccuracy(rotationVector),
                accuracyLabel(accuracy),
                ageMs,
                formatVector(rotationVector)
        );
    }

    @NonNull
    static String describeOrientationValue(
            @Nullable float[] orientationValues,
            int accuracy,
            long elapsedRealtimeMs
    ) {
        if (orientationValues == null || elapsedRealtimeMs < 0L) {
            return "value=waiting for sample";
        }
        if (orientationValues.length < 3) {
            return String.format(
                    Locale.US,
                    "value=invalid acc=%s raw=%s",
                    accuracyLabel(accuracy),
                    formatVector(orientationValues)
            );
        }

        long ageMs = Math.max(0L, SystemClock.elapsedRealtime() - elapsedRealtimeMs);

        return String.format(
                Locale.US,
                "value=heading=%.0fdeg pitch=%.0fdeg roll=%.0fdeg acc=%s age=%dms raw=%s",
                (orientationValues[0] + 360.0) % 360.0,
                orientationValues[1],
                orientationValues[2],
                accuracyLabel(accuracy),
                ageMs,
                formatVector(orientationValues)
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

    @NonNull
    private static String accuracyLabel(int accuracy) {
        switch (accuracy) {
            case HeadingAccuracyStatus.LOW:
                return "low";
            case HeadingAccuracyStatus.MEDIUM:
                return "medium";
            case HeadingAccuracyStatus.HIGH:
                return "high";
            case HeadingAccuracyStatus.UNRELIABLE:
            default:
                return "unreliable";
        }
    }

    @NonNull
    private static String formatVector(@NonNull float[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format(Locale.US, "%.3f", values[i]));
        }
        sb.append("]");
        return sb.toString();
    }
}
