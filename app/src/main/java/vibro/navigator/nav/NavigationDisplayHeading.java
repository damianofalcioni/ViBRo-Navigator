package vibro.navigator.nav;

import android.hardware.SensorManager;
import android.view.Surface;

import androidx.annotation.Nullable;

final class NavigationDisplayHeading {

    private static final long MAX_SAMPLE_AGE_MS = 5_000L;

    private NavigationDisplayHeading() {
    }

    @Nullable
    static Double headingDegrees(
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
    static Float headingAccuracyDegrees(
            @Nullable GeomagneticOrientationMonitor.Sample sample,
            boolean monitoringActive,
            long nowElapsedRealtimeMs
    ) {
        GeomagneticOrientationMonitor.Sample freshSample = freshSample(sample, monitoringActive, nowElapsedRealtimeMs);
        return freshSample == null ? null : accuracyDegrees(freshSample);
    }

    static double remapDegreesForDisplayRotation(double headingDegrees, int displayRotation) {
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

    @Nullable
    private static Float accuracyDegrees(GeomagneticOrientationMonitor.Sample sample) {
        if (sample.headingAccuracyDegrees != null) {
            return sample.headingAccuracyDegrees.floatValue();
        }
        switch (sample.accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return 10f;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return 20f;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return 35f;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
            default:
                return null;
        }
    }

    private static double rotationOffsetDegrees(int displayRotation) {
        switch (displayRotation) {
            case Surface.ROTATION_90:
                return 90.0;
            case Surface.ROTATION_180:
                return 180.0;
            case Surface.ROTATION_270:
                return 270.0;
            case Surface.ROTATION_0:
            default:
                return 0.0;
        }
    }
}
