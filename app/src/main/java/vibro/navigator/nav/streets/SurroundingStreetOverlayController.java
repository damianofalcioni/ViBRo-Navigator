package vibro.navigator.nav.streets;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.settings.AppCompassSettings;

public final class SurroundingStreetOverlayController {
    private static final int MAX_DISPLAY_STREET_SEGMENTS = 2_000;
    private static final int MAX_LOAD_CHUNKS_PER_REQUEST = 64;

    @NonNull
    private final Context appContext;
    @NonNull
    private final TaskScheduler resultScheduler;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    @NonNull
    private final Runnable stateEmitter;
    @NonNull
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @NonNull
    private final SurroundingStreetRefreshPolicy refreshPolicy = new SurroundingStreetRefreshPolicy();
    @NonNull
    private final SurroundingStreetChunkPlanner chunkPlanner = new SurroundingStreetChunkPlanner();
    @NonNull
    private final SurroundingStreetOverlayCache overlayCache = new SurroundingStreetOverlayCache();
    @NonNull
    private final SurroundingStreetChunkLoader chunkLoader;

    @NonNull
    private CompassStreetOverlay overlay = CompassStreetOverlay.EMPTY;
    @NonNull
    private SurroundingStreetChunkSelection activeSelection = SurroundingStreetChunkSelection.EMPTY;
    private NavigationLocation lastAcceptedLocation;
    private NavCompassState lastCompassState;
    private NavigationLocation lastRefreshLocation;
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
        this.stateEmitter = stateEmitter;
        chunkLoader = new SurroundingStreetChunkLoader(appContext, repository);
    }

    public void reset() {
        generation++;
        inFlight = false;
        overlayCache.clear();
        clearViewportState();
        lastAcceptedLocation = null;
        lastCompassState = null;
        lastRefreshLocation = null;
        lastRefreshElapsedMs = 0L;
    }

    public void onAcceptedLocation(@NonNull NavigationLocation location) {
        if (shutdown) {
            return;
        }
        lastAcceptedLocation = new NavigationLocation(location);
        rebuildSelection();
        requestOverlayIfNeeded();
    }

    public void onCompassViewport(@NonNull NavCompassState compassState) {
        if (shutdown) {
            return;
        }
        if (!AppCompassSettings.isSurroundingStreetsEnabled(appContext)) {
            clearAll();
            return;
        }
        if (!chunkPlanner.shouldShow(compassState)) {
            clearViewportState();
            return;
        }
        viewportActive = true;
        lastCompassState = compassState;
        rebuildSelection();
        requestOverlayIfNeeded();
    }

    public void clearCompassViewport() {
        clearViewportState();
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

    private void rebuildSelection() {
        if (!viewportActive || lastCompassState == null) {
            return;
        }
        activeSelection = chunkPlanner.select(lastCompassState, lastAcceptedLocation);
        overlay = overlayCache.overlayFor(
                activeSelection.displayKeys,
                MAX_DISPLAY_STREET_SEGMENTS,
                lastCompassState.displayMode.referenceSpeedMps
        );
    }

    private void requestOverlayIfNeeded() {
        if (shutdown || inFlight || !isReadyForLoading()) {
            return;
        }
        List<SurroundingStreetChunkKey> urgentMissing =
                overlayCache.missing(activeSelection.displayKeys, MAX_LOAD_CHUNKS_PER_REQUEST);
        if (!urgentMissing.isEmpty()) {
            requestChunks(urgentMissing, elapsedRealtimeClock.elapsedRealtimeMs(), false);
            return;
        }
        requestPrefetchIfNeeded();
    }

    private void requestPrefetchIfNeeded() {
        NavigationLocation location = lastAcceptedLocation;
        long nowElapsedMs = elapsedRealtimeClock.elapsedRealtimeMs();
        if (!refreshPolicy.shouldRefresh(
                location,
                lastRefreshLocation,
                lastRefreshElapsedMs,
                nowElapsedMs,
                false
        )) {
            return;
        }
        List<SurroundingStreetChunkKey> missing =
                overlayCache.missing(activeSelection.prefetchKeys, MAX_LOAD_CHUNKS_PER_REQUEST);
        if (!missing.isEmpty()) {
            requestChunks(missing, nowElapsedMs, true);
        }
    }

    private boolean isReadyForLoading() {
        return AppCompassSettings.isSurroundingStreetsEnabled(appContext)
                && viewportActive
                && lastAcceptedLocation != null
                && !activeSelection.isEmpty();
    }

    private void requestChunks(
            @NonNull List<SurroundingStreetChunkKey> keys,
            long nowElapsedMs,
            boolean prefetchRefresh
    ) {
        NavigationLocation requestLocation = new NavigationLocation(lastAcceptedLocation);
        int requestGeneration = ++generation;
        inFlight = true;
        executor.execute(() -> {
            SurroundingStreetChunkLoadResult loaded = chunkLoader.load(keys);
            resultScheduler.post(() -> applyChunks(
                    requestGeneration,
                    requestLocation,
                    nowElapsedMs,
                    prefetchRefresh,
                    loaded
            ));
        });
    }

    private void applyChunks(
            int requestGeneration,
            @NonNull NavigationLocation requestLocation,
            long requestElapsedMs,
            boolean prefetchRefresh,
            @NonNull SurroundingStreetChunkLoadResult loaded
    ) {
        if (shutdown || requestGeneration != generation) {
            return;
        }
        inFlight = false;
        if (prefetchRefresh) {
            lastRefreshLocation = requestLocation;
            lastRefreshElapsedMs = requestElapsedMs;
        }
        loaded.putInto(overlayCache);
        rebuildSelection();
        stateEmitter.run();
        requestOverlayIfNeeded();
    }

    private void clearAll() {
        generation++;
        inFlight = false;
        overlayCache.clear();
        clearViewportState();
        lastRefreshLocation = null;
        lastRefreshElapsedMs = 0L;
    }

    private void clearViewportState() {
        viewportActive = false;
        activeSelection = SurroundingStreetChunkSelection.EMPTY;
        overlay = CompassStreetOverlay.EMPTY;
        overlayCache.resetSpeedBucket();
    }
}
