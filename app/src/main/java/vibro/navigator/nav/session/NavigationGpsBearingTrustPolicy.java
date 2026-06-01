package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NavigationGpsBearingTrustPolicy {
    private static final float MIN_TRUSTED_GPS_BEARING_SPEED_MPS = 0.8f;
    private static final float MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS = 2.5f;
    private static final float MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES = 25f;

    @Nullable
    Double trustedBearingDegrees(@NonNull NavigationLocation location, float speedMps) {
        if (!location.hasBearing()) {
            return null;
        }
        if (speedMps < MIN_TRUSTED_GPS_BEARING_SPEED_MPS) {
            return null;
        }
        if (location.hasBearingAccuracy()) {
            return hasTrustedBearingAccuracy(location) ? (double) location.getBearing() : null;
        }
        return speedMps >= MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS
                ? (double) location.getBearing()
                : null;
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
