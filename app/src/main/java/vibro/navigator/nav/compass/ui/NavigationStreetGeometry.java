package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Arrays;

/** Caches simplified north-up segments for one street category without per-frame allocation. */
final class NavigationStreetGeometry {
    private float[] lines = new float[64];
    private int count;
    private float lastX;
    private float lastY;

    void moveTo(float x, float y) {
        lastX = x;
        lastY = y;
    }

    void lineTo(float x, float y) {
        if (count + 4 > lines.length) {
            lines = Arrays.copyOf(lines, lines.length * 2);
        }
        lines[count++] = lastX;
        lines[count++] = lastY;
        lines[count++] = x;
        lines[count++] = y;
        lastX = x;
        lastY = y;
    }

    void draw(Canvas canvas, Paint paint) {
        if (count == 0) {
            return;
        }
        canvas.drawLines(lines, 0, count, paint);
    }
}
