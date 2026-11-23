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

    boolean startIfEnabled(Runnable requestOrigin) {
        if (!enabled()) { return false; }
        source.start();
        if (needsOrigin()) {
            requestOrigin.run();
        }
        return true;
    }

    boolean needsOrigin() { return source != null && source.isWaitingForOrigin(); }

    boolean suppressPhysicalSeed(boolean suspended) { return suspended || enabled(); }

    boolean allowGnss(boolean allowed) { return allowed && !enabled(); }

    @Nullable
    NavigationLocation startup(StartupSeed physicalSeed, Runnable finishOrigin) {
        if (!enabled()) { return physicalSeed.read(); }
        if (needsOrigin() && source.seed(physicalSeed.read())) {
            finishOrigin.run();
        }
        return source.currentFix();
    }
}
