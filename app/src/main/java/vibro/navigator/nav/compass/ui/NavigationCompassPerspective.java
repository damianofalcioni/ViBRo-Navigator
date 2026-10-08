package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Matrix;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.CompassPerspectiveScale;

/** Orthographically tilts the heading-up route plane with an optional lower screen center. */
final class NavigationCompassPerspective {
    private final Matrix transform = new Matrix();
    boolean configure(float cx, float cy, float radius, float progress) {
        return configure(cx, cy, radius, progress, 0f);
    }

    boolean configure(float cx, float cy, float radius, float progress, float centerYOffset) {
        if (!Float.isFinite(radius) || radius <= 1f) {
            return false;
        }
        float verticalScale = CompassPerspectiveScale.verticalScale(progress);
        transform.setScale(1f, verticalScale, cx, cy);
        transform.postTranslate(0f, centerYOffset);
        return true;
    }

    void mapPoint(float x, float y, @NonNull float[] out) {
        out[0] = x;
        out[1] = y;
        transform.mapPoints(out);
    }

    void concat(@NonNull Canvas canvas) {
        canvas.concat(transform);
    }

}
