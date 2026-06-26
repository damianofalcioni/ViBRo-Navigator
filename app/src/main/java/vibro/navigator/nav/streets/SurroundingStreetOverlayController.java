package vibro.navigator.nav.streets;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.settings.AppCompassSettings;

public final class SurroundingStreetOverlayController {
    private static final String TAG = "SurroundingStreets";
    private static final double STREET_RADIUS_METERS = 700.0d;
    private static final int MAX_STREET_SEGMENTS = 2_000;

    @NonNull
    private final Context appContext;
    @NonNull
    private final TaskScheduler resultScheduler;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    @NonNull
    private final BRouterSegmentsRepository repository;
    @NonNull
    private final Runnable stateEmitter;
    @NonNull
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @NonNull
    private final SurroundingStreetRefreshPolicy refreshPolicy = new SurroundingStreetRefreshPolicy();

    @NonNull
    private CompassStreetOverlay overlay = CompassStreetOverlay.EMPTY;
    private NavigationLocation lastRefreshLocation;
    private long lastRefreshElapsedMs;
    private boolean inFlight;
    private int generation;
    private boolean shutdown;

    public SurroundingStreetOverlayController(
            @NonNull Context context,
            @NonNull TaskScheduler resultScheduler,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock,
            @NonNull BRouterSegmentsRepository repository,
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
        lastRefreshLocation = null;
        lastRefreshElapsedMs = 0L;
    }

    public void onAcceptedLocation(@NonNull NavigationLocation location) {
        if (shutdown) {
            return;
        }
        if (!AppCompassSettings.isSurroundingStreetsEnabled(appContext)) {
            clearIfNeeded();
            return;
        }
        long nowElapsedMs = elapsedRealtimeClock.elapsedRealtimeMs();
        if (!refreshPolicy.shouldRefresh(location, lastRefreshLocation, lastRefreshElapsedMs, nowElapsedMs, inFlight)) {
            return;
        }
        requestOverlay(location, nowElapsedMs);
    }

    @NonNull
    public CompassStreetOverlay currentOverlay() {
        return AppCompassSettings.isSurroundingStreetsEnabled(appContext)
                ? overlay
                : CompassStreetOverlay.EMPTY;
    }

    public void shutdown() {
        shutdown = true;
        generation++;
        executor.shutdownNow();
    }

    private void requestOverlay(@NonNull NavigationLocation location, long nowElapsedMs) {
        NavigationLocation requestLocation = new NavigationLocation(location);
        int requestGeneration = ++generation;
        inFlight = true;
        executor.execute(() -> {
            CompassStreetOverlay loaded = loadOverlay(requestLocation);
            resultScheduler.post(() -> applyOverlay(requestGeneration, requestLocation, nowElapsedMs, loaded));
        });
    }

    @NonNull
    private CompassStreetOverlay loadOverlay(@NonNull NavigationLocation location) {
        try {
            return repository.loadSurroundingStreets(
                    appContext,
                    location.getLatitude(),
                    location.getLongitude(),
                    STREET_RADIUS_METERS,
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
            long requestElapsedMs,
            @NonNull CompassStreetOverlay loaded
    ) {
        if (shutdown || requestGeneration != generation) {
            return;
        }
        inFlight = false;
        lastRefreshLocation = requestLocation;
        lastRefreshElapsedMs = requestElapsedMs;
        overlay = AppCompassSettings.isSurroundingStreetsEnabled(appContext)
                ? loaded
                : CompassStreetOverlay.EMPTY;
        stateEmitter.run();
    }

    private void clearIfNeeded() {
        generation++;
        inFlight = false;
        lastRefreshLocation = null;
        lastRefreshElapsedMs = 0L;
        overlay = CompassStreetOverlay.EMPTY;
    }
}
