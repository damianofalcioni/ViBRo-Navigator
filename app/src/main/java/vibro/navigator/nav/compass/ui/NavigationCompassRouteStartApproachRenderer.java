package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassDestinationProjection;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassRouteStartApproachRenderer {
    private static final float TARGET_MARKER_RADIUS_DP = 5f;
    private static final float TARGET_STROKE_WIDTH_DP = 2f;
    private static final float TARGET_DOT_LENGTH_DP = 2f;
    private static final float TARGET_DOT_GAP_DP = 6f;
    private static final int TARGET_LINE_ALPHA = 220;

    private final Paint targetLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NavigationRoutePathRenderer.PlotPoint projectedPoint = new NavigationRoutePathRenderer.PlotPoint();
    private boolean initialized;

    void draw(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        if (state == null || state.radiusState.visibleRadiusMeters <= 0f) {
            return;
        }
        CompassDestinationProjection target = state.routeStartApproachProjection;
        if (target == null) {
            return;
        }
        ensurePaintsInitialized(context);
        NavigationRoutePathRenderer.PlotPoint position = resolveTargetPosition(
                state,
                target,
                cx,
                cy,
                routeRadius,
                headingDegrees
        );
        if (position == null) {
            return;
        }
        canvas.drawLine(cx, cy, position.x, position.y, targetLinePaint);
        canvas.drawCircle(position.x, position.y, dp(context, TARGET_MARKER_RADIUS_DP), targetPaint);
    }

    @Nullable
    NavigationRoutePathRenderer.PlotPoint resolveTargetPosition(
            @NonNull NavCompassState state,
            @NonNull CompassDestinationProjection target,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        float distanceMeters = (float) Math.hypot(target.eastMeters, target.northMeters);
        if (distanceMeters < 1f || state.radiusState.visibleRadiusMeters <= 0f) {
            return null;
        }
        NavigationCompassRouteProjector.projectHeadingUp(
                target.eastMeters,
                target.northMeters,
                headingDegrees,
                projectedPoint
        );
        float displayDistanceMeters = Math.min(distanceMeters, state.radiusState.visibleRadiusMeters);
        float scale = routeRadius * displayDistanceMeters / (state.radiusState.visibleRadiusMeters * distanceMeters);
        projectedPoint.set(
                cx + projectedPoint.x * scale,
                cy - projectedPoint.y * scale
        );
        return projectedPoint;
    }

    private void ensurePaintsInitialized(@NonNull Context context) {
        if (initialized) {
            return;
        }
        targetLinePaint.setStyle(Paint.Style.STROKE);
        targetLinePaint.setStrokeWidth(dp(context, TARGET_STROKE_WIDTH_DP));
        targetLinePaint.setStrokeCap(Paint.Cap.ROUND);
        targetLinePaint.setColor(ContextCompat.getColor(context, R.color.compass_route));
        targetLinePaint.setAlpha(TARGET_LINE_ALPHA);
        targetLinePaint.setPathEffect(new DashPathEffect(
                new float[] {
                        dp(context, TARGET_DOT_LENGTH_DP),
                        dp(context, TARGET_DOT_GAP_DP)
                },
                0f
        ));

        targetPaint.setStyle(Paint.Style.FILL);
        targetPaint.setColor(ContextCompat.getColor(context, R.color.compass_route));

        initialized = true;
    }

    boolean drawsTargetLineForTest(@NonNull NavCompassState state) {
        return state.routeStartApproachProjection != null;
    }

    Paint targetLinePaintForTest(@NonNull Context context) {
        ensurePaintsInitialized(context);
        return targetLinePaint;
    }

    private float dp(@NonNull Context context, float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }
}
