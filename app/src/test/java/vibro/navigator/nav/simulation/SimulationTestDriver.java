package vibro.navigator.nav.simulation;

import java.util.List;
import java.util.ArrayList;

import vibro.navigator.dispatch.TaskScheduler;

/** Deterministic scheduler: no sleeps, Android framework or real GPS in simulation policy tests. */
public final class SimulationTestDriver implements TaskScheduler {
    public final NavigationSimulator source;
    private long nowMs = 100_000;
    private final List<Runnable> pending = new ArrayList<>();

    public SimulationTestDriver(NavigationSimulator.PathSource path, NavigationSimulator.LocationSink output) {
        source = new NavigationSimulator(this, this::nowMs, path, output);
    }

    public long nowMs() { return nowMs; }

    public void advance(long milliseconds) {
        nowMs += milliseconds;
        List<Runnable> ready = new ArrayList<>(pending);
        pending.clear();
        for (Runnable runnable : ready) { runnable.run(); }
    }

    @Override
    public void post(Runnable runnable) { pending.add(runnable); }

    @Override
    public void postDelayed(Runnable runnable, long delayMs) { pending.add(runnable); }

    @Override
    public void removeCallbacks(Runnable runnable) {
        pending.removeAll(java.util.Collections.singletonList(runnable));
    }
}
