package vibro.navigator.map;

import android.os.Handler;

import androidx.annotation.NonNull;

final class MapPoiStatusController {
    private static final long TRANSIENT_STATUS_MS = 3500L;

    @NonNull
    private final MapPoiOverlayView view;
    @NonNull
    private final Handler handler;
    @NonNull
    private final Runnable hideRunnable = this::hide;

    MapPoiStatusController(@NonNull MapPoiOverlayView view, @NonNull Handler handler) {
        this.view = view;
        this.handler = handler;
    }

    void show(int messageResId) {
        handler.removeCallbacks(hideRunnable);
        view.showStatus(messageResId);
    }

    void showTransient(int messageResId) {
        show(messageResId);
        handler.postDelayed(hideRunnable, TRANSIENT_STATUS_MS);
    }

    void hide() {
        handler.removeCallbacks(hideRunnable);
        view.hideStatus();
    }
}
