package vibro.navigator.main;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.intent.GpxWaypointRoute;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.ui.PoiInputController;

final class MainActivityGpxRouteApplier {
    private static final String TAG = "MainGpxRouteApplier";

    interface RouteModeSelector {
        void showRouteMode();
    }

    interface PoiSelectionApplier {
        void apply(@NonNull PoiInputController inputController, @NonNull Poi poi);
    }

    private MainActivityGpxRouteApplier() {
    }

    static void apply(
            @NonNull GpxWaypointRoute route,
            @NonNull PoiInputController destinationController,
            @NonNull MainActivityStopController stopController,
            @NonNull RouteModeSelector routeModeSelector,
            @NonNull PoiSelectionApplier poiSelectionApplier
    ) {
        routeModeSelector.showRouteMode();
        poiSelectionApplier.apply(destinationController, route.destination);
        stopController.replaceStops(route.stops);
        applyStops(route.stops, stopController.getStopControllers(), poiSelectionApplier);
        AppLogger.i(TAG, "Applied incoming GPX destination=" + route.destination.displayLabel()
                + " stopCount=" + route.stops.size());
    }

    private static void applyStops(
            @NonNull List<Poi> stops,
            @NonNull List<PoiInputController> stopControllers,
            @NonNull PoiSelectionApplier poiSelectionApplier
    ) {
        int count = Math.min(stops.size(), stopControllers.size());
        for (int index = 0; index < count; index++) {
            poiSelectionApplier.apply(stopControllers.get(index), stops.get(index));
        }
    }
}
