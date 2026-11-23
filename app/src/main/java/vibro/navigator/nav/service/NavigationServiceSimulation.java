package vibro.navigator.nav.service;

import android.content.Context;

import vibro.navigator.nav.simulation.NavigationSimulator;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionSimulation;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.settings.AppSimulationSettings;

/** Manual preferences are sampled on start; host auto-drive is scoped to this service session. */
final class NavigationServiceSimulation {
    private final Context context;
    private final NavigationSimulator source;
    private boolean autoDrive;
    private boolean startNoticePending;

    NavigationServiceSimulation(Context context, NavigationSession session, ElapsedRealtimeClock clock,
            NavigationSimulator.LocationSink output) {
        this.context = context;
        source = new NavigationSimulator(AndroidTaskScheduler.main(), clock,
                () -> NavigationSessionSimulation.path(session), output);
    }

    NavigationSimulator source() { return source; }

    boolean ignorePhysicalProviders() { return source.isEnabled(); }

    void acceptPhysical(NavigationLocation location, NavigationSimulator.LocationSink output) {
        source.acceptPhysical(location, output);
    }

    void prepare() {
        startNoticePending = false;
        source.configure(autoDrive || AppSimulationSettings.isEnabled(context), AppSimulationSettings.read(context));
    }

    void onNavigationStarted() {
        startNoticePending = source.isEnabled();
    }

    boolean consumeStartNotice() {
        boolean pending = startNoticePending;
        startNoticePending = false;
        return pending;
    }

    void enable(Runnable restart) {
        if (autoDrive) { return; }
        autoDrive = true;
        if (!source.isEnabled()) { restart.run(); }
    }

    void stop() {
        autoDrive = false;
        startNoticePending = false;
        source.pause();
    }
}
