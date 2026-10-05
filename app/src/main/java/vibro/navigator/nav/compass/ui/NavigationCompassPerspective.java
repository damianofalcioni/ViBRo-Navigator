package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Matrix;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.CompassPerspectiveScale;

/** Tilts the heading-up route plane with an optional lower screen center and bounded forward distances. */
final class NavigationCompassPerspective {
    private final Matrix transform = new Matrix();
    private final float[] matrixValues = new float[9];
    boolean configure(float cx, float cy, float radius, float progress) {
        return configure(cx, cy, radius, progress, 0f);
    }

    boolean configure(float cx, float cy, float radius, float progress, float centerYOffset) {
        if (!Float.isFinite(radius) || radius <= 1f) {
            return false;
        }
        float depth = CompassPerspectiveScale.depth(progress);
        float verticalScale = CompassPerspectiveScale.verticalScale(progress);
        float screenCy = cy + centerYOffset;
        float denominatorY = -depth / radius;
        float denominatorOffset = 1f - denominatorY * cy;
        matrixValues[Matrix.MSCALE_X] = 1f;
        matrixValues[Matrix.MSKEW_X] = cx * denominatorY;
        matrixValues[Matrix.MTRANS_X] = cx * (denominatorOffset - 1f);
        matrixValues[Matrix.MSKEW_Y] = 0f;
        matrixValues[Matrix.MSCALE_Y] = verticalScale + screenCy * denominatorY;
        matrixValues[Matrix.MTRANS_Y] = screenCy * denominatorOffset - cy * verticalScale;
        matrixValues[Matrix.MPERSP_0] = 0f;
        matrixValues[Matrix.MPERSP_1] = denominatorY;
        matrixValues[Matrix.MPERSP_2] = denominatorOffset;
        transform.setValues(matrixValues);
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
