package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavState;

final class ViBRoAutoSurfaceRenderer implements SurfaceCallback {

    interface Controls {
        void onBlockedRoad();

        void onStopNavigation();

        void onTogglePaused();
    }

    private static final String TAG = "ViBRoAutoSurface";
    private static final long COMPASS_TRANSITION_FRAME_DELAY_MS = 16L;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ViBRoAutoSurfacePainter painter;
    private final Rect stableArea = new Rect();
    private final Runnable compassTransitionTicker = this::renderOnMainThread;

    @Nullable
    private SurfaceContainer surfaceContainer;
    @Nullable
    private NavState currentState;
    private boolean renderPosted;

    ViBRoAutoSurfaceRenderer(@NonNull CarContext carContext, @NonNull Controls controls) {
        painter = new ViBRoAutoSurfacePainter(carContext, controls);
    }

    void setState(@Nullable NavState state) {
        currentState = state;
        render();
    }

    void render() {
        if (renderPosted) {
            return;
        }
        renderPosted = true;
        uiHandler.post(this::renderOnMainThread);
    }

    void clearSurface() {
        surfaceContainer = null;
        clearCompassCallbacks();
    }

    void dispose() {
        currentState = null;
        clearSurface();
        painter.dispose();
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
        if (state != null && painter.handleClick(x, y, state)) {
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
            uiHandler.postDelayed(compassTransitionTicker, COMPASS_TRANSITION_FRAME_DELAY_MS);
        }
    }

    private void clearCompassCallbacks() {
        uiHandler.removeCallbacks(compassTransitionTicker);
    }
}
