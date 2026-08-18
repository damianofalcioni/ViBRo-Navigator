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

    interface Controls {
        void onBlockedRoad();

        void onStopNavigation();

        void onTogglePaused();

        void onToggleCustomButton();

        @NonNull
        String buildCurrentDirectionDetailsText();
    }

    private static final String TAG = "ViBRoAutoSurface";
    private static final long COMPASS_TRANSITION_FRAME_DELAY_MS = 16L;

    private final TaskScheduler uiScheduler;
    private final CarContext carContext;
    private final Controls controls;
    private final ViBRoAutoCompassStreetViewportSink compassStreetViewportSink;
    private ViBRoAutoSurfacePainter painter;
    private final Rect stableArea = new Rect();
    private final Runnable compassTransitionTicker = this::renderOnMainThread;

    @Nullable
    private SurfaceContainer surfaceContainer;
    @Nullable
    private NavState currentState;
    private boolean renderPosted;

    ViBRoAutoSurfaceRenderer(
            @NonNull CarContext carContext,
            @NonNull Controls controls,
            @NonNull TaskScheduler uiScheduler,
            @NonNull ViBRoAutoCompassStreetViewportSink compassStreetViewportSink
    ) {
        this.carContext = carContext;
        this.controls = controls;
        this.compassStreetViewportSink = compassStreetViewportSink;
        this.uiScheduler = uiScheduler;
        painter = new ViBRoAutoSurfacePainter(
                carContext,
                controls,
                compassStreetViewportSink,
                AndroidElapsedRealtimeClock.INSTANCE
        );
    }

    void setState(@Nullable NavState state) {
        currentState = state;
        if (state == null) {
            compassStreetViewportSink.clearCompassStreetViewport();
        }
        render();
    }

    void render() {
        if (renderPosted) {
            return;
        }
        renderPosted = true;
        uiScheduler.post(this::renderOnMainThread);
    }

    void clearSurface() {
        surfaceContainer = null;
        compassStreetViewportSink.clearCompassStreetViewport();
        clearCompassCallbacks();
    }

    void dispose() {
        currentState = null;
        clearSurface();
        painter.dispose();
    }

    void refreshTheme() {
        painter.dispose();
        painter = new ViBRoAutoSurfacePainter(
                carContext,
                controls,
                compassStreetViewportSink,
                AndroidElapsedRealtimeClock.INSTANCE
        );
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
        scheduleCompassTransitionIfNeeded();
    }

    private void scheduleCompassTransitionIfNeeded() {
        clearCompassCallbacks();
        if (painter.isCompassTransitionInProgress()) {
            uiScheduler.postDelayed(compassTransitionTicker, COMPASS_TRANSITION_FRAME_DELAY_MS);
        }
    }

    private void clearCompassCallbacks() {
        uiScheduler.removeCallbacks(compassTransitionTicker);
    }
}
