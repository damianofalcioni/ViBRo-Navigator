package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NavigationGpsBearingTrustPolicy {
    private static final float MIN_TRUSTED_GPS_BEARING_SPEED_MPS = 0.8f;
    private static final float MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS = 2.5f;
    private static final float MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES = 25f;
    private static final float MIN_DISPLAY_BEARING_SPEED_MPS = 0.35f;

    @Nullable
    Double trustedBearingDegrees(@NonNull NavigationLocation location, float speedMps) {
        return trustedBearingDegrees(location, speedMps, MIN_TRUSTED_GPS_BEARING_SPEED_MPS);
    }

    @Nullable
    Double trustedDisplayBearingDegrees(@NonNull NavigationLocation location, float speedMps) {
        // Display course can follow slow movement; reroute evidence keeps its stricter speed gate.
        return trustedBearingDegrees(location, speedMps, MIN_DISPLAY_BEARING_SPEED_MPS);
    }

    @Nullable
    private Double trustedBearingDegrees(@NonNull NavigationLocation location, float speedMps, float minimumSpeedMps) {
        if (!hasUsableBearing(location)) {
            return null;
        }
        if (!Float.isFinite(speedMps) || speedMps < minimumSpeedMps) {
            return null;
        }
        if (location.hasBearingAccuracy()) {
            return hasTrustedBearingAccuracy(location) ? (double) location.getBearing() : null;
        }
        return speedMps >= MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS
                ? (double) location.getBearing()
                : null;
    }

    private static boolean hasUsableBearing(NavigationLocation location) {
        return location.hasBearing() && Float.isFinite(location.getBearing());
    }

    @Nullable
    Float currentBearingAccuracyDegrees(@NonNull NavigationLocation location) {
        if (!location.hasBearingAccuracy()) {
            return null;
        }
        float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
        return Float.isFinite(bearingAccuracyDegrees) && bearingAccuracyDegrees >= 0f
                ? bearingAccuracyDegrees
                : null;
    }

    private static boolean hasTrustedBearingAccuracy(@NonNull NavigationLocation location) {
        float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
        return Float.isFinite(bearingAccuracyDegrees)
                && bearingAccuracyDegrees >= 0f
                && bearingAccuracyDegrees <= MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES;
    }
}
