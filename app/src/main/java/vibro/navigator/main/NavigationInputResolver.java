package vibro.navigator.main;

import vibro.navigator.R;


import vibro.navigator.nav.model.NavigationRequest;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.logging.AppLogger;

import java.util.ArrayList;
import java.util.List;

final class NavigationInputResolver {

    private static final String TAG = "NavigationInputResolver";

    private NavigationInputResolver() {
    }

    @Nullable
    static Result resolve(
            @NonNull Context context,
            @NonNull PoiInputController destinationController,
            @NonNull List<PoiInputController> stopControllers,
            @NonNull String profile
    ) {
        Poi destination = resolveDestination(context, destinationController);
        if (destination == null) {
            return null;
        }

        List<Poi> stops = resolveStops(context, stopControllers);
        if (stops == null) {
            return null;
        }

        List<LatLon> stopPoints = new ArrayList<>(stops.size());
        for (Poi stop : stops) {
            stopPoints.add(new LatLon(stop.lat, stop.lon));
        }

        return new Result(
                new NavigationRequest(
                        profile,
                        destination.name,
                        new LatLon(destination.lat, destination.lon),
                        stopPoints
                ),
                destination,
                stops
        );
    }

    static void rememberHistory(@NonNull PoiHistoryStore historyStore, @NonNull Result input) {
        historyStore.addOrPromote(input.destination);
        for (Poi stop : input.stops) {
            historyStore.addOrPromote(stop);
        }
    }

    @Nullable
    private static Poi resolveDestination(@NonNull Context context, @NonNull PoiInputController destinationController) {
        Poi destination = destinationController.getSelectedPoi();
        if (destination == null) {
            destination = destinationController.parseCurrentPoi();
        }
        if (destination == null) {
            AppLogger.w(TAG, "Navigation blocked because destination is missing or unparsable");
            Toast.makeText(context, R.string.msg_missing_destination, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (Double.isNaN(destination.lat) || Double.isNaN(destination.lon)) {
            AppLogger.w(TAG, "Navigation blocked because destination coordinates are invalid destination="
                    + formatPoi(destination));
            Toast.makeText(context, R.string.msg_invalid_coordinates, Toast.LENGTH_SHORT).show();
            return null;
        }
        return destination;
    }

    @Nullable
    private static List<Poi> resolveStops(@NonNull Context context, @NonNull List<PoiInputController> stopControllers) {
        List<Poi> resolvedStops = new ArrayList<>();
        for (PoiInputController controller : stopControllers) {
            String raw = controller.getRawText().trim();
            if (raw.isEmpty()) {
                continue;
            }
            Poi stop = controller.getSelectedPoi();
            if (stop == null) {
                stop = controller.parseCurrentPoi();
            }
            if (stop == null || Double.isNaN(stop.lat) || Double.isNaN(stop.lon)) {
                AppLogger.w(TAG, "Navigation blocked because a stop is invalid raw=" + raw);
                Toast.makeText(context, R.string.msg_invalid_stop, Toast.LENGTH_SHORT).show();
                return null;
            }
            resolvedStops.add(stop);
        }
        return resolvedStops;
    }

    @NonNull
    private static String formatPoi(@NonNull Poi poi) {
        return poi.displayLabel() + " (" + poi.lat + "," + poi.lon + ")";
    }

    static final class Result {
        @NonNull
        final NavigationRequest request;
        @NonNull
        final Poi destination;
        @NonNull
        final List<Poi> stops;

        Result(
                @NonNull NavigationRequest request,
                @NonNull Poi destination,
                @NonNull List<Poi> stops
        ) {
            this.request = request;
            this.destination = destination;
            this.stops = stops;
        }
    }
}


