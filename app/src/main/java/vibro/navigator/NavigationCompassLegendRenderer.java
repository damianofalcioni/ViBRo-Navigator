package vibro.navigator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.NavCompassState;
import vibro.navigator.nav.NavigationTextFormatter;

final class NavigationCompassLegendRenderer {
    private final RectF arcBounds = new RectF();
    private final NavigationRoutePathRenderer.PlotPoint rightAnchor = new NavigationRoutePathRenderer.PlotPoint();
    private final NavigationRoutePathRenderer.PlotPoint leftAnchor = new NavigationRoutePathRenderer.PlotPoint();

    void draw(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState compassState,
            float cx,
            float cy,
            float radius,
            @NonNull float[] ringScales,
            float outerDistanceRingScale,
            float distanceMarkWidthPx,
            float distanceLabelOffsetPx,
            @NonNull Paint distanceMarkPaint,
            @NonNull Paint distanceLegendRightPaint,
            @NonNull Paint distanceLegendLeftPaint,
            @NonNull Paint headingAccuracyGuidePaint
    ) {
        if (compassState == null || compassState.visibleRadiusMeters <= 0f) {
            return;
        }

        Float visibleHeadingAccuracyDegrees = resolvedVisibleHeadingAccuracyDegrees(compassState, 5f, 85f);
        Paint.FontMetrics fontMetrics = distanceLegendRightPaint.getFontMetrics();
        float labelBaselineOffset = -(fontMetrics.ascent + fontMetrics.descent) / 2f;
        float dashHalfWidth = distanceMarkWidthPx / 2f;
        for (float ringScale : ringScales) {
            drawLegendRow(
                    canvas,
                    context,
                    compassState,
                    cx,
                    cy,
                    radius,
                    ringScale,
                    outerDistanceRingScale,
                    dashHalfWidth,
                    distanceLabelOffsetPx,
                    labelBaselineOffset,
                    visibleHeadingAccuracyDegrees,
                    distanceMarkPaint,
                    distanceLegendRightPaint,
                    distanceLegendLeftPaint,
                    headingAccuracyGuidePaint
            );
        }
    }

    @Nullable
    static Float resolvedVisibleHeadingAccuracyDegrees(
            @Nullable NavCompassState compassState,
            float minVisibleDegrees,
            float maxDegrees
    ) {
        if (compassState == null || compassState.headingAccuracyDegrees == null) {
            return null;
        }
        float boundedAccuracyDegrees = Math.min(
                maxDegrees,
                Math.max(0f, compassState.headingAccuracyDegrees)
        );
        if (boundedAccuracyDegrees <= 0f) {
            return null;
        }
        return Math.max(minVisibleDegrees, boundedAccuracyDegrees);
    }

    static float resolveLegendRingDistanceMeters(
            @Nullable NavCompassState compassState,
            float ringScale,
            float outerDistanceRingScale
    ) {
        if (compassState == null || compassState.visibleRadiusMeters <= 0f) {
            return 0f;
        }
        return compassState.visibleRadiusMeters * (ringScale / outerDistanceRingScale);
    }

    private void drawLegendRow(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @NonNull NavCompassState compassState,
            float cx,
            float cy,
            float radius,
            float ringScale,
            float outerDistanceRingScale,
            float dashHalfWidth,
            float distanceLabelOffsetPx,
            float labelBaselineOffset,
            @Nullable Float visibleHeadingAccuracyDegrees,
            @NonNull Paint distanceMarkPaint,
            @NonNull Paint distanceLegendRightPaint,
            @NonNull Paint distanceLegendLeftPaint,
            @NonNull Paint headingAccuracyGuidePaint
    ) {
        float y = cy - radius * ringScale;
        float ringDistanceMeters = resolveLegendRingDistanceMeters(
                compassState,
                ringScale,
                outerDistanceRingScale
        );
        String distanceLabel = formatDistanceLabel(context, ringDistanceMeters);
        String secondsLabel = formatRingTimeLabel(context, compassState, ringDistanceMeters);
        if (visibleHeadingAccuracyDegrees != null) {
            drawHeadingAccuracyLegendRow(
                    canvas,
                    cx,
                    cy,
                    radius * ringScale,
                    visibleHeadingAccuracyDegrees,
                    labelBaselineOffset,
                    distanceLabelOffsetPx,
                    distanceLabel,
                    secondsLabel,
                    distanceLegendRightPaint,
                    distanceLegendLeftPaint,
                    headingAccuracyGuidePaint
            );
            return;
        }
        drawPlainLegendRow(
                canvas,
                cx,
                y,
                dashHalfWidth,
                distanceLabelOffsetPx,
                labelBaselineOffset,
                distanceLabel,
                secondsLabel,
                distanceMarkPaint,
                distanceLegendRightPaint,
                distanceLegendLeftPaint
        );
    }

    private void drawHeadingAccuracyLegendRow(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float ringRadius,
            float visibleHeadingAccuracyDegrees,
            float labelBaselineOffset,
            float distanceLabelOffsetPx,
            @NonNull String distanceLabel,
            @NonNull String secondsLabel,
            @NonNull Paint distanceLegendRightPaint,
            @NonNull Paint distanceLegendLeftPaint,
            @NonNull Paint headingAccuracyGuidePaint
    ) {
        drawHeadingAccuracyArc(
                canvas,
                cx,
                cy,
                ringRadius,
                visibleHeadingAccuracyDegrees,
                headingAccuracyGuidePaint
        );
        resolveHeadingAccuracyRingIntersection(
                rightAnchor,
                cx,
                cy,
                ringRadius,
                -90f + visibleHeadingAccuracyDegrees
        );
        resolveHeadingAccuracyRingIntersection(
                leftAnchor,
                cx,
                cy,
                ringRadius,
                -90f - visibleHeadingAccuracyDegrees
        );
        canvas.drawText(
                distanceLabel,
                rightAnchor.x + distanceLabelOffsetPx,
                rightAnchor.y + labelBaselineOffset,
                distanceLegendRightPaint
        );
        canvas.drawText(
                secondsLabel,
                leftAnchor.x - distanceLabelOffsetPx,
                leftAnchor.y + labelBaselineOffset,
                distanceLegendLeftPaint
        );
    }

    private void drawPlainLegendRow(
            @NonNull Canvas canvas,
            float cx,
            float y,
            float dashHalfWidth,
            float distanceLabelOffsetPx,
            float labelBaselineOffset,
            @NonNull String distanceLabel,
            @NonNull String secondsLabel,
            @NonNull Paint distanceMarkPaint,
            @NonNull Paint distanceLegendRightPaint,
            @NonNull Paint distanceLegendLeftPaint
    ) {
        float labelX = cx + dashHalfWidth + distanceLabelOffsetPx;
        float secondsX = cx - dashHalfWidth - distanceLabelOffsetPx;
        canvas.drawLine(cx - dashHalfWidth, y, cx + dashHalfWidth, y, distanceMarkPaint);
        canvas.drawText(distanceLabel, labelX, y + labelBaselineOffset, distanceLegendRightPaint);
        canvas.drawText(secondsLabel, secondsX, y + labelBaselineOffset, distanceLegendLeftPaint);
    }

    private void drawHeadingAccuracyArc(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float arcRadius,
            float visibleHeadingAccuracyDegrees,
            @NonNull Paint headingAccuracyGuidePaint
    ) {
        arcBounds.set(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius);
        canvas.drawArc(
                arcBounds,
                -90f - visibleHeadingAccuracyDegrees,
                visibleHeadingAccuracyDegrees * 2f,
                false,
                headingAccuracyGuidePaint
        );
    }

    private static void resolveHeadingAccuracyRingIntersection(
            @NonNull NavigationRoutePathRenderer.PlotPoint out,
            float cx,
            float cy,
            float ringRadius,
            float angleDegrees
    ) {
        double radians = Math.toRadians(angleDegrees);
        out.set(
                cx + (float) Math.cos(radians) * ringRadius,
                cy + (float) Math.sin(radians) * ringRadius
        );
    }

    @NonNull
    private static String formatDistanceLabel(@NonNull Context context, float distanceMeters) {
        if (distanceMeters >= 1000f) {
            return context.getString(R.string.format_distance_km, distanceMeters / 1000f);
        }
        return context.getString(R.string.format_distance_m, distanceMeters);
    }

    @NonNull
    private static String formatRingTimeLabel(
            @NonNull Context context,
            @NonNull NavCompassState compassState,
            float distanceMeters
    ) {
        int seconds = (int) Math.round(distanceMeters / Math.max(1f, compassState.referenceSpeedMps));
        return NavigationTextFormatter.formatTimeSeconds(context, seconds);
    }
}
