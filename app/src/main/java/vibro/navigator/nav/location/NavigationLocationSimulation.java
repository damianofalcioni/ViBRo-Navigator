package vibro.navigator.nav.location;

import androidx.annotation.Nullable;


import vibro.navigator.nav.simulation.NavigationSimulator;

/** Optional override policy keeps physical-provider recovery and seeding consistently gated. */
final class NavigationLocationSimulation {
    interface StartupSeed { @Nullable NavigationLocation read(); }
    @Nullable private final NavigationSimulator source;

    NavigationLocationSimulation(@Nullable NavigationSimulator source) { this.source = source; }

    boolean enabled() { return source != null && source.isEnabled(); }

    void pause() {
        if (source != null) { source.pause(); }
    }

    boolean startIfEnabled() {
        if (!enabled()) { return false; }
        source.start();
        return true;
    }

    boolean suppressSeeds(boolean suspended) {
        return suspended || enabled() && source.currentFix() != null;
    }

    boolean suppressProviderSeed(boolean suspended) { return suspended || enabled(); }

    boolean allowGnss(boolean allowed) { return allowed && !enabled(); }

    @Nullable
    NavigationLocation startup(StartupSeed physicalSeed) {
        if (!enabled()) { return physicalSeed.read(); }
        if (source.currentFix() == null) { source.seed(physicalSeed.read()); }
        return source.currentFix();
    }
}
