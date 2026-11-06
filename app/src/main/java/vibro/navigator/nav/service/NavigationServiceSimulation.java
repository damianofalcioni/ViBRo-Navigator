package vibro.navigator.nav.service;

import android.content.Context;

import vibro.navigator.nav.simulation.NavigationSimulator;
import vibro.navigator.nav.simulation.NavigationSimulationDisplay;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionSimulation;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.R;
import vibro.navigator.settings.AppSimulationSettings;

/** Manual preferences are sampled on start; host auto-drive is scoped to this service session. */
final class NavigationServiceSimulation {
    private final Context context;
    private final NavigationSimulator source;
    private boolean autoDrive;

    NavigationServiceSimulation(Context context, NavigationSession session, ElapsedRealtimeClock clock,
            NavigationSimulator.LocationSink output) {
        this.context = context;
        source = new NavigationSimulator(AndroidTaskScheduler.main(), clock,
                () -> NavigationSessionSimulation.path(session), output);
    }

    NavState label(NavState state) {
        return source.isEnabled() ? NavigationSimulationDisplay.label(state, context.getString(R.string.label_simulation))
                : state;
    }

    NavigationSimulator source() { return source; }

    boolean ignorePhysicalProviders() { return source.isEnabled(); }

    void acceptPhysical(NavigationLocation location, NavigationSimulator.LocationSink output) {
        source.acceptPhysical(location, output);
    }

    void prepare() {
        source.configure(autoDrive || AppSimulationSettings.isEnabled(context), AppSimulationSettings.read(context));
    }

    void enable(Runnable restart) {
        if (autoDrive) { return; }
        autoDrive = true;
        if (!source.isEnabled()) { restart.run(); }
    }

    void stop() {
        autoDrive = false;
        source.pause();
    }
}
