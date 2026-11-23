package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceContainer;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

/** The navigation surface contains map/route content; the host template owns guidance and controls. */
final class ViBRoAutoSurfacePainter {
    private final CarContext carContext;
    private final RectF overlayBounds = new RectF();
    private final ViBRoAutoCompassPainter compassPainter;

    ViBRoAutoSurfacePainter(@NonNull CarContext carContext,
            @NonNull ViBRoAutoCompassStreetViewportSink viewportSink,
            @NonNull ElapsedRealtimeClock clock) {
        this.carContext = carContext;
        compassPainter = new ViBRoAutoCompassPainter(carContext, viewportSink, clock);
    }

    void draw(@NonNull Canvas canvas, @NonNull SurfaceContainer container,
            @NonNull Rect stableArea, @Nullable NavState state) {
        canvas.drawColor(AndroidAppTheme.color(carContext, R.attr.vibroBackgroundColor));
        if (state == null) {
            return;
        }
        Rect area = stableArea.isEmpty() ? new Rect(0, 0, container.getWidth(), container.getHeight()) : stableArea;
        float scale = ViBRoAutoRenderScale.fromContentHeight(carContext, area.height());
        overlayBounds.set(area);
        compassPainter.draw(canvas, state, area.left, area.top, area.width(), area.height(),
                true, overlayBounds, scale);
    }

    boolean handleClick(float x, float y, @NonNull NavState state) {
        return compassPainter.handleClick(x, y, state);
    }

    boolean handleScroll(float x, float y, @NonNull NavState state) {
        return compassPainter.handleScroll(x, y, state);
    }

    boolean handleScale(float x, float y, float factor, @NonNull NavState state) {
        return compassPainter.handleScale(x, y, factor, state);
    }

    boolean isCompassTransitionInProgress() {
        return compassPainter.isTransitionInProgress();
    }

    void refreshTheme() {
        compassPainter.refreshTheme();
    }

    void resetCompass() {
        compassPainter.reset();
    }

    void dispose() {
        compassPainter.dispose();
    }
}
