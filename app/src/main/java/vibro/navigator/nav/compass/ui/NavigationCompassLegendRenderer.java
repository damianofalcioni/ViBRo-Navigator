package vibro.navigator.nav.compass.ui;


import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.format.NavigationTextFormatter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


final class NavigationCompassLegendRenderer {
    private final RectF arcBounds = new RectF();
    private final RectF labelBounds = new RectF();
    private final NavigationRoutePathRenderer.PlotPoint rightAnchor = new NavigationRoutePathRenderer.PlotPoint();
    private final NavigationRoutePathRenderer.PlotPoint leftAnchor = new NavigationRoutePathRenderer.PlotPoint();
    private final float[] projectedAnchor = new float[2];

    void draw(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState compassState,
            float visibleRadiusMeters,
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
            @NonNull Paint headingAccuracyGuidePaint,
            boolean showHeadingAccuracy,
            boolean labelsAboveReference
    ) {
        if (compassState == null || visibleRadiusMeters <= 0f) {
            return;
        }

        Float visibleHeadingAccuracyDegrees = showHeadingAccuracy
                ? resolvedVisibleHeadingAccuracyDegrees(compassState, 5f, 85f)
                : null;
        drawReferences(canvas, cx, cy, radius, ringScales, distanceMarkWidthPx,
                visibleHeadingAccuracyDegrees, distanceMarkPaint, headingAccuracyGuidePaint);
        drawLabels(canvas, context, compassState, visibleRadiusMeters, cx, cy, radius, ringScales,
                outerDistanceRingScale, distanceMarkWidthPx, distanceLabelOffsetPx,
                distanceLegendRightPaint, distanceLegendLeftPaint, null,
                canvas.getWidth(), canvas.getHeight(), labelsAboveReference);
    }

    void drawReferences(
            @NonNull Canvas canvas, float cx, float cy, float radius, @NonNull float[] ringScales,
            float markWidthPx, @Nullable Float accuracyDegrees,
            @NonNull Paint markPaint, @NonNull Paint accuracyPaint
    ) {
        for (float ringScale : ringScales) {
            float ringRadius = radius * ringScale;
            if (accuracyDegrees != null) {
                drawHeadingAccuracyArc(canvas, cx, cy, ringRadius, accuracyDegrees, accuracyPaint);
            } else {
                float y = cy - ringRadius;
                canvas.drawLine(cx - markWidthPx / 2f, y, cx + markWidthPx / 2f, y, markPaint);
            }
        }
    }

    void drawLabels(
            @NonNull Canvas canvas, @NonNull Context context, @Nullable NavCompassState compassState,
            float visibleRadiusMeters, float cx, float cy, float radius, @NonNull float[] ringScales,
            float outerDistanceRingScale, float markWidthPx, float labelOffsetPx,
            @NonNull Paint rightPaint, @NonNull Paint leftPaint,
            @Nullable NavigationCompassPerspective perspective, float viewportWidth, float viewportHeight,
            boolean labelsAboveReference
    ) {
        if (compassState == null || visibleRadiusMeters <= 0f) {
            return;
        }
        Paint.FontMetrics fontMetrics = rightPaint.getFontMetrics();
        labelBounds.set(0f, 0f, viewportWidth, viewportHeight);
        for (float ringScale : ringScales) {
            float ringY = cy - radius * ringScale;
            rightAnchor.set(cx + markWidthPx / 2f, ringY);
            leftAnchor.set(cx - markWidthPx / 2f, ringY);
            float distanceMeters = visibleRadiusMeters * ringScale / outerDistanceRingScale;
            drawLabel(canvas, perspective, rightAnchor, labelOffsetPx,
                    formatDistanceLabel(context, distanceMeters), rightPaint, fontMetrics, labelsAboveReference);
            drawLabel(canvas, perspective, leftAnchor, -labelOffsetPx,
                    formatRingTimeLabel(context, compassState, distanceMeters), leftPaint, fontMetrics,
                    labelsAboveReference);
        }
    }

    private void drawLabel(
            @NonNull Canvas canvas, @Nullable NavigationCompassPerspective perspective,
            @NonNull NavigationRoutePathRenderer.PlotPoint anchor, float offsetPx,
            @NonNull String text, @NonNull Paint paint, @NonNull Paint.FontMetrics fontMetrics,
            boolean labelsAboveReference
    ) {
        float x = anchor.x;
        float y = anchor.y;
        if (perspective != null) {
            perspective.mapPoint(x, y, projectedAnchor);
            x = projectedAnchor[0];
            y = projectedAnchor[1];
        }
        x += offsetPx;
        y -= labelsAboveReference
                ? fontMetrics.descent + Math.abs(offsetPx)
                : (fontMetrics.ascent + fontMetrics.descent) / 2f;
        if (perspective != null || labelsAboveReference) {
            float textWidth = paint.measureText(text);
            float minimumX = paint.getTextAlign() == Paint.Align.RIGHT ? textWidth : 0f;
            float maximumX = paint.getTextAlign() == Paint.Align.LEFT ? labelBounds.right - textWidth : labelBounds.right;
            x = Math.max(minimumX, Math.min(maximumX, x));
            y = Math.max(-fontMetrics.ascent, Math.min(labelBounds.bottom - fontMetrics.descent, y));
        }
        canvas.drawText(text, x, y, paint);
    }

    @Nullable
    static Float resolvedVisibleHeadingAccuracyDegrees(
            @Nullable NavCompassState compassState,
            float minVisibleDegrees,
            float maxDegrees
    ) {
        if (compassState == null || compassState.displayMode.headingAccuracyDegrees == null) {
            return null;
        }
        float boundedAccuracyDegrees = Math.min(
                maxDegrees,
                Math.max(0f, compassState.displayMode.headingAccuracyDegrees)
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
        float radiusMeters = visibleRadiusMeters(compassState);
        if (radiusMeters <= 0f) {
            return 0f;
        }
        return radiusMeters * (ringScale / outerDistanceRingScale);
    }

    static float visibleRadiusMeters(@Nullable NavCompassState compassState) {
        return compassState == null ? 0f : compassState.radiusState.visibleRadiusMeters;
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

    @NonNull
    private static String formatDistanceLabel(@NonNull Context context, float distanceMeters) {
        return NavigationTextFormatter.formatDistance(context, distanceMeters);
    }

    @NonNull
    private static String formatRingTimeLabel(
            @NonNull Context context,
            @NonNull NavCompassState compassState,
            float distanceMeters
    ) {
        int seconds = (int) Math.round(distanceMeters / Math.max(1f, compassState.displayMode.referenceSpeedMps));
        return NavigationTextFormatter.formatTimeSeconds(context, seconds);
    }
}
