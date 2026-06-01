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
    private final RectF bounds = new RectF();

    ViBRoAutoCompassPainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        compassModeController = new NavigationCompassModeController(elapsedRealtimeClock);
        compassView = new NavigationCompassView(carContext);
        overlayPainter = new ViBRoAutoCompassOverlayPainter(carContext, controls);
    }

    void draw(@NonNull Canvas canvas, @NonNull NavState state, float left, float top, float size) {
        bounds.set(left, top, left + size, top + size);
        NavCompassState compassState = compassModeController.resolve(state.routeStatus.compassState);
        compassView.setNavigationPaused(state.pauseStatus.paused);
        compassView.setCompassState(compassState);
        int resolvedSize = Math.max(1, Math.round(size));
        int measureSpec = View.MeasureSpec.makeMeasureSpec(resolvedSize, View.MeasureSpec.EXACTLY);
        compassView.measure(measureSpec, measureSpec);
        compassView.layout(0, 0, resolvedSize, resolvedSize);
        int saveCount = canvas.save();
        canvas.translate(left, top);
        compassView.draw(canvas);
        canvas.restoreToCount(saveCount);
        overlayPainter.draw(canvas, state, bounds);
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
