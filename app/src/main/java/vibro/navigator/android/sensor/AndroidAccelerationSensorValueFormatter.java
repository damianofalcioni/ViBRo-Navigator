package vibro.navigator.android.sensor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

final class AndroidAccelerationSensorValueFormatter {
    private static final int AXIS_COUNT = 3;

    private AndroidAccelerationSensorValueFormatter() {
    }

    @NonNull
    static String describeValue(
            @Nullable float[] accelerationValues,
            int accuracy,
            long elapsedRealtimeMs,
            long nowElapsedRealtimeMs
    ) {
        if (accelerationValues == null || elapsedRealtimeMs < 0L) {
            return "value=waiting for sample";
        }
        if (accelerationValues.length < AXIS_COUNT) {
            return String.format(
                    Locale.US,
                    "value=invalid acc=%s raw=%s",
                    AndroidSensorValueFormat.accuracyLabel(accuracy),
                    AndroidSensorValueFormat.formatVector(accelerationValues)
            );
        }
        long ageMs = Math.max(0L, nowElapsedRealtimeMs - elapsedRealtimeMs);
        return String.format(
                Locale.US,
                "value=x=%.3fm/s^2 y=%.3fm/s^2 z=%.3fm/s^2 mag=%.3fm/s^2 acc=%s age=%dms raw=%s",
                accelerationValues[0],
                accelerationValues[1],
                accelerationValues[2],
                magnitude(accelerationValues),
                AndroidSensorValueFormat.accuracyLabel(accuracy),
                ageMs,
                AndroidSensorValueFormat.formatVector(accelerationValues)
        );
    }

    private static double magnitude(@NonNull float[] accelerationValues) {
        double x = accelerationValues[0];
        double y = accelerationValues[1];
        double z = accelerationValues[2];
        return Math.sqrt(x * x + y * y + z * z);
    }
}
