package vibro.navigator.map;

import androidx.annotation.NonNull;

import vibro.navigator.dispatch.TaskScheduler;

final class MapPoiStatusController {
    private static final long TRANSIENT_STATUS_MS = 3500L;

    @NonNull
    private final MapPoiOverlayView view;
    @NonNull
    private final TaskScheduler scheduler;
    @NonNull
    private final Runnable hideRunnable = this::hide;

    MapPoiStatusController(@NonNull MapPoiOverlayView view, @NonNull TaskScheduler scheduler) {
        this.view = view;
        this.scheduler = scheduler;
    }

    void show(int messageResId) {
        scheduler.removeCallbacks(hideRunnable);
        view.showStatus(messageResId);
    }

    void showTransient(int messageResId) {
        show(messageResId);
        scheduler.postDelayed(hideRunnable, TRANSIENT_STATUS_MS);
    }

    void hide() {
        scheduler.removeCallbacks(hideRunnable);
        view.hideStatus();
    }
}
