package vibro.navigator.main;

import vibro.navigator.R;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.poi.ui.PoiReverseGeocodeController;

final class MainActivityPoiInputActionCoordinator {

    private static final String TAG = "MainPoiActions";

    @NonNull
    private final MainActivityMapPickerCoordinator mapPickerCoordinator;
    @NonNull
    private final MainActivitySpeechInputController speechInputController;

    MainActivityPoiInputActionCoordinator(
            @NonNull MainActivity activity,
            @NonNull PoiReverseGeocodeController reverseGeocodeController
    ) {
        mapPickerCoordinator = new MainActivityMapPickerCoordinator(activity, reverseGeocodeController);
        speechInputController = new MainActivitySpeechInputController(activity);
    }

    static boolean handleRequestPermissionsResult(
            @Nullable MainActivityPoiInputActionCoordinator coordinator,
            int requestCode,
            @NonNull int[] grantResults
    ) {
        return coordinator != null
                && coordinator.handleRequestPermissionsResult(requestCode, grantResults);
    }

    static void dispose(@Nullable MainActivityPoiInputActionCoordinator coordinator) {
        if (coordinator != null) {
            coordinator.dispose();
        }
    }

    static void openStopMapPicker(
            @Nullable MainActivityPoiInputActionCoordinator coordinator,
            @Nullable MainActivityStopController stopController,
            @NonNull PoiInputController stopInputController
    ) {
        if (coordinator == null || stopController == null) {
            AppLogger.w(TAG, "Stop map picker requested before stop controller was ready");
            return;
        }
        coordinator.openStopMapPicker(stopController, stopInputController);
    }

    void openDestinationMapPicker(@NonNull PoiInputController destinationController) {
        mapPickerCoordinator.openDestinationMapPicker(destinationController);
    }

    void openDestinationSpeechInput(@NonNull PoiInputController destinationController) {
        speechInputController.openDestinationSpeechInput(destinationController);
    }

    void renderSpeechInputButtons(
            @NonNull View destinationVoiceButton,
            @NonNull LinearLayout stopsContainer
    ) {
        boolean visible = speechInputController.isSpeechInputVisible();
        destinationVoiceButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        MainActivityStopVoiceButtons.setVisible(stopsContainer, visible);
    }

    void openStopMapPicker(
            @NonNull MainActivityStopController stopController,
            @NonNull PoiInputController stopInputController
    ) {
        mapPickerCoordinator.openStopMapPicker(stopController, stopInputController);
    }

    @NonNull
    MainActivityStopController.MapPickListener stopRowActions(
            @NonNull MainActivityStopController.MapPickListener mapPickListener
    ) {
        return new StopRowActions(mapPickListener, speechInputController);
    }

    boolean handleActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data,
            @Nullable PoiInputController destinationController,
            @Nullable MainActivityStopController stopController
    ) {
        if (speechInputController.handleActivityResult(requestCode, resultCode, data)) {
            return true;
        }
        return mapPickerCoordinator.handleActivityResult(
                requestCode,
                resultCode,
                data,
                destinationController,
                stopController
        );
    }

    boolean handleRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        return speechInputController.handleRequestPermissionsResult(requestCode, grantResults);
    }

    void dispose() {
        speechInputController.dispose();
    }

    private static final class StopRowActions implements MainActivityStopController.MapPickListener {

        @NonNull
        private final MainActivityStopController.MapPickListener mapPickListener;
        @NonNull
        private final MainActivitySpeechInputController speechInputController;

        private StopRowActions(
                @NonNull MainActivityStopController.MapPickListener mapPickListener,
                @NonNull MainActivitySpeechInputController speechInputController
        ) {
            this.mapPickListener = mapPickListener;
            this.speechInputController = speechInputController;
        }

        @Override
        public void onPickStopFromMap(@NonNull PoiInputController stopInputController) {
            mapPickListener.onPickStopFromMap(stopInputController);
        }

        @Override
        public void onStopRowCreated(
                @NonNull View row,
                @NonNull PoiInputController stopInputController
        ) {
            View stopVoiceButton = row.findViewById(R.id.stopVoiceButton);
            stopVoiceButton.setVisibility(speechInputController.isSpeechInputVisible() ? View.VISIBLE : View.GONE);
            stopVoiceButton.setOnClickListener(
                    v -> speechInputController.openStopSpeechInput(stopInputController)
            );
        }
    }
}
