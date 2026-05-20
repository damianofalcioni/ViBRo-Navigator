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
        return describeLocationValue(
                toSnapshot(location),
                fixedSatelliteCount,
                System.currentTimeMillis()
        );
    }

    @NonNull
    static String describeLocationValue(
            @NonNull LocationSnapshot location,
            @Nullable Integer fixedSatelliteCount,
            long nowMs
    ) {
        StringBuilder sb = new StringBuilder("value=");
        sb.append(String.format(
                Locale.US,
                "lat=%.6f lon=%.6f",
                location.lat,
                location.lon
        ));
        if (location.accuracyMeters != null) {
            sb.append(String.format(Locale.US, " acc=%.1fm", location.accuracyMeters));
        }
        if (location.altitudeMeters != null) {
            sb.append(String.format(Locale.US, " alt=%.1fm", location.altitudeMeters));
        }
        if (location.speedMps != null) {
            sb.append(String.format(Locale.US, " speed=%.1fkm/h", location.speedMps * 3.6f));
        }
        if (location.bearingDegrees != null) {
            sb.append(String.format(Locale.US, " bearing=%.0fdeg", location.bearingDegrees));
        }
        appendBearingAccuracy(sb, location.bearingAccuracyDegrees);
        appendSatelliteCount(sb, fixedSatelliteCount);
        long ageSeconds = Math.max(0L, (nowMs - location.timeMs) / 1000L);
        sb.append(" age=").append(ageSeconds).append("s");
        return sb.toString();
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

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);
        double headingDegrees = (Math.toDegrees(orientation[0]) + 360.0) % 360.0;
        double pitchDegrees = Math.toDegrees(orientation[1]);
        double rollDegrees = Math.toDegrees(orientation[2]);
        String headingAccuracyValue = describeHeadingAccuracy(rotationVector);
        long ageMs = Math.max(0L, SystemClock.elapsedRealtime() - elapsedRealtimeMs);

        return String.format(
                Locale.US,
                "value=heading=%.0fdeg pitch=%.0fdeg roll=%.0fdeg headingAcc=%s acc=%s age=%dms raw=%s",
                headingDegrees,
                pitchDegrees,
                rollDegrees,
                headingAccuracyValue,
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

        double headingDegrees = (orientationValues[0] + 360.0) % 360.0;
        double pitchDegrees = orientationValues[1];
        double rollDegrees = orientationValues[2];
        long ageMs = Math.max(0L, SystemClock.elapsedRealtime() - elapsedRealtimeMs);

        return String.format(
                Locale.US,
                "value=heading=%.0fdeg pitch=%.0fdeg roll=%.0fdeg acc=%s age=%dms raw=%s",
                headingDegrees,
                pitchDegrees,
                rollDegrees,
                accuracyLabel(accuracy),
                ageMs,
                formatVector(orientationValues)
        );
    }

    private static void appendBearingAccuracy(@NonNull StringBuilder sb, @Nullable Float bearingAccuracyDegrees) {
        if (bearingAccuracyDegrees != null) {
            sb.append(String.format(Locale.US, " bearingAcc=%.0fdeg", bearingAccuracyDegrees));
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

    @NonNull
    private static LocationSnapshot toSnapshot(@NonNull Location location) {
        return new LocationSnapshot(
                location.getLatitude(),
                location.getLongitude(),
                location.getTime(),
                location.hasAccuracy() ? location.getAccuracy() : null,
                location.hasAltitude() ? location.getAltitude() : null,
                location.hasSpeed() ? location.getSpeed() : null,
                location.hasBearing() ? location.getBearing() : null,
                bearingAccuracyDegrees(location)
        );
    }

    @Nullable
    private static Float bearingAccuracyDegrees(@NonNull Location location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy()) {
            return location.getBearingAccuracyDegrees();
        }
        return null;
    }

    static final class LocationSnapshot {
        final double lat;
        final double lon;
        final long timeMs;
        @Nullable
        final Float accuracyMeters;
        @Nullable
        final Double altitudeMeters;
        @Nullable
        final Float speedMps;
        @Nullable
        final Float bearingDegrees;
        @Nullable
        final Float bearingAccuracyDegrees;

        LocationSnapshot(
                double lat,
                double lon,
                long timeMs,
                @Nullable Float accuracyMeters,
                @Nullable Double altitudeMeters,
                @Nullable Float speedMps,
                @Nullable Float bearingDegrees,
                @Nullable Float bearingAccuracyDegrees
        ) {
            this.lat = lat;
            this.lon = lon;
            this.timeMs = timeMs;
            this.accuracyMeters = accuracyMeters;
            this.altitudeMeters = altitudeMeters;
            this.speedMps = speedMps;
            this.bearingDegrees = bearingDegrees;
            this.bearingAccuracyDegrees = bearingAccuracyDegrees;
        }
    }
}
