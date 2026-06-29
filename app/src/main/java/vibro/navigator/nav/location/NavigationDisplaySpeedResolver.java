package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;

final class NavigationDisplaySpeedResolver {
    private static final float MAX_STATIONARY_REPORTED_SPEED_MPS = 0.35f;
    private static final float MAX_UNCONFIRMED_LOW_SPEED_MPS = 2.5f;
    private static final double MAX_STATIONARY_RECENT_DISTANCE_METERS = 0.8;
    private static final double MIN_LOW_SPEED_CONFIRMATION_DISTANCE_METERS = 1.5;
    private static final double LOW_SPEED_CONFIRMATION_ACCURACY_FACTOR = 0.4;
    private static final double MAX_LOW_SPEED_CONFIRMATION_DISTANCE_METERS = 8.0;
    private static final float MAX_TRUSTED_LOW_SPEED_BEARING_ACCURACY_DEGREES = 25f;
    private static final long MIN_FALLBACK_DISPLAY_SPEED_ELAPSED_MS = 2_000L;
    private static final float MAX_FALLBACK_DISPLAY_SPEED_ACCURACY_METERS = 25f;

    private NavigationDisplaySpeedResolver() {
    }

    static float resolve(
            @NonNull NavigationLocation location,
            @Nullable NavigationLocation previousFiltered,
            boolean stationaryRecentMotion,
            float speedMps
    ) {
        if (isSuppressedBaseSpeed(stationaryRecentMotion, speedMps)) {
            return 0f;
        }
        if (!location.hasSpeed()) {
            return resolveFallbackSpeed(location, previousFiltered, speedMps);
        }
        return resolveReportedSpeed(location, previousFiltered, speedMps);
    }

    private static boolean isSuppressedBaseSpeed(boolean stationaryRecentMotion, float speedMps) {
        return stationaryRecentMotion
                || !Float.isFinite(speedMps)
                || speedMps <= MAX_STATIONARY_REPORTED_SPEED_MPS;
    }

    private static float resolveReportedSpeed(
            @NonNull NavigationLocation location,
            @Nullable NavigationLocation previousFiltered,
            float speedMps
    ) {
        if (!hasBasicMovementEvidence(previousFiltered, location)) {
            return 0f;
        }
        if (speedMps < MAX_UNCONFIRMED_LOW_SPEED_MPS
                && !hasLowSpeedMovementEvidence(previousFiltered, location)) {
            return 0f;
        }
        return speedMps;
    }

    private static float resolveFallbackSpeed(
            @NonNull NavigationLocation location,
            @Nullable NavigationLocation previousFiltered,
            float speedMps
    ) {
        if (!hasFallbackMovementEvidence(previousFiltered, location)) {
            return 0f;
        }
        return speedMps;
    }

    private static boolean hasFallbackMovementEvidence(
            @Nullable NavigationLocation previousFiltered,
            @NonNull NavigationLocation location
    ) {
        return hasBasicMovementEvidence(previousFiltered, location)
                && hasEnoughFallbackElapsedTime(previousFiltered, location)
                && hasAccurateFallbackFixes(previousFiltered, location);
    }

    private static boolean hasEnoughFallbackElapsedTime(
            @Nullable NavigationLocation previousFiltered,
            @NonNull NavigationLocation location
    ) {
        return previousFiltered != null
                && location.getElapsedRealtimeOrTimeMs() - previousFiltered.getElapsedRealtimeOrTimeMs()
                >= MIN_FALLBACK_DISPLAY_SPEED_ELAPSED_MS;
    }

    private static boolean hasAccurateFallbackFixes(
            @Nullable NavigationLocation previousFiltered,
            @NonNull NavigationLocation location
    ) {
        return previousFiltered != null
                && displayableAccuracyMeters(previousFiltered) <= MAX_FALLBACK_DISPLAY_SPEED_ACCURACY_METERS
                && displayableAccuracyMeters(location) <= MAX_FALLBACK_DISPLAY_SPEED_ACCURACY_METERS;
    }

    private static boolean hasBasicMovementEvidence(
            @Nullable NavigationLocation previousFiltered,
            @NonNull NavigationLocation location
    ) {
        return previousFiltered != null
                && distanceMeters(previousFiltered, location) > MAX_STATIONARY_RECENT_DISTANCE_METERS;
    }

    private static boolean hasLowSpeedMovementEvidence(
            @Nullable NavigationLocation previousFiltered,
            @NonNull NavigationLocation location
    ) {
        if (previousFiltered == null) {
            return false;
        }
        double distanceMeters = distanceMeters(previousFiltered, location);
        if (hasTrustedLowSpeedBearingAccuracy(location)) {
            return distanceMeters > MAX_STATIONARY_RECENT_DISTANCE_METERS;
        }
        return distanceMeters >= lowSpeedConfirmationDistanceMeters(previousFiltered, location);
    }

    private static boolean hasTrustedLowSpeedBearingAccuracy(@NonNull NavigationLocation location) {
        if (!location.hasBearingAccuracy()) {
            return false;
        }
        float bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
        return Float.isFinite(bearingAccuracyDegrees)
                && bearingAccuracyDegrees >= 0f
                && bearingAccuracyDegrees <= MAX_TRUSTED_LOW_SPEED_BEARING_ACCURACY_DEGREES;
    }

    private static double lowSpeedConfirmationDistanceMeters(
            @NonNull NavigationLocation first,
            @NonNull NavigationLocation second
    ) {
        float accuracyMeters = Math.max(displayableAccuracyMeters(first), displayableAccuracyMeters(second));
        if (!Float.isFinite(accuracyMeters) || accuracyMeters <= 0f) {
            return MIN_LOW_SPEED_CONFIRMATION_DISTANCE_METERS;
        }
        return Math.max(
                MIN_LOW_SPEED_CONFIRMATION_DISTANCE_METERS,
                Math.min(
                        MAX_LOW_SPEED_CONFIRMATION_DISTANCE_METERS,
                        accuracyMeters * LOW_SPEED_CONFIRMATION_ACCURACY_FACTOR
                )
        );
    }

    private static float displayableAccuracyMeters(@NonNull NavigationLocation location) {
        return location.hasAccuracy()
                && Float.isFinite(location.getAccuracy())
                && location.getAccuracy() > 0f
                ? location.getAccuracy()
                : Float.NaN;
    }

    private static double distanceMeters(
            @NonNull NavigationLocation first,
            @NonNull NavigationLocation second
    ) {
        return GeoMath.distanceMeters(
                first.getLatitude(),
                first.getLongitude(),
                second.getLatitude(),
                second.getLongitude()
        );
    }
}
