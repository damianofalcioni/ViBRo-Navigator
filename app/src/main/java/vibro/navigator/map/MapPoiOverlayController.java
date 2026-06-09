package vibro.navigator.map;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
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
    private final TaskScheduler mainThreadScheduler = AndroidTaskScheduler.main();
    @NonNull
    private final MapPoiStatusController statusController;
    @NonNull
    private final MapPoiLoader poiLoader;
    @NonNull
    private final MapPoiCategoryFilter categoryFilter;
    @NonNull
    private final MapPoiDiscoveryRunner discoveryRunner;
    @NonNull
    private final Runnable refreshRunnable = this::refreshNow;
    @NonNull
    private final Runnable discoveryRunnable = this::discoverCategoriesNow;
    private volatile boolean active = true;

    MapPoiOverlayController(
            @NonNull MapPoiOverlayView view,
            @NonNull MapPickerScriptController scriptController,
            @NonNull MapPoiCategoryFilter categoryFilter
    ) {
        this.view = view;
        this.scriptController = scriptController;
        this.categoryFilter = categoryFilter;
        this.statusController = new MapPoiStatusController(view, mainThreadScheduler);
        this.poiLoader = new MapPoiLoader(mainThreadScheduler);
        this.discoveryRunner = new MapPoiDiscoveryRunner(mainThreadScheduler, poiLoader);
        view.setPanelListener(this::scheduleCategoryDiscovery);
        if (categoryFilter.isEnabled()) {
            selection.setCategories(categoryFilter.categories());
        }
        renderCategories();
    }

    @NonNull
    static MapPoiOverlayController attach(
            @NonNull Activity activity,
            @NonNull MapPickerScriptController scriptController
    ) {
        return new MapPoiOverlayController(
                MapPoiOverlayView.bind(activity),
                scriptController,
                MapPoiCategoryFilter.fromSettings(activity)
        );
    }

    void onMapViewChanged() {
        runIfActive(() -> {
            if (selection.hasEnabledCategories()) {
                scheduleRefresh();
            }
        });
    }

    void shutdown() {
        active = false;
        mainThreadScheduler.removeCallbacks(refreshRunnable);
        mainThreadScheduler.removeCallbacks(discoveryRunnable);
        statusController.hide();
        poiLoader.shutdown();
        discoveryRunner.shutdown();
    }

    private void renderCategories() {
        view.populateCategories(selection.categories(), selection.enabledCategoryIds(), this::onCategoryChecked);
    }

    private void onCategoryChecked(@NonNull MapPoiCategory category, boolean checked) {
        runIfActive(() -> {
            selection.setChecked(category, checked);
            renderCategories();
            view.hidePanel();
            if (!selection.hasEnabledCategories()) {
                clearPois();
                return;
            }
            refreshNow();
        });
    }

    private void clearPois() {
        mainThreadScheduler.removeCallbacks(refreshRunnable);
        statusController.hide();
        poiLoader.cancel();
        scriptController.clearPoiMarkers();
    }

    private void scheduleRefresh() {
        mainThreadScheduler.removeCallbacks(refreshRunnable);
        mainThreadScheduler.postDelayed(refreshRunnable, REFRESH_DELAY_MS);
    }

    private void scheduleCategoryDiscovery() {
        runIfActive(() -> {
            mainThreadScheduler.removeCallbacks(discoveryRunnable);
            mainThreadScheduler.postDelayed(discoveryRunnable, DISCOVERY_DELAY_MS);
        });
    }

    private void discoverCategoriesNow() {
        runIfActive(() -> {
            if (categoryFilter.isEnabled()) {
                discoverFilteredCategoriesNow();
                return;
            }
            statusController.show(R.string.msg_map_poi_loading);
            scriptController.requestBounds(this::handleDiscoveryBoundsResult);
        });
    }

    private void discoverFilteredCategoriesNow() {
        selection.setCategories(categoryFilter.categories());
        renderCategories();
        if (!categoryFilter.hasCategories()) {
            clearPois();
            statusController.show(R.string.msg_map_poi_categories_empty);
            return;
        }
        statusController.show(R.string.msg_map_poi_loading);
        scriptController.requestBounds(this::handleFilteredDiscoveryBoundsResult);
    }

    private void handleFilteredDiscoveryBoundsResult(@Nullable String value) {
        runIfActive(() -> {
            try {
                MapPickerBounds bounds = MapPickerBounds.parseJavascriptResult(value);
                if (bounds == null || !bounds.isReadyForPoiFetch()) {
                    showZoomInMessage();
                    return;
                }
                discoveryRunner.discoverFiltered(bounds, categoryFilter.categories(), new CategoryDiscoveryListener());
            } catch (JSONException e) {
                AppLogger.w(TAG, "Failed to parse map bounds for filtered POI category discovery", e);
                showUnavailableMessage();
            }
        });
    }

    private void handleDiscoveryBoundsResult(@Nullable String value) {
        runIfActive(() -> {
            try {
                MapPickerBounds bounds = MapPickerBounds.parseJavascriptResult(value);
                if (bounds == null || !bounds.isReadyForPoiFetch()) {
                    selection.clearCategories();
                    renderCategories();
                    showZoomInMessage();
                    return;
                }
                discoveryRunner.discoverAll(bounds, new CategoryDiscoveryListener());
            } catch (JSONException e) {
                AppLogger.w(TAG, "Failed to parse map bounds for POI category discovery", e);
                showUnavailableMessage();
            }
        });
    }

    private void applyCategoryResult(@NonNull List<MapPoiCategory> discovered) {
        runIfActive(() -> {
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
        });
    }

    private void refreshNow() {
        runIfActive(() -> {
            if (!selection.hasEnabledCategories()) {
                clearPois();
                return;
            }
            statusController.show(R.string.msg_map_poi_loading);
            scriptController.requestBounds(this::handleBoundsResult);
        });
    }

    private void handleBoundsResult(@Nullable String value) {
        runIfActive(() -> {
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
        });
    }

    private void applyPoiResult(@NonNull List<MapPoiMarker> markers) {
        runIfActive(() -> {
            scriptController.setPoiMarkers(toJson(markers));
            if (markers.isEmpty()) {
                statusController.show(R.string.msg_map_poi_empty);
                return;
            }
            statusController.hide();
        });
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

    private void runIfActive(@NonNull Runnable action) {
        if (active) {
            action.run();
        }
    }

    private final class CategoryDiscoveryListener implements MapPoiDiscoveryRunner.Listener {
        @Override
        public void onDiscoveryComplete(@NonNull OsmMapPoiDiscoveryResult discovery) {
            applyCategoryResult(discovery.categories);
        }

        @Override
        public void onDiscoveryFailure() {
            showUnavailableMessage();
        }
    }

    private final class PoiLoadListener implements MapPoiLoader.Listener {
        @Override
        public void onCachedPois(@NonNull List<MapPoiMarker> markers) {
            runIfActive(() -> scriptController.setPoiMarkers(toJson(markers)));
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
