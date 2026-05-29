package vibro.navigator.about;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import vibro.navigator.nav.location.NavigationLocation;

final class AboutSensorValueFormatter {

    private AboutSensorValueFormatter() {
    }

    @NonNull
    static String describeLocationValue(@NonNull NavigationLocation location, @Nullable Integer fixedSatelliteCount) {
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
    private static LocationSnapshot toSnapshot(@NonNull NavigationLocation location) {
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
    private static Float bearingAccuracyDegrees(@NonNull NavigationLocation location) {
        return location.hasBearingAccuracy() ? location.getBearingAccuracyDegrees() : null;
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
