package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

final class NavigationCompassPausedRingRenderer {
    private NavigationCompassPausedRingRenderer() {
    }

    static void draw(
            @NonNull Canvas canvas,
            boolean navigationPaused,
            float cx,
            float cy,
            float radius,
            float outerLayerRadius,
            float outerLayerStrokeScale,
            @NonNull Paint paint
    ) {
        if (!navigationPaused) {
            return;
        }
        paint.setStrokeWidth(radius * outerLayerStrokeScale);
        canvas.drawCircle(cx, cy, outerLayerRadius, paint);
    }
}
