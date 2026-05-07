package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassOrientationCueRenderer {

    private static final float ARC_RADIUS_OFFSET_DP = 5f;
    private static final float ARC_STROKE_WIDTH_DP = 1.1f;
    private static final float MARKER_ARC_GAP_DEGREES = 6f;
    private static final float MARKER_TIP_OFFSET_DP = 2f;
    private static final float MARKER_WIDTH_DP = 14f;
    private static final float MARKER_HEIGHT_DP = 8f;

    private final Paint cuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path markerPath = new Path();
    private final RectF arcBounds = new RectF();
    private boolean initialized;

    void draw(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float radius,
            float currentHeadingDegrees
    ) {
        CompassOrientationCue cue = state == null ? null : state.orientationCue;
        if (cue == null) {
            return;
        }
        ensurePaintInitialized(context);
        float sweepDegrees = signedSweepDegrees(currentHeadingDegrees, cue.targetHeadingDegrees);
        drawArc(canvas, context, cx, cy, radius, arcSweepWithMarkerGap(sweepDegrees));
        drawMarker(canvas, context, cx, cy, radius, sweepDegrees);
    }

    float signedSweepDegrees(float currentHeadingDegrees, float targetHeadingDegrees) {
        float normalized = (targetHeadingDegrees - currentHeadingDegrees + 540f) % 360f - 180f;
        return normalized == -180f ? 180f : normalized;
    }

    float arcSweepWithMarkerGap(float sweepDegrees) {
        if (Math.abs(sweepDegrees) <= MARKER_ARC_GAP_DEGREES) {
            return 0f;
        }
        return sweepDegrees > 0f
                ? sweepDegrees - MARKER_ARC_GAP_DEGREES
                : sweepDegrees + MARKER_ARC_GAP_DEGREES;
    }

    private void drawArc(
            @NonNull Canvas canvas,
            @NonNull Context context,
            float cx,
            float cy,
            float radius,
            float sweepDegrees
    ) {
        float arcRadius = radius + dp(context, ARC_RADIUS_OFFSET_DP);
        arcBounds.set(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius);
        cuePaint.setStyle(Paint.Style.STROKE);
        cuePaint.setStrokeWidth(dp(context, ARC_STROKE_WIDTH_DP));
        cuePaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(arcBounds, -90f, sweepDegrees, false, cuePaint);
    }

    private void drawMarker(
            @NonNull Canvas canvas,
            @NonNull Context context,
            float cx,
            float cy,
            float radius,
            float sweepDegrees
    ) {
        float tipY = cy - radius - dp(context, MARKER_TIP_OFFSET_DP);
        float baseY = tipY - dp(context, MARKER_HEIGHT_DP);
        float halfWidth = dp(context, MARKER_WIDTH_DP) / 2f;

        markerPath.reset();
        markerPath.moveTo(cx, tipY);
        markerPath.lineTo(cx - halfWidth, baseY);
        markerPath.lineTo(cx + halfWidth, baseY);
        markerPath.close();

        cuePaint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.rotate(sweepDegrees, cx, cy);
        canvas.drawPath(markerPath, cuePaint);
        canvas.restore();
    }

    private void ensurePaintInitialized(@NonNull Context context) {
        if (initialized) {
            return;
        }
        cuePaint.setColor(ContextCompat.getColor(context, R.color.danger));
        initialized = true;
    }

    private float dp(@NonNull Context context, float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }
}
