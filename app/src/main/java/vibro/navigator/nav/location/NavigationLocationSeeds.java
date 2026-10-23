package vibro.navigator.nav.location;

import vibro.navigator.logging.AppLogger;

/** Seed acquisition stays separate from ongoing subscription and suspension state. */
final class NavigationLocationSeeds {
    private final NavigationLocationProvider providers;
    private final FusedLocationUpdateClient fused;

    NavigationLocationSeeds(NavigationLocationProvider providers, FusedLocationUpdateClient fused) {
        this.providers = providers;
        this.fused = fused;
    }

    void request(boolean suspended, boolean preferFused) {
        if (suspended) {
            return;
        }
        boolean fine = providers.hasFineLocationPermission();
        boolean coarse = providers.hasCoarseLocationPermission();
        if (!NavigationLocationProviders.hasAnyLocationPermission(fine, coarse)) {
            return;
        }
        if (preferFused) {
            fused.requestCurrentLocationSeed(fine, coarse);
        } else {
            providers.requestCurrentLocationSeeds(fine, coarse);
        }
    }

    void requestEnabled(String provider, boolean allowed, boolean suspended) {
        if (allowed && !suspended) {
            providers.requestSeedForEnabledProvider(provider);
        }
    }

    NavigationLocation bestStartup(long nowMs) {
        boolean fine = providers.hasFineLocationPermission();
        boolean coarse = providers.hasCoarseLocationPermission();
        if (!NavigationLocationProviders.hasAnyLocationPermission(fine, coarse)) {
            return null;
        }
        NavigationLocation best = LastKnownLocationSelector.findBestStartup(
                providers::getLastKnownLocationQuietly, fine, coarse, nowMs);
        AppLogger.d("NavLocation", "Best last known NavigationLocation=" + NavigationLocationFormatter.format(best));
        return best;
    }
}
