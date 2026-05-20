package vibro.navigator.nav.session;

import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

final class NavigationGpsBearingTrustPolicy {
    private static final float MIN_TRUSTED_GPS_BEARING_SPEED_MPS = 0.8f;
    private static final float MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS = 2.5f;
    private static final float MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES = 25f;

    @Nullable
    Double trustedBearingDegrees(@NonNull Location location, float speedMps) {
        if (!location.hasBearing()) {
            return null;
        }
        if (speedMps < MIN_TRUSTED_GPS_BEARING_SPEED_MPS) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy()) {
            return hasTrustedBearingAccuracy(location) ? (double) location.getBearing() : null;
        }
        return speedMps >= MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS
                ? (double) location.getBearing()
                : null;
    }

    @Nullable
    Float currentBearingAccuracyDegrees(@NonNull Location location) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !location.hasBearingAccuracy()) {
            return null;
        }
        float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
        return Float.isFinite(bearingAccuracyDegrees) && bearingAccuracyDegrees >= 0f
                ? bearingAccuracyDegrees
                : null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static boolean hasTrustedBearingAccuracy(@NonNull Location location) {
        float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
        return Float.isFinite(bearingAccuracyDegrees)
                && bearingAccuracyDegrees >= 0f
                && bearingAccuracyDegrees <= MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES;
    }
}
