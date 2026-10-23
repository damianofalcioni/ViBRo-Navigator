package vibro.navigator.nav.startup;

import androidx.annotation.Nullable;

import vibro.navigator.nav.location.LiveLocationCoordinator;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;

/** A validated initial cache seed may precede the active provider's first observation. */
public final class NavigationStartupLocationSeed {
    private NavigationStartupLocationSeed() { }

    @Nullable
    public static LiveLocationCoordinator.Selection select(NavigationLocation seed, long nowMs, int acceptedFixCount,
            @Nullable String activeProviders, LiveLocationCoordinator coordinator) {
        if (acceptedFixCount > 0 || !NavigationStartupLocationSelector.isUsable(seed, nowMs)) {
            return null;
        }
        coordinator.remember(seed);
        LiveLocationCoordinator.Selection selection = coordinator.selectBestLiveSelection(nowMs);
        if (!NavigationLocationProviders.isActive(activeProviders, seed.getProvider())) {
            // Retain dispatch chronology, but never let the cache seed compete with later live fixes.
            coordinator.clearProvider(seed.getProvider());
        }
        return selection;
    }
}
