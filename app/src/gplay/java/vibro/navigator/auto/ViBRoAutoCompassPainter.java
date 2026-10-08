package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;

import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.ui.NavigationCompassView;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.orientation.NavigationCompassModeController;
import vibro.navigator.nav.orientation.NavigationCompassGestureState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.settings.AppCompassSettings;

final class ViBRoAutoCompassPainter {

    private final NavigationCompassModeController compassModeController;
    private final NavigationCompassGestureState gestureState = new NavigationCompassGestureState();
    private final ViBRoAutoGestureSequence gestures;
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    private final CarContext carContext;
    private final NavigationCompassView compassView;
    private final ViBRoAutoCompassOverlayPainter overlayPainter;
    private final ViBRoAutoCompassStreetViewportSink compassStreetViewportSink;
    private final RectF bounds = new RectF();
    private Boolean lastFullscreenRouteMode;

    ViBRoAutoCompassPainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls,
            @NonNull ViBRoAutoCompassStreetViewportSink compassStreetViewportSink,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        compassModeController = new NavigationCompassModeController(elapsedRealtimeClock);
        this.carContext = carContext;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        float touchSlop = ViewConfiguration.get(carContext).getScaledTouchSlop();
        float density = carContext.getResources().getDisplayMetrics().density;
        gestures = new ViBRoAutoGestureSequence(Math.max(48f * density, touchSlop * 2f),
                touchSlop, ViewConfiguration.getDoubleTapTimeout());
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
        if (lastFullscreenRouteMode == null || lastFullscreenRouteMode != fullscreenRouteMode) {
            compassView.setFullscreenRouteModeEnabled(fullscreenRouteMode);
            lastFullscreenRouteMode = fullscreenRouteMode;
        }
        NavCompassState compassState = resolveCompassState(state);
        compassView.setPerspectiveProgress(gestureState.perspectiveProgress(compassModeController.perspectiveProgress()));
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
        if (!gestures.isRecent(elapsedRealtimeClock.elapsedRealtimeMs())) {
            compassModeController.onCompassTapped(state.routeStatus.compassState, animateRadiusTransition());
        }
        return true;
    }

    boolean handleScroll(float distanceX, float distanceY, @NonNull NavState state) {
        if (bounds.isEmpty() || resolveCompassState(state) == null) {
            return false;
        }
        int step = gestures.scroll(distanceX, distanceY, elapsedRealtimeClock.elapsedRealtimeMs());
        if (step != 0) {
            compassModeController.onCompassSwiped(state.routeStatus.compassState, step > 0, animateRadiusTransition());
            return true;
        }
        return gestureState.tiltBy(gestures.tiltDistance() * 1.5f / Math.max(1f, bounds.height()));
    }

    boolean handleScale(float focusX, float focusY, float factor, @NonNull NavState state) {
        if (!acceptsScaleFocus(focusX, focusY) || resolveCompassState(state) == null) {
            return false;
        }
        return gestureState.zoomBy(gestures.scale(factor, elapsedRealtimeClock.elapsedRealtimeMs()));
    }

    private boolean acceptsScaleFocus(float x, float y) {
        if (bounds.isEmpty() || !Float.isFinite(x) || !Float.isFinite(y)) {
            return false;
        }
        // Rotary hosts can omit the focal point by supplying negative coordinates.
        return (x < 0f || y < 0f || bounds.contains(x, y)) && !overlayPainter.containsControl(x, y);
    }

    @Nullable
    private NavCompassState resolveCompassState(@NonNull NavState state) {
        return gestureState.apply(compassModeController.resolve(state.routeStatus.compassState,
                animateRadiusTransition()), compassModeController.isPerspectiveViewEnabled());
    }

    private boolean animateRadiusTransition() {
        return !AppCompassSettings.isInstantZoomEnabled(carContext);
    }

    void reset() {
        gestures.clear();
        compassModeController.reset();
        gestureState.reset();
        bounds.setEmpty();
        compassView.setCompassState(null);
    }

    boolean isTransitionInProgress() {
        return compassModeController.isTransitionInProgress();
    }

    void dispose() {
        reset();
    }
}
