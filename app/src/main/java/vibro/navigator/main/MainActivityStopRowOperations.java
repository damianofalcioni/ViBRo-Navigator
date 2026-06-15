package vibro.navigator.main;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.poi.Poi;
import vibro.navigator.poi.ui.PoiInputController;

final class MainActivityStopRowOperations {
    private MainActivityStopRowOperations() {
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
