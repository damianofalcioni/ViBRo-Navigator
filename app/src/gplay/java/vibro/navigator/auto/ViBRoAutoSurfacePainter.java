package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceContainer;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.settings.AppCompassSettings;

final class ViBRoAutoSurfacePainter {

    private final CarContext carContext;
    private final Rect fullSurfaceArea = new Rect();
    private final RectF compassOverlayBounds = new RectF();
    private final ViBRoAutoTextColumnPainter textColumnPainter;
    private final ViBRoAutoCompassPainter compassPainter;

    ViBRoAutoSurfacePainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls,
            @NonNull ViBRoAutoCompassStreetViewportSink compassStreetViewportSink,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.carContext = carContext;
        textColumnPainter = new ViBRoAutoTextColumnPainter(carContext, controls, elapsedRealtimeClock);
        compassPainter = new ViBRoAutoCompassPainter(
                carContext,
                controls,
                compassStreetViewportSink,
                elapsedRealtimeClock
        );
    }

    void draw(
            @NonNull Canvas canvas,
            @NonNull SurfaceContainer container,
            @NonNull Rect stableArea,
            @Nullable NavState state
    ) {
        canvas.drawColor(AndroidAppTheme.color(carContext, R.attr.vibroBackgroundColor));
        fullSurfaceArea.set(0, 0, container.getWidth(), container.getHeight());
        Rect contentArea = stableArea.isEmpty() ? fullSurfaceArea : stableArea;
        float layoutScale = ViBRoAutoRenderScale.fromContentHeight(this.carContext, contentArea.height());
        if (state == null) {
            textColumnPainter.drawMessage(
                    canvas,
                    contentArea,
                    carContext.getString(R.string.auto_no_active_navigation_title),
                    layoutScale
            );
            return;
        }
        drawActiveNavigation(
                canvas,
                contentArea,
                state,
                AppCompassSettings.isFullscreenRouteEnabled(carContext),
                layoutScale
        );
    }

    boolean handleClick(float x, float y, @NonNull NavState state) {
        if (textColumnPainter.handleClick(x, y, state)) {
            return true;
        }
        return compassPainter.handleClick(x, y, state);
    }

    boolean isCompassTransitionInProgress() {
        return compassPainter.isTransitionInProgress();
    }

    void dispose() {
        compassPainter.dispose();
    }

    private void drawActiveNavigation(
            @NonNull Canvas canvas,
            @NonNull Rect area,
            @NonNull NavState state,
            boolean fullscreenRouteMode,
            float layoutScale
    ) {
        float padding = dp(16f);
        float left = area.left + padding;
        float top = area.top + padding;
        float width = Math.max(1f, area.width() - padding * 2f);
        float height = Math.max(1f, area.height() - padding * 2f);
        if (width >= height) {
            drawLandscape(canvas, state, left, top, width, height, padding, fullscreenRouteMode, layoutScale);
            return;
        }
        drawPortraitFallback(canvas, state, left, top, width, height, padding, fullscreenRouteMode, layoutScale);
    }

    private void drawLandscape(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float height,
            float padding,
            boolean fullscreenRouteMode,
            float layoutScale
    ) {
        float columnWidth = Math.max(1f, (width - padding) / 2f);
        float textWidth = columnWidth;
        float compassAreaWidth = columnWidth;
        float compassAreaLeft = left + textWidth + padding;
        if (fullscreenRouteMode) {
            compassOverlayBounds.set(compassAreaLeft, top, compassAreaLeft + compassAreaWidth, top + height);
            textColumnPainter.draw(canvas, state, left, top, textWidth, height, false, layoutScale);
            compassPainter.draw(
                    canvas,
                    state,
                    compassAreaLeft,
                    top,
                    compassAreaWidth,
                    height,
                    true,
                    compassOverlayBounds,
                    layoutScale
            );
            return;
        }
        float compassSize = Math.min(height, compassAreaWidth);
        float compassLeft = compassAreaLeft + (compassAreaWidth - compassSize) / 2f;
        float compassTop = top + (height - compassSize) / 2f;
        compassOverlayBounds.set(compassLeft, compassTop, compassLeft + compassSize, compassTop + compassSize);
        textColumnPainter.draw(canvas, state, left, top, textWidth, height, false, layoutScale);
        compassPainter.draw(
                canvas,
                state,
                compassLeft,
                compassTop,
                compassSize,
                compassSize,
                false,
                compassOverlayBounds,
                layoutScale
        );
    }

    private void drawPortraitFallback(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float height,
            float padding,
            boolean fullscreenRouteMode,
            float layoutScale
    ) {
        float compassSize = Math.min(width, height * 0.55f);
        float compassLeft = left + (width - compassSize) / 2f;
        compassOverlayBounds.set(compassLeft, top, compassLeft + compassSize, top + compassSize);
        if (fullscreenRouteMode) {
            compassPainter.draw(canvas, state, left, top, width, height, true, compassOverlayBounds, layoutScale);
            textColumnPainter.draw(
                    canvas,
                    state,
                    left,
                    top + compassSize + padding,
                    width,
                    height - compassSize - padding,
                    true,
                    layoutScale
            );
            return;
        }
        compassPainter.draw(
                canvas,
                state,
                compassLeft,
                top,
                compassSize,
                compassSize,
                false,
                compassOverlayBounds,
                layoutScale
        );
        textColumnPainter.draw(
                canvas,
                state,
                left,
                top + compassSize + padding,
                width,
                height - compassSize - padding,
                false,
                layoutScale
        );
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, carContext.getResources().getDisplayMetrics());
    }
}
