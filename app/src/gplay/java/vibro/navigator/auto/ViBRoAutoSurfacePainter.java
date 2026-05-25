package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceContainer;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavState;

final class ViBRoAutoSurfacePainter {

    private static final float LANDSCAPE_TEXT_WIDTH_RATIO = 0.48f;

    private final CarContext carContext;
    private final Rect fullSurfaceArea = new Rect();
    private final ViBRoAutoTextColumnPainter textColumnPainter;
    private final ViBRoAutoCompassPainter compassPainter;

    ViBRoAutoSurfacePainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls
    ) {
        this.carContext = carContext;
        textColumnPainter = new ViBRoAutoTextColumnPainter(carContext, controls);
        compassPainter = new ViBRoAutoCompassPainter(carContext, controls);
    }

    void draw(
            @NonNull Canvas canvas,
            @NonNull SurfaceContainer container,
            @NonNull Rect stableArea,
            @Nullable NavState state
    ) {
        canvas.drawColor(Color.BLACK);
        fullSurfaceArea.set(0, 0, container.getWidth(), container.getHeight());
        Rect contentArea = stableArea.isEmpty() ? fullSurfaceArea : stableArea;
        if (state == null) {
            textColumnPainter.drawMessage(canvas, contentArea, carContext.getString(R.string.auto_connecting));
            return;
        }
        drawActiveNavigation(canvas, contentArea, state);
    }

    boolean handleClick(float x, float y, @NonNull NavState state) {
        if (textColumnPainter.handleClick(x, y, state)) {
            return false;
        }
        return compassPainter.handleClick(x, y, state);
    }

    boolean isCompassTransitionInProgress() {
        return compassPainter.isTransitionInProgress();
    }

    void dispose() {
        compassPainter.dispose();
    }

    private void drawActiveNavigation(@NonNull Canvas canvas, @NonNull Rect area, @NonNull NavState state) {
        float padding = dp(16f);
        float left = area.left + padding;
        float top = area.top + padding;
        float width = Math.max(1f, area.width() - padding * 2f);
        float height = Math.max(1f, area.height() - padding * 2f);
        if (width >= height) {
            drawLandscape(canvas, state, left, top, width, height, padding);
            return;
        }
        drawPortraitFallback(canvas, state, left, top, width, height, padding);
    }

    private void drawLandscape(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float height,
            float padding
    ) {
        float textWidth = width * LANDSCAPE_TEXT_WIDTH_RATIO;
        float compassAreaWidth = Math.max(1f, width - textWidth - padding);
        float compassSize = Math.min(height, compassAreaWidth);
        float compassLeft = left + textWidth + padding + (compassAreaWidth - compassSize) / 2f;
        float compassTop = top + (height - compassSize) / 2f;
        textColumnPainter.draw(canvas, state, left, top, textWidth, height);
        compassPainter.draw(canvas, state, compassLeft, compassTop, compassSize);
    }

    private void drawPortraitFallback(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float height,
            float padding
    ) {
        float compassSize = Math.min(width, height * 0.55f);
        float compassLeft = left + (width - compassSize) / 2f;
        compassPainter.draw(canvas, state, compassLeft, top, compassSize);
        textColumnPainter.draw(canvas, state, left, top + compassSize + padding, width, height - compassSize - padding);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, carContext.getResources().getDisplayMetrics());
    }
}
