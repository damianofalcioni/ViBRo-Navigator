package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;

import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavState;

final class ViBRoAutoSurfaceRenderer implements SurfaceCallback {

    private static final String TAG = "ViBRoAutoSurface";
    private final TaskScheduler uiScheduler;
    private final ViBRoAutoCompassStreetViewportSink compassStreetViewportSink;
    private final ViBRoAutoSurfacePainter painter;
    private final Rect stableArea = new Rect();
    private final Runnable surfaceAnimationTicker = this::renderOnMainThread;

    @Nullable
    private SurfaceContainer surfaceContainer;
    @Nullable
    private NavState currentState;
    private boolean renderPosted;

    ViBRoAutoSurfaceRenderer(
            @NonNull CarContext carContext,
            @NonNull TaskScheduler uiScheduler,
            @NonNull ViBRoAutoCompassStreetViewportSink compassStreetViewportSink
    ) {
        this.compassStreetViewportSink = compassStreetViewportSink;
        this.uiScheduler = uiScheduler;
        painter = new ViBRoAutoSurfacePainter(
                carContext,
                compassStreetViewportSink,
                AndroidElapsedRealtimeClock.INSTANCE
        );
    }

    void setState(@Nullable NavState state) {
        currentState = state;
        if (state == null) {
            painter.resetCompass();
            compassStreetViewportSink.clearCompassStreetViewport();
        }
        render();
    }

    void render() {
        if (surfaceContainer == null || renderPosted) {
            return;
        }
        renderPosted = true;
        uiScheduler.post(this::renderOnMainThread);
    }

    void clearSurface() {
        surfaceContainer = null;
        painter.resetCompass();
        compassStreetViewportSink.clearCompassStreetViewport();
        clearAnimationCallbacks();
    }

    void dispose() {
        currentState = null;
        clearSurface();
        painter.dispose();
    }

    void refreshTheme() {
        painter.refreshTheme();
        render();
    }

    @Override
    public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
        this.surfaceContainer = surfaceContainer;
        render();
    }

    @Override
    public void onStableAreaChanged(@NonNull Rect stableArea) {
        this.stableArea.set(stableArea);
        render();
    }

    @Override
    public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
        clearSurface();
    }

    @Override
    public void onClick(float x, float y) {
        NavState state = currentState;
        boolean handled = state != null && painter.handleClick(x, y, state);
        AppLogger.d(TAG, "Surface click x=" + x + " y=" + y
                + " hasState=" + (state != null)
                + " handled=" + handled);
        if (handled) {
            renderOnMainThread();
        }
    }

    @Override
    public void onScroll(float distanceX, float distanceY) {
        NavState state = currentState;
        if (surfaceContainer != null && state != null && painter.handleScroll(distanceX, distanceY, state)) {
            render();
        }
    }

    @Override
    public void onScale(float focusX, float focusY, float scaleFactor) {
        NavState state = currentState;
        if (surfaceContainer != null && state != null && painter.handleScale(focusX, focusY, scaleFactor, state)) {
            render();
        }
    }

    private void renderOnMainThread() {
        renderPosted = false;
        SurfaceContainer container = surfaceContainer;
        if (container == null) {
            return;
        }
        Surface surface = container.getSurface();
        if (surface == null || !surface.isValid()) {
            return;
        }
        Canvas canvas = null;
        try {
            canvas = surface.lockCanvas(null);
            painter.draw(canvas, container, stableArea, currentState);
        } catch (IllegalArgumentException | IllegalStateException e) {
            AppLogger.w(TAG, "Could not render Android Auto surface", e);
        } finally {
            if (canvas != null) {
                surface.unlockCanvasAndPost(canvas);
            }
        }
        scheduleAnimationIfNeeded();
    }

    private void scheduleAnimationIfNeeded() {
        clearAnimationCallbacks();
        if (painter.isCompassTransitionInProgress()) {
            uiScheduler.postAnimationFrame(surfaceAnimationTicker);
        }
    }

    private void clearAnimationCallbacks() {
        uiScheduler.cancelAnimationFrame(surfaceAnimationTicker);
        uiScheduler.removeCallbacks(surfaceAnimationTicker);
    }
}
