package vibro.navigator.main;

import vibro.navigator.R;
import vibro.navigator.map.MapPickerActivity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.logging.AppLogger;

final class MainActivityMapPickerCoordinator {

    private static final int REQ_PICK_DESTINATION_ON_MAP = 2001;
    private static final int REQ_PICK_STOP_ON_MAP_BASE = 3000;
    private static final int MAX_STOP_PICKER_COUNT = 1000;
    // Lets the pressed/ripple state draw before Android starts the map picker activity.
    static final long MAP_PICKER_LAUNCH_DELAY_MS = 100L;

    private static final String TAG = "MainMapPicker";

    private final Activity activity;
    private final TaskScheduler scheduler;

    MainActivityMapPickerCoordinator(@NonNull MainActivity activity) {
        this(activity, AndroidTaskScheduler.main());
    }

    MainActivityMapPickerCoordinator(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler
    ) {
        this.activity = activity;
        this.scheduler = scheduler;
    }

    void openDestinationMapPicker(@NonNull PoiInputController destinationController) {
        AppLogger.i(TAG, "Destination map picker requested");
        scheduler.postDelayed(
                () -> startDestinationMapPicker(destinationController),
                MAP_PICKER_LAUNCH_DELAY_MS
        );
    }

    void openStopMapPicker(
            @NonNull MainActivityStopController stopController,
            @NonNull PoiInputController stopInputController
    ) {
        AppLogger.i(TAG, "Stop map picker requested");
        scheduler.postDelayed(
                () -> startStopMapPicker(stopController, stopInputController),
                MAP_PICKER_LAUNCH_DELAY_MS
        );
    }

    private void startDestinationMapPicker(@NonNull PoiInputController destinationController) {
        if (!canStartPicker()) {
            return;
        }
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

    private void startStopMapPicker(
            @NonNull MainActivityStopController stopController,
            @NonNull PoiInputController stopInputController
    ) {
        if (!canStartPicker()) {
            return;
        }
        int stopIndex = stopController.indexOf(stopInputController);
        if (stopIndex < 0) {
            AppLogger.w(TAG, "Stop map request ignored because controller is no longer attached");
            return;
        }
        if (stopIndex >= MAX_STOP_PICKER_COUNT) {
            AppLogger.w(TAG, "Stop map request ignored for unsupported index=" + stopIndex);
            return;
        }
        AppLogger.i(TAG, "Opening map picker for stop index=" + stopIndex);
        activity.startActivityForResult(
                MapPickerActivity.createIntent(
                        activity,
                        activity.getString(R.string.title_map_picker_stop, stopIndex + 1),
                        resolveInitialPoi(stopInputController)
                ),
                REQ_PICK_STOP_ON_MAP_BASE + stopIndex
        );
    }

    private boolean canStartPicker() {
        return !activity.isFinishing() && !activity.isDestroyed();
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
        if (resultCode != Activity.RESULT_OK || destinationController == null) {
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
        if (resultCode != Activity.RESULT_OK || stopController == null) {
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

