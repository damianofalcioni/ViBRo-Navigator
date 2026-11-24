package vibro.navigator.nav.location;

import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.simulation.NavigationSimulator;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

/** Seed acquisition stays separate from ongoing subscription and suspension state. */
final class NavigationLocationSeeds {
    private final NavigationLocationProvider providers;
    private final FusedLocationUpdateClient fused;
    private final ElapsedRealtimeClock clock;

    NavigationLocationSeeds(NavigationLocationProvider providers, FusedLocationUpdateClient fused,
            ElapsedRealtimeClock clock) {
        this.providers = providers;
        this.fused = fused;
        this.clock = clock;
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

    void requestSimulationOrigin(boolean preferFused) {
        boolean fine = providers.hasFineLocationPermission();
        boolean coarse = providers.hasCoarseLocationPermission();
        if (!NavigationLocationProviders.hasAnyLocationPermission(fine, coarse)) {
            return;
        }
        // Keep receiving fixes until one is usable, including on platforms without getCurrentLocation.
        providers.requestProviderUpdates(providers.enabledPermittedProviders(fine, coarse),
                NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS);
        if (preferFused) {
            fused.requestCurrentLocationSeed(fine, coarse);
        }
    }

    void finishSimulationOrigin() {
        providers.removeUpdates();
        fused.cancelCurrentLocationSeed();
    }

    @Nullable
    NavigationLocation bestStartup(long nowMs, boolean simulationOrigin) {
        boolean fine = providers.hasFineLocationPermission();
        boolean coarse = providers.hasCoarseLocationPermission();
        if (!NavigationLocationProviders.hasAnyLocationPermission(fine, coarse)) {
            return null;
        }
        long selectionTimeMs = simulationOrigin ? clock.elapsedRealtimeMs() : nowMs;
        NavigationLocation best = LastKnownLocationSelector.findBestStartup(
                provider -> lastKnownStartupCandidate(provider, selectionTimeMs, simulationOrigin),
                fine, coarse, selectionTimeMs);
        AppLogger.d("NavLocation", "Best last known NavigationLocation=" + NavigationLocationFormatter.format(best));
        return best;
    }

    @Nullable
    private NavigationLocation lastKnownStartupCandidate(String provider, long nowMs, boolean simulationOrigin) {
        NavigationLocation fix = providers.getLastKnownLocationQuietly(provider);
        return !simulationOrigin || NavigationSimulator.isUsableOrigin(fix, nowMs) ? fix : null;
    }
}
