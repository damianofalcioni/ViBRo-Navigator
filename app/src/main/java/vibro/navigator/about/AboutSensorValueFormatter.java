package vibro.navigator.about;

import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

final class AboutSensorValueFormatter {

    private AboutSensorValueFormatter() {
    }

    @NonNull
    static String describeLocationValue(@NonNull Location location, @Nullable Integer fixedSatelliteCount) {
        StringBuilder sb = new StringBuilder("value=");
        sb.append(String.format(
                Locale.US,
                "lat=%.6f lon=%.6f",
                location.getLatitude(),
                location.getLongitude()
        ));
        if (location.hasAccuracy()) {
            sb.append(String.format(Locale.US, " acc=%.1fm", location.getAccuracy()));
        }
        if (location.hasAltitude()) {
            sb.append(String.format(Locale.US, " alt=%.1fm", location.getAltitude()));
        }
        if (location.hasSpeed()) {
            sb.append(String.format(Locale.US, " speed=%.1fkm/h", location.getSpeed() * 3.6f));
        }
        if (location.hasBearing()) {
            sb.append(String.format(Locale.US, " bearing=%.0fdeg", location.getBearing()));
        }
        appendBearingAccuracy(sb, location);
        appendSatelliteCount(sb, fixedSatelliteCount);
        long ageSeconds = Math.max(0L, (System.currentTimeMillis() - location.getTime()) / 1000L);
        sb.append(" age=").append(ageSeconds).append("s");
        return sb.toString();
    }

    @NonNull
    static String describeGeomagneticValue(
            @Nullable float[] geomagneticVector,
            int geomagneticAccuracy,
            long geomagneticElapsedRealtimeMs
    ) {
        if (geomagneticVector == null || geomagneticElapsedRealtimeMs < 0L) {
            return "value=waiting for sample";
        }

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, geomagneticVector);
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);
        double headingDegrees = (Math.toDegrees(orientation[0]) + 360.0) % 360.0;
        double pitchDegrees = Math.toDegrees(orientation[1]);
        double rollDegrees = Math.toDegrees(orientation[2]);
        String headingAccuracyValue = describeHeadingAccuracy(geomagneticVector);
        long ageMs = Math.max(0L, SystemClock.elapsedRealtime() - geomagneticElapsedRealtimeMs);

        return String.format(
                Locale.US,
                "value=heading=%.0fdeg pitch=%.0fdeg roll=%.0fdeg headingAcc=%s acc=%s age=%dms raw=%s",
                headingDegrees,
                pitchDegrees,
                rollDegrees,
                headingAccuracyValue,
                accuracyLabel(geomagneticAccuracy),
                ageMs,
                formatVector(geomagneticVector)
        );
    }

    private static void appendBearingAccuracy(@NonNull StringBuilder sb, @NonNull Location location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy()) {
            sb.append(String.format(Locale.US, " bearingAcc=%.0fdeg", location.getBearingAccuracyDegrees()));
        }
    }

    private static void appendSatelliteCount(@NonNull StringBuilder sb, @Nullable Integer fixedSatelliteCount) {
        if (fixedSatelliteCount != null && fixedSatelliteCount >= 0) {
            sb.append(" sats=").append(fixedSatelliteCount);
        }
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
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return "low";
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return "medium";
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return "high";
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
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
