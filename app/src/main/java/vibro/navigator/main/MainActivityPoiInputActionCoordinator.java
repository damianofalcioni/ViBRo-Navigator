package vibro.navigator.main;

import vibro.navigator.R;

import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.poi.ui.PoiReverseGeocodeController;

final class MainActivityPoiInputActionCoordinator {

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

    void openDestinationMapPicker(@NonNull PoiInputController destinationController) {
        mapPickerCoordinator.openDestinationMapPicker(destinationController);
    }

    void openDestinationSpeechInput(@NonNull PoiInputController destinationController) {
        speechInputController.openDestinationSpeechInput(destinationController);
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
            row.findViewById(R.id.stopVoiceButton).setOnClickListener(
                    v -> speechInputController.openStopSpeechInput(stopInputController)
            );
        }
    }
}
