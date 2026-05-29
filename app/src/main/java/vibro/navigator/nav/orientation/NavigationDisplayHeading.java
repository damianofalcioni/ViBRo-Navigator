package vibro.navigator.nav.orientation;

import androidx.annotation.Nullable;

public final class NavigationDisplayHeading {

    private static final long MAX_SAMPLE_AGE_MS = 5_000L;

    private NavigationDisplayHeading() {
    }

    @Nullable
    public static Double headingDegrees(
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            boolean monitoringActive,
            long nowElapsedRealtimeMs,
            int displayRotation
    ) {
        GeomagneticOrientationMonitor.Sample freshSample = freshSample(sample, monitoringActive, nowElapsedRealtimeMs);
        return freshSample == null
                ? null
                : remapDegreesForDisplayRotation(freshSample.headingDegrees, displayRotation);
    }

    @Nullable
    public static Float headingAccuracyDegrees(
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            boolean monitoringActive,
            long nowElapsedRealtimeMs
    ) {
        GeomagneticOrientationMonitor.Sample freshSample = freshSample(sample, monitoringActive, nowElapsedRealtimeMs);
        Double headingAccuracyDegrees = freshSample == null
                ? null
                : freshSample.effectiveHeadingAccuracyDegrees(nowElapsedRealtimeMs);
        return headingAccuracyDegrees == null ? null : headingAccuracyDegrees.floatValue();
    }

    public static double remapDegreesForDisplayRotation(double headingDegrees, int displayRotation) {
        double rotationOffsetDegrees = rotationOffsetDegrees(displayRotation);
        double normalized = (headingDegrees + rotationOffsetDegrees) % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }

    @Nullable
    private static GeomagneticOrientationMonitor.Sample freshSample(
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            boolean monitoringActive,
            long nowElapsedRealtimeMs
    ) {
        if (!monitoringActive || sample == null) {
            return null;
        }
        return nowElapsedRealtimeMs - sample.elapsedRealtimeMs > MAX_SAMPLE_AGE_MS ? null : sample;
    }

    private static double rotationOffsetDegrees(int displayRotation) {
        switch (displayRotation) {
            case DisplayRotation.ROTATION_90:
                return 90.0;
            case DisplayRotation.ROTATION_180:
                return 180.0;
            case DisplayRotation.ROTATION_270:
                return 270.0;
            case DisplayRotation.ROTATION_0:
            default:
                return 0.0;
        }
    }
}
