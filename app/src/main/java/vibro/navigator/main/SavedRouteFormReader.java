package vibro.navigator.main;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.ui.PoiInputController;

final class SavedRouteFormReader {
    private SavedRouteFormReader() {
    }

    @Nullable
    static SavedRoutePoints capture(
            @NonNull Context context,
            @NonNull PoiInputController destinationController,
            @NonNull List<PoiInputController> stopControllers
    ) {
        Poi destination = resolveDestination(context, destinationController);
        if (destination == null) {
            return null;
        }
        List<Poi> stops = resolveStops(context, stopControllers);
        if (stops == null) {
            return null;
        }
        return new SavedRoutePoints(destination, stops);
    }

    @Nullable
    private static Poi resolveDestination(
            @NonNull Context context,
            @NonNull PoiInputController destinationController
    ) {
        Poi destination = resolvePoi(destinationController);
        if (destination == null) {
            Toast.makeText(context, R.string.msg_missing_destination, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (!destination.hasValidCoordinates()) {
            Toast.makeText(context, R.string.msg_invalid_coordinates, Toast.LENGTH_SHORT).show();
            return null;
        }
        return destination;
    }

    @Nullable
    private static List<Poi> resolveStops(
            @NonNull Context context,
            @NonNull List<PoiInputController> stopControllers
    ) {
        List<Poi> stops = new ArrayList<>();
        for (PoiInputController controller : stopControllers) {
            if (controller.getRawText().trim().isEmpty()) {
                continue;
            }
            Poi stop = resolvePoi(controller);
            if (stop == null || !stop.hasValidCoordinates()) {
                Toast.makeText(context, R.string.msg_invalid_stop, Toast.LENGTH_SHORT).show();
                return null;
            }
            stops.add(stop);
        }
        return stops;
    }

    @Nullable
    private static Poi resolvePoi(@NonNull PoiInputController controller) {
        Poi poi = controller.getSelectedPoi();
        return poi != null ? poi : controller.parseCurrentPoi();
    }
}
