package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NavigationCompassHeadingGuideRenderer {
    private final Paint arrowPaint;
    private final Paint accuracyPaint;
    private final float arrowHalfWidth;
    private final float arrowHeight;
    private boolean enabled;

    NavigationCompassHeadingGuideRenderer(Context context, Paint arrowPaint, Paint accuracyPaint) {
        this.arrowPaint = arrowPaint;
        this.accuracyPaint = accuracyPaint;
        arrowHalfWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                NavigationCompassOrientationCueRenderer.MARKER_WIDTH_DP,
                context.getResources().getDisplayMetrics()) / 2f;
        arrowHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                NavigationCompassOrientationCueRenderer.MARKER_HEIGHT_DP,
                context.getResources().getDisplayMetrics());
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void drawArrow(@NonNull Canvas canvas, float cx, float cy, float radius) {
        if (!enabled) {
            return;
        }
        float tipY = cy - radius * NavigationCompassView.HEADING_GUIDE_ARROW_TIP_SCALE;
        float baseY = cy - Math.max(radius * NavigationCompassView.OUTER_COMPASS_LAYER_INNER_SCALE,
                radius - arrowHeight);
        canvas.drawLine(cx, cy, cx, tipY, arrowPaint);
        canvas.drawLine(cx, tipY, cx - arrowHalfWidth, baseY, arrowPaint);
        canvas.drawLine(cx, tipY, cx + arrowHalfWidth, baseY, arrowPaint);
    }

    void drawAccuracy(@NonNull Canvas canvas, float cx, float cy, float radius, @Nullable Float degrees) {
        if (!enabled || degrees == null) {
            return;
        }
        float guideRadius = radius * NavigationCompassView.OUTER_COMPASS_LAYER_INNER_SCALE;
        drawAccuracyLine(canvas, cx, cy, guideRadius, -90f - degrees);
        drawAccuracyLine(canvas, cx, cy, guideRadius, -90f + degrees);
    }

    private void drawAccuracyLine(Canvas canvas, float cx, float cy, float radius, float angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        float endX = cx + (float) Math.cos(radians) * radius;
        float endY = cy + (float) Math.sin(radians) * radius;
        canvas.drawLine(cx, cy, endX, endY, accuracyPaint);
    }
}
