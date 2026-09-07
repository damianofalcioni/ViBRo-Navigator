package vibro.navigator.main;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.intent.IncomingLocationPlacement;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.ui.PoiInputController;

final class MainActivityStopRowOperations {
    private MainActivityStopRowOperations() {
    }

    @NonNull
    static PoiInputController incomingLocationTarget(
            @NonNull PoiInputController destinationController,
            @NonNull MainActivityStopController stopController
    ) {
        List<String> stopTexts = new ArrayList<>();
        for (PoiInputController controller : stopController.getStopControllers()) {
            stopTexts.add(controller.getRawText());
        }
        int index = IncomingLocationPlacement.targetStopIndex(destinationController.getRawText(), stopTexts);
        if (index < 0) {
            return destinationController;
        }
        if (index == stopController.size()) {
            stopController.addStopRow(null);
        }
        return stopController.getStopControllers().get(index);
    }

    static void restoreStops(
            @NonNull MainActivityStopController stopController,
            @NonNull List<Poi> stops
    ) {
        for (Poi stop : stops) {
            stopController.addRestoredStop(stop);
        }
    }

    static void disposeAll(@NonNull List<PoiInputController> controllers) {
        for (PoiInputController controller : controllers) {
            controller.dispose();
        }
    }
}
