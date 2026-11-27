package vibro.navigator.nav.simulation;

import androidx.annotation.Nullable;


import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.startup.NavigationStartupLocationSelector;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

/** App-local fixes only: never installs a system mock provider or changes other apps' GPS. */
public final class NavigationSimulator {
    public interface LocationSink { void onLocation(NavigationLocation location); }
    public interface PathSource { NavigationSimulationPath currentPath(); }
    public static final String PROVIDER = "simulation";
    // One-second fixes allow normal guidance/arrival evaluation. Cap sleep gaps to avoid teleporting.
    public static final long FIX_INTERVAL_MS = 1_000L;
    private final TaskScheduler scheduler;
    private final ElapsedRealtimeClock clock;
    private final PathSource path;
    private final LocationSink output;
    private final NavigationSimulationMotion motion = new NavigationSimulationMotion();
    private final Runnable tick = this::tick;
    private NavigationSimulationConfig config = new NavigationSimulationConfig(0, null);
    private boolean enabled;
    private boolean running;
    private long previousTick;
    private long generation;
    @Nullable private LatLon position;

    public NavigationSimulator(TaskScheduler scheduler, ElapsedRealtimeClock clock,
            PathSource path, LocationSink output) {
        this.scheduler = scheduler;
        this.clock = clock;
        this.path = path;
        this.output = output;
    }

    public void configure(boolean enabled, NavigationSimulationConfig config) {
        pause();
        this.enabled = enabled;
        this.config = config;
        position = config.initialPosition;
        motion.reset();
    }

    public boolean isEnabled() { return enabled; }

    public boolean isWaitingForOrigin() { return enabled && position == null; }

    public boolean seed(@Nullable NavigationLocation location) {
        if (!isWaitingForOrigin() || !isUsableOrigin(location, clock.elapsedRealtimeMs())) {
            return false;
        }
        position = new LatLon(location.getLatitude(), location.getLongitude());
        return true;
    }

    public static boolean isUsableOrigin(@Nullable NavigationLocation location, long nowMs) {
        return location != null && location.getAccuracy() > 0
                && location.getElapsedRealtimeOrTimeMs() <= nowMs
                && LatLon.isValidCoordinate(location.getLatitude(), location.getLongitude())
                && NavigationStartupLocationSelector.isUsableForRouteStart(location, nowMs);
    }

    public boolean acceptPhysical(NavigationLocation location, LocationSink realOutput) {
        if (enabled) { return seed(location); }
        realOutput.onLocation(location);
        return false;
    }

    @Nullable
    public NavigationLocation currentFix() {
        return position == null ? null : fix(position, position, 0);
    }

    public void start() {
        if (running) { return; }
        running = true;
        previousTick = clock.elapsedRealtimeMs();
        scheduler.postDelayed(tick, FIX_INTERVAL_MS);
    }

    public void pause() {
        running = false;
        generation++;
        scheduler.removeCallbacks(tick);
    }

    private void tick() {
        if (!running) { return; }
        long activeGeneration = generation;
        long now = clock.elapsedRealtimeMs();
        double seconds = Math.max(0, Math.min(2 * FIX_INTERVAL_MS, now - previousTick)) / 1000.0;
        previousTick = now;
        if (position != null) {
            LatLon before = position;
            position = motion.advance(before, path.currentPath(), config.speedMps * seconds);
            output.onLocation(fix(before, position, seconds));
        }
        if (running && generation == activeGeneration) { scheduler.postDelayed(tick, FIX_INTERVAL_MS); }
    }

    private NavigationLocation fix(LatLon before, LatLon after, double seconds) {
        NavigationLocation fix = new NavigationLocation(PROVIDER);
        fix.setTime(System.currentTimeMillis(), clock.elapsedRealtimeMs());
        fix.setLatitude(after.lat);
        fix.setLongitude(after.lon);
        fix.setAccuracy(1f);
        double distance = NavigationSimulationMotion.distance(before, after);
        fix.setSpeed(seconds > 0 ? (float) (distance / seconds) : 0);
        fix.setSpeedAccuracyMetersPerSecond(0.01f);
        if (distance > 0.01) {
            fix.setBearing((float) GeoMath.bearingDegrees(before.lat, before.lon, after.lat, after.lon));
            fix.setBearingAccuracyDegrees(1f);
        }
        return fix;
    }
}
