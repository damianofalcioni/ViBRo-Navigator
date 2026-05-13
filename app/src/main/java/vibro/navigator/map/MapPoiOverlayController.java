package vibro.navigator.map;

import android.os.Handler;
import android.os.Looper;
import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;

final class MapPoiOverlayController {
    private static final String TAG = "MapPoiOverlay";
    private static final long REFRESH_DELAY_MS = 850L;
    private static final long DISCOVERY_DELAY_MS = 650L;

    @NonNull
    private final MapPoiOverlayView view;
    @NonNull
    private final MapPickerScriptController scriptController;
    @NonNull
    private final MapPoiCategorySelection selection = new MapPoiCategorySelection();
    @NonNull
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final MapPoiStatusController statusController;
    @NonNull
    private final MapPoiLoader poiLoader;
    @NonNull
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @NonNull
    private final OsmMapPoiClient client = new OsmMapPoiClient();
    @NonNull
    private final Runnable refreshRunnable = this::refreshNow;
    @NonNull
    private final Runnable discoveryRunnable = this::discoverCategoriesNow;

    private int discoveryGeneration;

    MapPoiOverlayController(
            @NonNull MapPoiOverlayView view,
            @NonNull MapPickerScriptController scriptController
    ) {
        this.view = view;
        this.scriptController = scriptController;
        this.statusController = new MapPoiStatusController(view, mainHandler);
        this.poiLoader = new MapPoiLoader(mainHandler);
        view.setPanelListener(this::scheduleCategoryDiscovery);
        renderCategories();
    }

    @NonNull
    static MapPoiOverlayController attach(
            @NonNull Activity activity,
            @NonNull MapPickerScriptController scriptController
    ) {
        return new MapPoiOverlayController(MapPoiOverlayView.bind(activity), scriptController);
    }

    void onMapViewChanged() {
        if (selection.hasEnabledCategories()) {
            scheduleRefresh();
        }
    }

    void shutdown() {
        mainHandler.removeCallbacks(refreshRunnable);
        mainHandler.removeCallbacks(discoveryRunnable);
        statusController.hide();
        poiLoader.shutdown();
        executor.shutdownNow();
    }

    private void renderCategories() {
        view.populateCategories(selection.categories(), selection.enabledCategoryIds(), this::onCategoryChecked);
    }

    private void onCategoryChecked(@NonNull MapPoiCategory category, boolean checked) {
        selection.setChecked(category, checked);
        renderCategories();
        view.hidePanel();
        if (!selection.hasEnabledCategories()) {
            clearPois();
            return;
        }
        refreshNow();
    }

    private void clearPois() {
        mainHandler.removeCallbacks(refreshRunnable);
        statusController.hide();
        poiLoader.cancel();
        scriptController.clearPoiMarkers();
    }

    private void scheduleRefresh() {
        mainHandler.removeCallbacks(refreshRunnable);
        mainHandler.postDelayed(refreshRunnable, REFRESH_DELAY_MS);
    }

    private void scheduleCategoryDiscovery() {
        mainHandler.removeCallbacks(discoveryRunnable);
        mainHandler.postDelayed(discoveryRunnable, DISCOVERY_DELAY_MS);
    }

    private void discoverCategoriesNow() {
        statusController.show(R.string.msg_map_poi_loading);
        scriptController.requestBounds(this::handleDiscoveryBoundsResult);
    }

    private void handleDiscoveryBoundsResult(@Nullable String value) {
        try {
            MapPickerBounds bounds = MapPickerBounds.parseJavascriptResult(value);
            if (bounds == null || !bounds.isReadyForPoiFetch()) {
                selection.clearCategories();
                renderCategories();
                showZoomInMessage();
                return;
            }
            fetchCategories(bounds);
        } catch (JSONException e) {
            AppLogger.w(TAG, "Failed to parse map bounds for POI category discovery", e);
            showUnavailableMessage();
        }
    }

    private void fetchCategories(@NonNull MapPickerBounds bounds) {
        int discoveryId = ++discoveryGeneration;
        executor.execute(() -> {
            try {
                OsmMapPoiDiscoveryResult discovery = client.discover(bounds);
                poiLoader.rememberDiscovery(bounds, discovery);
                mainHandler.post(() -> applyCategoryResult(discoveryId, discovery.categories));
            } catch (IOException e) {
                AppLogger.w(TAG, "Failed to discover map POI categories", e);
                mainHandler.post(() -> applyCategoryFailure(discoveryId));
            }
        });
    }

    private void applyCategoryResult(int discoveryId, @NonNull List<MapPoiCategory> discovered) {
        if (discoveryId != discoveryGeneration) {
            return;
        }
        selection.setCategories(discovered);
        renderCategories();
        if (!selection.hasCategories()) {
            clearPois();
            statusController.show(R.string.msg_map_poi_categories_empty);
        } else if (!selection.hasEnabledCategories()) {
            statusController.hide();
        } else {
            refreshNow();
        }
    }

    private void applyCategoryFailure(int discoveryId) {
        if (discoveryId == discoveryGeneration) {
            showUnavailableMessage();
        }
    }

    private void refreshNow() {
        if (!selection.hasEnabledCategories()) {
            clearPois();
            return;
        }
        statusController.show(R.string.msg_map_poi_loading);
        scriptController.requestBounds(this::handleBoundsResult);
    }

    private void handleBoundsResult(@Nullable String value) {
        try {
            MapPickerBounds bounds = MapPickerBounds.parseJavascriptResult(value);
            if (bounds == null || !bounds.isReadyForPoiFetch()) {
                showZoomInMessage();
                return;
            }
            poiLoader.load(bounds, selection.enabledCategories(), new PoiLoadListener());
        } catch (JSONException e) {
            AppLogger.w(TAG, "Failed to parse map bounds for POI fetch", e);
            showUnavailableMessage();
        }
    }

    private void applyPoiResult(@NonNull List<MapPoiMarker> markers) {
        scriptController.setPoiMarkers(toJson(markers));
        if (markers.isEmpty()) {
            statusController.show(R.string.msg_map_poi_empty);
            return;
        }
        statusController.hide();
    }

    private void applyPoiFailure() {
        statusController.showTransient(R.string.msg_map_poi_unavailable);
    }

    @NonNull
    private org.json.JSONArray toJson(@NonNull List<MapPoiMarker> markers) {
        return MapPoiMarkerJson.toJson(markers, view::labelFor);
    }

    private void showZoomInMessage() {
        scriptController.clearPoiMarkers();
        statusController.showTransient(R.string.msg_map_poi_zoom_in);
    }

    private void showUnavailableMessage() {
        scriptController.clearPoiMarkers();
        statusController.showTransient(R.string.msg_map_poi_unavailable);
    }

    private final class PoiLoadListener implements MapPoiLoader.Listener {
        @Override
        public void onCachedPois(@NonNull List<MapPoiMarker> markers) {
            scriptController.setPoiMarkers(toJson(markers));
        }

        @Override
        public void onPoiLoadComplete(@NonNull List<MapPoiMarker> markers) {
            applyPoiResult(markers);
        }

        @Override
        public void onPoiLoadFailure() {
            applyPoiFailure();
        }
    }
}
