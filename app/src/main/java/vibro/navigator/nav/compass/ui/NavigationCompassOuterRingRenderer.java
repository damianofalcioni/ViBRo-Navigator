package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

final class NavigationCompassOuterRingRenderer {
    static final float CARDINAL_TEXT_SIZE_SCALE = 0.09f;
    private static final int TICK_COUNT = 24;
    private static final float OUTER_SCALE = 0.97f;
    private static final float TICK_LENGTH_SCALE = 0.018f;

    private final Paint minorTickPaint;
    private final Paint accentTickPaint;
    private final Paint cardinalPaint;

    NavigationCompassOuterRingRenderer(
            @NonNull Paint minorTickPaint,
            @NonNull Paint accentTickPaint,
            @NonNull Paint cardinalPaint
    ) {
        this.minorTickPaint = minorTickPaint;
        this.accentTickPaint = accentTickPaint;
        this.cardinalPaint = cardinalPaint;
    }

    void draw(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float radius,
            float layerRadius,
            float headingDegrees
    ) {
        canvas.save();
        canvas.rotate(-headingDegrees, cx, cy);
        for (int i = 0; i < TICK_COUNT; i++) {
            if (i % 6 == 0) {
                continue;
            }
            float angle = (360f / TICK_COUNT) * i - 90f;
            double radians = Math.toRadians(angle);
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);
            float outer = radius * OUTER_SCALE;
            float inner = outer - radius * TICK_LENGTH_SCALE;
            Paint paint = i % 3 == 1 ? accentTickPaint : minorTickPaint;
            canvas.drawLine(
                    cx + inner * cos,
                    cy + inner * sin,
                    cx + outer * cos,
                    cy + outer * sin,
                    paint
            );
        }
        drawCardinal(canvas, cx, cy - layerRadius, "N", radius);
        drawCardinal(canvas, cx + layerRadius, cy, "O", radius);
        drawCardinal(canvas, cx, cy + layerRadius, "S", radius);
        drawCardinal(canvas, cx - layerRadius, cy, "W", radius);
        canvas.restore();
    }

    private void drawCardinal(@NonNull Canvas canvas, float x, float y, @NonNull String label, float radius) {
        cardinalPaint.setTextSize(radius * CARDINAL_TEXT_SIZE_SCALE);
        Paint.FontMetrics fontMetrics = cardinalPaint.getFontMetrics();
        float baseline = y - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(label, x, baseline, cardinalPaint);
    }
}
