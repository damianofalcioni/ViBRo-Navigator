package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavGpsTelemetry {
    public final float speedMps;
    @NonNull
    public final String compactLine;
    @NonNull
    public final String speedText;
    @NonNull
    public final String elevationText;
    @NonNull
    public final String accuracyText;
    @NonNull
    public final String bearingText;
    @NonNull
    public final String bearingAccuracyText;
    @NonNull
    public final String fixedSatelliteCountText;
    @NonNull
    public final String acquiredFixCountText;
    @NonNull
    public final String obtainedTimeText;

    public NavGpsTelemetry(
            float speedMps,
            @NonNull String compactLine,
            @NonNull String speedText,
            @NonNull String elevationText,
            @NonNull String accuracyText,
            @NonNull String bearingText,
            @NonNull String bearingAccuracyText,
            @NonNull String fixedSatelliteCountText,
            @NonNull String acquiredFixCountText,
            @NonNull String obtainedTimeText
    ) {
        this.speedMps = speedMps;
        this.compactLine = compactLine;
        this.speedText = speedText;
        this.elevationText = elevationText;
        this.accuracyText = accuracyText;
        this.bearingText = bearingText;
        this.bearingAccuracyText = bearingAccuracyText;
        this.fixedSatelliteCountText = fixedSatelliteCountText;
        this.acquiredFixCountText = acquiredFixCountText;
        this.obtainedTimeText = obtainedTimeText;
    }

    @NonNull
    public static NavGpsTelemetry unavailable(@NonNull String unavailableText) {
        return new NavGpsTelemetry(
                Float.NaN,
                unavailableText,
                unavailableText,
                unavailableText,
                unavailableText,
                unavailableText,
                unavailableText,
                unavailableText,
                unavailableText,
                unavailableText
        );
    }

    @NonNull
    public NavGpsTelemetry withCompactLine(@NonNull String compactLine) {
        return new NavGpsTelemetry(
                speedMps,
                compactLine,
                speedText,
                elevationText,
                accuracyText,
                bearingText,
                bearingAccuracyText,
                fixedSatelliteCountText,
                acquiredFixCountText,
                obtainedTimeText
        );
    }

    @NonNull
    public NavGpsTelemetry withObtainedTimeText(@NonNull String obtainedTimeText) {
        return new NavGpsTelemetry(
                speedMps,
                compactLine,
                speedText,
                elevationText,
                accuracyText,
                bearingText,
                bearingAccuracyText,
                fixedSatelliteCountText,
                acquiredFixCountText,
                obtainedTimeText
        );
    }
}
