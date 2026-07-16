package vibro.navigator.main;

import androidx.annotation.Nullable;

final class MainActivitySpeechInputVisibility {
    private MainActivitySpeechInputVisibility() {
    }

    static void render(
            @Nullable MainActivityPoiInputActionCoordinator coordinator,
            @Nullable MainActivityControls controls
    ) {
        if (coordinator == null || controls == null) {
            return;
        }
        coordinator.renderSpeechInputButtons(controls.destinationVoiceButton, controls.stopsContainer);
    }
}
