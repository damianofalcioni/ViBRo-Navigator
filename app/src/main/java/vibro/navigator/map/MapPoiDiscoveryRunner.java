package vibro.navigator.map;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;

final class MapPoiDiscoveryRunner {
    private static final String TAG = "MapPoiDiscoveryRunner";

    interface Listener {
        void onDiscoveryComplete(@NonNull OsmMapPoiDiscoveryResult discovery);

        void onDiscoveryFailure();
    }

    @NonNull
    private final TaskScheduler mainThreadScheduler;
    @NonNull
    private final MapPoiLoader poiLoader;
    @NonNull
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @NonNull
    private final OsmMapPoiClient client = new OsmMapPoiClient();

    private int generation;
    private volatile boolean shutdown;

    MapPoiDiscoveryRunner(
            @NonNull TaskScheduler mainThreadScheduler,
            @NonNull MapPoiLoader poiLoader
    ) {
        this.mainThreadScheduler = mainThreadScheduler;
        this.poiLoader = poiLoader;
    }

    void discoverAll(
            @NonNull MapPickerBounds bounds,
            @NonNull Listener listener
    ) {
        discover(++generation, bounds, listener, () -> client.discover(bounds));
    }

    void discoverFiltered(
            @NonNull MapPickerBounds bounds,
            @NonNull List<MapPoiCategory> categories,
            @NonNull Listener listener
    ) {
        discover(++generation, bounds, listener, () -> client.discover(bounds, categories));
    }

    void shutdown() {
        shutdown = true;
        generation++;
        executor.shutdownNow();
    }

    private void discover(
            int discoveryId,
            @NonNull MapPickerBounds bounds,
            @NonNull Listener listener,
            @NonNull DiscoveryRequest request
    ) {
        if (shutdown) {
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    OsmMapPoiDiscoveryResult discovery = request.fetch();
                    if (!shutdown) {
                        poiLoader.rememberDiscovery(bounds, discovery);
                        mainThreadScheduler.post(() -> applyResult(discoveryId, discovery, listener));
                    }
                } catch (IOException e) {
                    AppLogger.w(TAG, "Failed to discover map POI categories", e);
                    mainThreadScheduler.post(() -> applyFailure(discoveryId, listener));
                }
            });
        } catch (RejectedExecutionException e) {
            AppLogger.w(TAG, "Map POI discovery rejected because the runner is shut down", e);
            mainThreadScheduler.post(() -> applyFailure(discoveryId, listener));
        }
    }

    private void applyResult(
            int discoveryId,
            @NonNull OsmMapPoiDiscoveryResult discovery,
            @NonNull Listener listener
    ) {
        if (!shutdown && discoveryId == generation) {
            listener.onDiscoveryComplete(discovery);
        }
    }

    private void applyFailure(int discoveryId, @NonNull Listener listener) {
        if (!shutdown && discoveryId == generation) {
            listener.onDiscoveryFailure();
        }
    }

    private interface DiscoveryRequest {
        @NonNull
        OsmMapPoiDiscoveryResult fetch() throws IOException;
    }
}
