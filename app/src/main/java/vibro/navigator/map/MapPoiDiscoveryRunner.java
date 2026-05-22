package vibro.navigator.map;

import android.os.Handler;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.logging.AppLogger;

final class MapPoiDiscoveryRunner {
    private static final String TAG = "MapPoiDiscoveryRunner";

    interface Listener {
        void onDiscoveryComplete(@NonNull OsmMapPoiDiscoveryResult discovery);

        void onDiscoveryFailure();
    }

    @NonNull
    private final Handler mainHandler;
    @NonNull
    private final MapPoiLoader poiLoader;
    @NonNull
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @NonNull
    private final OsmMapPoiClient client = new OsmMapPoiClient();

    private int generation;

    MapPoiDiscoveryRunner(
            @NonNull Handler mainHandler,
            @NonNull MapPoiLoader poiLoader
    ) {
        this.mainHandler = mainHandler;
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
        generation++;
        executor.shutdownNow();
    }

    private void discover(
            int discoveryId,
            @NonNull MapPickerBounds bounds,
            @NonNull Listener listener,
            @NonNull DiscoveryRequest request
    ) {
        executor.execute(() -> {
            try {
                OsmMapPoiDiscoveryResult discovery = request.fetch();
                poiLoader.rememberDiscovery(bounds, discovery);
                mainHandler.post(() -> applyResult(discoveryId, discovery, listener));
            } catch (IOException e) {
                AppLogger.w(TAG, "Failed to discover map POI categories", e);
                mainHandler.post(() -> applyFailure(discoveryId, listener));
            }
        });
    }

    private void applyResult(
            int discoveryId,
            @NonNull OsmMapPoiDiscoveryResult discovery,
            @NonNull Listener listener
    ) {
        if (discoveryId == generation) {
            listener.onDiscoveryComplete(discovery);
        }
    }

    private void applyFailure(int discoveryId, @NonNull Listener listener) {
        if (discoveryId == generation) {
            listener.onDiscoveryFailure();
        }
    }

    private interface DiscoveryRequest {
        @NonNull
        OsmMapPoiDiscoveryResult fetch() throws IOException;
    }
}
