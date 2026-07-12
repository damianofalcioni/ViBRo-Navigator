package vibro.navigator.android.location;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;

final class GplayFusedUpdateFailureHandler {
    private static final String TAG = "FusedLocation";

    @NonNull
    private Runnable listener = () -> {
    };
    private int activeGeneration;

    void setListener(@NonNull Runnable listener) {
        this.listener = listener;
    }

    int beginRequest() {
        return ++activeGeneration;
    }

    void invalidate() {
        activeGeneration++;
    }

    void handle(int requestGeneration, @NonNull Exception error) {
        if (requestGeneration != activeGeneration) {
            AppLogger.d(TAG, "Ignoring stale fused update failure");
            return;
        }
        AppLogger.w(TAG, "Fused location updates failed", error);
        listener.run();
    }
}
