package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;

import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.ui.NavigationCompassView;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.orientation.NavigationCompassModeController;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class ViBRoAutoCompassPainter {

    private final NavigationCompassModeController compassModeController;
    private final NavigationCompassView compassView;
    private final ViBRoAutoCompassOverlayPainter overlayPainter;
    private final ViBRoAutoCompassStreetViewportSink compassStreetViewportSink;
    private final RectF bounds = new RectF();

    ViBRoAutoCompassPainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls,
            @NonNull ViBRoAutoCompassStreetViewportSink compassStreetViewportSink,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        compassModeController = new NavigationCompassModeController(elapsedRealtimeClock);
        compassView = new NavigationCompassView(carContext);
        this.compassStreetViewportSink = compassStreetViewportSink;
        overlayPainter = new ViBRoAutoCompassOverlayPainter(carContext, controls);
    }

    void draw(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float height,
            boolean fullscreenRouteMode,
            @NonNull RectF overlayBounds,
            float scale
    ) {
        compassView.setFullscreenRouteModeEnabled(fullscreenRouteMode);
        NavCompassState compassState = compassModeController.resolve(state.routeStatus.compassState);
        compassStreetViewportSink.onCompassStreetViewport(compassState);
        compassView.setNavigationPaused(state.pauseStatus.paused);
        compassView.setCompassState(compassState);
        int resolvedWidth = Math.max(1, Math.round(width));
        int resolvedHeight = Math.max(1, Math.round(height));
        int widthSpec = View.MeasureSpec.makeMeasureSpec(resolvedWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(resolvedHeight, View.MeasureSpec.EXACTLY);
        compassView.measure(widthSpec, heightSpec);
        int measuredWidth = Math.max(1, compassView.getMeasuredWidth());
        int measuredHeight = Math.max(1, compassView.getMeasuredHeight());
        compassView.layout(0, 0, measuredWidth, measuredHeight);
        bounds.set(left, top, left + measuredWidth, top + measuredHeight);
        int saveCount = canvas.save();
        canvas.clipRect(left, top, left + measuredWidth, top + measuredHeight);
        canvas.translate(left, top);
        compassView.draw(canvas);
        canvas.restoreToCount(saveCount);
        overlayPainter.draw(canvas, state, overlayBounds, scale);
    }

    boolean handleClick(float x, float y, @NonNull NavState state) {
        if (overlayPainter.handleClick(x, y)) {
            return true;
        }
        if (!bounds.contains(x, y)) {
            return false;
        }
        compassModeController.onCompassTapped(state.routeStatus.compassState);
        return true;
    }

    boolean isTransitionInProgress() {
        return compassModeController.isTransitionInProgress();
    }

    void dispose() {
        compassView.setCompassState(null);
    }
}
