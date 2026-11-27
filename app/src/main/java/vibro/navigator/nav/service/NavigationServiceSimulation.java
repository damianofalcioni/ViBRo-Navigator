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
    private boolean phoneStartNoticePending;
    private boolean carStartNoticePending;

    NavigationServiceSimulation(Context context, NavigationSession session, ElapsedRealtimeClock clock,
            NavigationSimulator.LocationSink output) {
        this.context = context;
        source = new NavigationSimulator(AndroidTaskScheduler.main(), clock,
                () -> NavigationSessionSimulation.path(session), output);
    }

    NavigationSimulator source() { return source; }

    boolean ignorePhysicalProviders() { return source.isEnabled(); }

    boolean acceptPhysical(NavigationLocation location, NavigationSimulator.LocationSink output) {
        return source.acceptPhysical(location, output);
    }

    void prepare() {
        clearStartNotices();
        source.configure(autoDrive || AppSimulationSettings.isEnabled(context), AppSimulationSettings.read(context));
    }

    void onNavigationStarted() {
        phoneStartNoticePending = source.isEnabled();
        carStartNoticePending = phoneStartNoticePending;
    }

    boolean consumePhoneStartNotice() {
        boolean pending = phoneStartNoticePending;
        phoneStartNoticePending = false;
        return pending;
    }

    boolean consumeCarStartNotice() {
        boolean pending = carStartNoticePending;
        carStartNoticePending = false;
        return pending;
    }

    void enable(Runnable restart) {
        if (autoDrive) { return; }
        autoDrive = true;
        if (!source.isEnabled()) { restart.run(); }
    }

    void stop() {
        autoDrive = false;
        clearStartNotices();
        source.pause();
    }

    private void clearStartNotices() {
        phoneStartNoticePending = false;
        carStartNoticePending = false;
    }
}
