package vibro.navigator.nav.startup;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationStartupLocationSelector {

    private static final long MAX_AGE_MS = 15_000L;
    private static final float MAX_ACCURACY_METERS = 50f;
    private static final float MAX_ROUTE_START_ACCURACY_METERS = 25f;

    private NavigationStartupLocationSelector() {
    }

    @Nullable
    public static Location selectBest(@Nullable Location gps, @Nullable Location network, long nowMs) {
        Location best = null;
        if (isUsable(gps, nowMs)) {
            best = gps;
        }
        if (isUsable(network, nowMs) && isBetterThan(network, best)) {
            best = network;
        }
        return best == null ? null : new Location(best);
    }

    public static boolean isUsable(@Nullable Location location, long nowMs) {
        if (location == null) {
            return false;
        }
        long ageMs = Math.max(0L, nowMs - location.getTime());
        return ageMs <= MAX_AGE_MS
                && accuracyMeters(location) <= MAX_ACCURACY_METERS;
    }

    public static boolean isUsableForRouteStart(@Nullable Location location, long nowMs) {
        if (location == null) {
            return false;
        }
        long ageMs = Math.max(0L, nowMs - location.getTime());
        return ageMs <= MAX_AGE_MS
                && accuracyMeters(location) <= MAX_ROUTE_START_ACCURACY_METERS;
    }

    private static boolean isBetterThan(@NonNull Location candidate, @Nullable Location currentBest) {
        return currentBest == null
                || candidate.getTime() > currentBest.getTime()
                || (candidate.getTime() == currentBest.getTime()
                && accuracyMeters(candidate) < accuracyMeters(currentBest));
    }

    private static float accuracyMeters(@NonNull Location location) {
        return location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }
}
