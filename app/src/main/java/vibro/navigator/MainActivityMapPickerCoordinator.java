package vibro.navigator;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.poi.Poi;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.util.AppLogger;

final class MainActivityMapPickerCoordinator {

    private static final int REQ_PICK_DESTINATION_ON_MAP = 2001;
    private static final int REQ_PICK_STOP_ON_MAP_BASE = 3000;
    private static final int MAX_STOP_PICKER_COUNT = 1000;

    private static final String TAG = "MainMapPicker";

    private final MainActivity activity;

    MainActivityMapPickerCoordinator(@NonNull MainActivity activity) {
        this.activity = activity;
    }

    @SuppressWarnings("deprecation")
    void openDestinationMapPicker(@NonNull PoiInputController destinationController) {
        AppLogger.i(TAG, "Opening map picker for destination");
        activity.startActivityForResult(
                MapPickerActivity.createIntent(
                        activity,
                        activity.getString(R.string.title_map_picker_destination),
                        resolveInitialPoi(destinationController)
                ),
                REQ_PICK_DESTINATION_ON_MAP
        );
    }

    @SuppressWarnings("deprecation")
    void openStopMapPicker(int stopIndex, @Nullable Poi initialPoi) {
        AppLogger.i(TAG, "Opening map picker for stop index=" + stopIndex);
        activity.startActivityForResult(
                MapPickerActivity.createIntent(
                        activity,
                        activity.getString(R.string.title_map_picker_stop, stopIndex + 1),
                        initialPoi
                ),
                REQ_PICK_STOP_ON_MAP_BASE + stopIndex
        );
    }

    boolean handleActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data,
            @Nullable PoiInputController destinationController,
            @Nullable MainActivityStopController stopController
    ) {
        if (requestCode == REQ_PICK_DESTINATION_ON_MAP) {
            handleDestinationMapResult(resultCode, data, destinationController);
            return true;
        }
        if (isStopPickerRequest(requestCode)) {
            handleStopMapResult(
                    requestCode - REQ_PICK_STOP_ON_MAP_BASE,
                    resultCode,
                    data,
                    stopController
            );
            return true;
        }
        return false;
    }

    private boolean isStopPickerRequest(int requestCode) {
        return requestCode >= REQ_PICK_STOP_ON_MAP_BASE
                && requestCode < REQ_PICK_STOP_ON_MAP_BASE + MAX_STOP_PICKER_COUNT;
    }

    private void handleDestinationMapResult(
            int resultCode,
            @Nullable Intent data,
            @Nullable PoiInputController destinationController
    ) {
        if (resultCode != MainActivity.RESULT_OK || destinationController == null) {
            return;
        }
        Poi poi = MapPickerActivity.parseResult(activity, data);
        if (poi == null) {
            AppLogger.w(TAG, "Destination map picker returned without POI");
            return;
        }
        destinationController.setPoi(poi);
        AppLogger.i(TAG, "Destination selected from map=" + poi.displayLabel());
    }

    private void handleStopMapResult(
            int stopIndex,
            int resultCode,
            @Nullable Intent data,
            @Nullable MainActivityStopController stopController
    ) {
        if (resultCode != MainActivity.RESULT_OK || stopController == null) {
            return;
        }
        Poi poi = MapPickerActivity.parseResult(activity, data);
        if (poi == null) {
            AppLogger.w(TAG, "Stop map picker returned without POI index=" + stopIndex);
            return;
        }
        stopController.setStopPoi(stopIndex, poi);
        AppLogger.i(TAG, "Stop selected from map index=" + stopIndex + " poi=" + poi.displayLabel());
    }

    @Nullable
    private static Poi resolveInitialPoi(@NonNull PoiInputController controller) {
        Poi selectedPoi = controller.getSelectedPoi();
        if (selectedPoi != null) {
            return selectedPoi;
        }
        return controller.parseCurrentPoi();
    }
}
