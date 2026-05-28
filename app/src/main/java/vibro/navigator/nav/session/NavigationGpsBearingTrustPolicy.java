package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

final class NavigationGpsBearingTrustPolicy {
    private static final float MIN_TRUSTED_GPS_BEARING_SPEED_MPS = 0.8f;
    private static final float MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS = 2.5f;
    private static final float MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES = 25f;

    @Nullable
    Double trustedBearingDegrees(@NonNull NavigationLocation NavigationLocation, float speedMps) {
        if (!NavigationLocation.hasBearing()) {
            return null;
        }
        if (speedMps < MIN_TRUSTED_GPS_BEARING_SPEED_MPS) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && NavigationLocation.hasBearingAccuracy()) {
            return hasTrustedBearingAccuracy(NavigationLocation) ? (double) NavigationLocation.getBearing() : null;
        }
        return speedMps >= MIN_GPS_BEARING_SPEED_WITHOUT_ACCURACY_MPS
                ? (double) NavigationLocation.getBearing()
                : null;
    }

    @Nullable
    Float currentBearingAccuracyDegrees(@NonNull NavigationLocation NavigationLocation) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !NavigationLocation.hasBearingAccuracy()) {
            return null;
        }
        float bearingAccuracyDegrees = NavigationLocation.getBearingAccuracyDegrees();
        return Float.isFinite(bearingAccuracyDegrees) && bearingAccuracyDegrees >= 0f
                ? bearingAccuracyDegrees
                : null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static boolean hasTrustedBearingAccuracy(@NonNull NavigationLocation NavigationLocation) {
        float bearingAccuracyDegrees = NavigationLocation.getBearingAccuracyDegrees();
        return Float.isFinite(bearingAccuracyDegrees)
                && bearingAccuracyDegrees >= 0f
                && bearingAccuracyDegrees <= MAX_TRUSTED_GPS_BEARING_ACCURACY_DEGREES;
    }
}
