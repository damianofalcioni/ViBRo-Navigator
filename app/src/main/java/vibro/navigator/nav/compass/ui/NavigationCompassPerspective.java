package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.CompassPerspectiveScale;

/** Tilts the heading-up ground plane using either an affine or a central projection. */
final class NavigationCompassPerspective {
    private final Matrix transform = new Matrix();
    private final float[] matrixValues = new float[9];
    private final RectF groundBounds = new RectF();
    private boolean centralPerspective;
    boolean configure(float cx, float cy, float radius, float progress) {
        return configure(cx, cy, radius, progress, 0f);
    }

    boolean configure(float cx, float cy, float radius, float progress, float centerYOffset) {
        return configure(cx, cy, radius, progress, centerYOffset, false);
    }

    boolean configure(float cx, float cy, float radius, float progress, float centerYOffset,
                      boolean centralPerspective) {
        if (!Float.isFinite(radius) || radius <= 1f) {
            return false;
        }
        this.centralPerspective = centralPerspective;
        float verticalScale = CompassPerspectiveScale.verticalScale(progress);
        if (centralPerspective) {
            configureCentral(cx, cy, radius, progress, centerYOffset, verticalScale);
        } else {
            transform.setScale(1f, verticalScale, cx, cy);
            transform.postTranslate(0f, centerYOffset);
        }
        return true;
    }

    private void configureCentral(float cx, float cy, float radius, float progress,
                                  float centerYOffset, float verticalScale) {
        // In local ground coordinates: x' = x / (1 - k*y), y' = s*y / (1 - k*y).
        // Forward ground has negative y, so it shrinks; nearby ground grows.
        matrixValues[0] = 1f;
        matrixValues[4] = verticalScale;
        matrixValues[7] = -CompassPerspectiveScale.depthFactor(progress) / radius;
        matrixValues[8] = 1f;
        transform.setValues(matrixValues);
        transform.preTranslate(-cx, -cy);
        transform.postTranslate(cx, cy + centerYOffset);
        groundBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
    }

    void mapPoint(float x, float y, @NonNull float[] out) {
        out[0] = x;
        out[1] = y;
        transform.mapPoints(out);
    }

    void concat(@NonNull Canvas canvas) {
        canvas.concat(transform);
        if (centralPerspective) {
            // Clip before drawing any geometry outside the prepared plane or behind the camera.
            canvas.clipRect(groundBounds);
        }
    }

}
