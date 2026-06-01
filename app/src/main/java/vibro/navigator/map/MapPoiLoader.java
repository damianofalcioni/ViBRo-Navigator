package vibro.navigator.map;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;

final class MapPoiLoader {
    private static final String TAG = "MapPoiLoader";

    interface Listener {
        void onCachedPois(@NonNull List<MapPoiMarker> markers);

        void onPoiLoadComplete(@NonNull List<MapPoiMarker> markers);

        void onPoiLoadFailure();
    }

    @NonNull
    private final TaskScheduler mainThreadScheduler;
    @NonNull
    private final MapPoiCache cache = new MapPoiCache();
    @NonNull
    private final OsmMapPoiClient client = new OsmMapPoiClient();
    @NonNull
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int generation;

    MapPoiLoader(@NonNull TaskScheduler mainThreadScheduler) {
        this.mainThreadScheduler = mainThreadScheduler;
    }

    void shutdown() {
        cancel();
        executor.shutdownNow();
    }

    void cancel() {
        generation++;
    }

    void rememberDiscovery(
            @NonNull MapPickerBounds bounds,
            @NonNull OsmMapPoiDiscoveryResult discovery
    ) {
        cache.rememberAll(bounds, discovery.markers);
    }

    void load(
            @NonNull MapPickerBounds bounds,
            @NonNull List<MapPoiCategory> categories,
            @NonNull Listener listener
    ) {
        int loadId = ++generation;
        listener.onCachedPois(cache.visibleMarkers(bounds, categories));
        List<MapPoiFetchRequest> requests = cache.missingRequests(bounds, categories);
        if (requests.isEmpty()) {
            listener.onPoiLoadComplete(cache.visibleMarkers(bounds, categories));
            return;
        }
        fetchMissing(loadId, bounds, categories, requests, listener);
    }

    private void fetchMissing(
            int loadId,
            @NonNull MapPickerBounds bounds,
            @NonNull List<MapPoiCategory> categories,
            @NonNull List<MapPoiFetchRequest> requests,
            @NonNull Listener listener
    ) {
        executor.execute(() -> {
            try {
                fetchRequests(requests);
                List<MapPoiMarker> visible = cache.visibleMarkers(bounds, categories);
                mainThreadScheduler.post(() -> applyResult(loadId, visible, listener));
            } catch (IOException e) {
                AppLogger.w(TAG, "Failed to load map POIs", e);
                mainThreadScheduler.post(() -> applyFailure(loadId, listener));
            }
        });
    }

    private void fetchRequests(@NonNull List<MapPoiFetchRequest> requests) throws IOException {
        for (MapPoiFetchRequest request : requests) {
            List<MapPoiMarker> markers = client.search(
                    request.bounds,
                    Collections.singletonList(request.category)
            );
            cache.remember(request.category, request.bounds, markers);
        }
    }

    private void applyResult(
            int loadId,
            @NonNull List<MapPoiMarker> markers,
            @NonNull Listener listener
    ) {
        if (loadId == generation) {
            listener.onPoiLoadComplete(markers);
        }
    }

    private void applyFailure(int loadId, @NonNull Listener listener) {
        if (loadId == generation) {
            listener.onPoiLoadFailure();
        }
    }
}
