package vibro.navigator.nav.streets;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.settings.AppCompassSettings;

public final class SurroundingStreetOverlayController {
    private static final String TAG = "SurroundingStreets";
    private static final int MAX_STREET_SEGMENTS = 2_000;

    @NonNull
    private final Context appContext;
    @NonNull
    private final TaskScheduler resultScheduler;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    @NonNull
    private final SurroundingStreetRepository repository;
    @NonNull
    private final Runnable stateEmitter;
    @NonNull
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @NonNull
    private final SurroundingStreetRefreshPolicy refreshPolicy = new SurroundingStreetRefreshPolicy();
    @NonNull
    private final SurroundingStreetViewportPolicy viewportPolicy = new SurroundingStreetViewportPolicy();

    @NonNull
    private CompassStreetOverlay overlay = CompassStreetOverlay.EMPTY;
    private NavigationLocation lastAcceptedLocation;
    private NavigationLocation lastRefreshLocation;
    private double activeRadiusMeters;
    private double lastRefreshRadiusMeters;
    private long lastRefreshElapsedMs;
    private boolean inFlight;
    private int generation;
    private boolean viewportActive;
    private boolean shutdown;

    public SurroundingStreetOverlayController(
            @NonNull Context context,
            @NonNull TaskScheduler resultScheduler,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock,
            @NonNull SurroundingStreetRepository repository,
            @NonNull Runnable stateEmitter
    ) {
        appContext = context.getApplicationContext();
        this.resultScheduler = resultScheduler;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        this.repository = repository;
        this.stateEmitter = stateEmitter;
    }

    public void reset() {
        generation++;
        inFlight = false;
        overlay = CompassStreetOverlay.EMPTY;
        lastAcceptedLocation = null;
        lastRefreshLocation = null;
        activeRadiusMeters = 0.0d;
        lastRefreshRadiusMeters = 0.0d;
        lastRefreshElapsedMs = 0L;
        viewportActive = false;
    }

    public void onAcceptedLocation(@NonNull NavigationLocation location) {
        if (shutdown) {
            return;
        }
        lastAcceptedLocation = new NavigationLocation(location);
        requestOverlayIfNeeded();
    }

    public void onCompassViewport(@NonNull NavCompassState compassState) {
        if (shutdown) {
            return;
        }
        if (!AppCompassSettings.isSurroundingStreetsEnabled(appContext)) {
            clearIfNeeded();
            return;
        }
        if (!viewportPolicy.shouldShow(compassState)) {
            clearIfNeeded();
            return;
        }
        viewportActive = true;
        activeRadiusMeters = viewportPolicy.extractionRadiusMeters(compassState);
        requestOverlayIfNeeded();
    }

    public void clearCompassViewport() {
        clearIfNeeded();
    }

    private void requestOverlayIfNeeded() {
        if (shutdown) {
            return;
        }
        if (!AppCompassSettings.isSurroundingStreetsEnabled(appContext)) {
            clearIfNeeded();
            return;
        }
        NavigationLocation location = lastAcceptedLocation;
        if (!viewportActive || location == null) {
            return;
        }
        long nowElapsedMs = elapsedRealtimeClock.elapsedRealtimeMs();
        if (!refreshPolicy.shouldRefresh(
                location,
                lastRefreshLocation,
                lastRefreshRadiusMeters,
                activeRadiusMeters,
                lastRefreshElapsedMs,
                nowElapsedMs,
                inFlight
        )) {
            return;
        }
        requestOverlay(location, activeRadiusMeters, nowElapsedMs);
    }

    @NonNull
    public CompassStreetOverlay currentOverlay() {
        return AppCompassSettings.isSurroundingStreetsEnabled(appContext) && viewportActive
                ? overlay
                : CompassStreetOverlay.EMPTY;
    }

    public void shutdown() {
        shutdown = true;
        generation++;
        executor.shutdownNow();
    }

    private void requestOverlay(
            @NonNull NavigationLocation location,
            double radiusMeters,
            long nowElapsedMs
    ) {
        NavigationLocation requestLocation = new NavigationLocation(location);
        int requestGeneration = ++generation;
        inFlight = true;
        executor.execute(() -> {
            CompassStreetOverlay loaded = loadOverlay(requestLocation, radiusMeters);
            resultScheduler.post(() -> applyOverlay(
                    requestGeneration,
                    requestLocation,
                    radiusMeters,
                    nowElapsedMs,
                    loaded
            ));
        });
    }

    @NonNull
    private CompassStreetOverlay loadOverlay(@NonNull NavigationLocation location, double radiusMeters) {
        try {
            return repository.loadSurroundingStreets(
                    appContext,
                    location.getLatitude(),
                    location.getLongitude(),
                    radiusMeters,
                    MAX_STREET_SEGMENTS
            );
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to load surrounding streets", e);
            return CompassStreetOverlay.EMPTY;
        }
    }

    private void applyOverlay(
            int requestGeneration,
            @NonNull NavigationLocation requestLocation,
            double requestRadiusMeters,
            long requestElapsedMs,
            @NonNull CompassStreetOverlay loaded
    ) {
        if (shutdown || requestGeneration != generation) {
            return;
        }
        inFlight = false;
        lastRefreshLocation = requestLocation;
        lastRefreshRadiusMeters = requestRadiusMeters;
        lastRefreshElapsedMs = requestElapsedMs;
        overlay = AppCompassSettings.isSurroundingStreetsEnabled(appContext) && viewportActive
                ? loaded
                : CompassStreetOverlay.EMPTY;
        stateEmitter.run();
        requestOverlayIfNeeded();
    }

    private void clearIfNeeded() {
        generation++;
        inFlight = false;
        lastRefreshLocation = null;
        activeRadiusMeters = 0.0d;
        lastRefreshRadiusMeters = 0.0d;
        lastRefreshElapsedMs = 0L;
        viewportActive = false;
        overlay = CompassStreetOverlay.EMPTY;
    }
}
