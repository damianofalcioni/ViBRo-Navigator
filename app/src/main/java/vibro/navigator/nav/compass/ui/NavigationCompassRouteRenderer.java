package vibro.navigator.nav.compass.ui;


import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassPassedRouteSegments;
import vibro.navigator.nav.compass.CompassRoutePoint;
import vibro.navigator.nav.compass.NavCompassState;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

import vibro.navigator.geo.LatLon;

final class NavigationCompassRouteRenderer {

    private static final float ROUTE_STROKE_WIDTH_DP = 3f;
    private static final float STRAIGHT_LINE_STROKE_WIDTH_DP = 2f;
    private static final float STRAIGHT_LINE_DOT_LENGTH_DP = 2f;
    private static final float STRAIGHT_LINE_DOT_GAP_DP = 6f;
    private static final int ROUTE_THRESHOLD_ALPHA = 51;
    private static final int ROUTE_DEFAULT_ALPHA = 255;
    private static final int STRAIGHT_LINE_ALPHA = 220;

    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint straightLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routeThresholdPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint passedRoutePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accuracyOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NavigationCompassStreetRenderer streetRenderer = new NavigationCompassStreetRenderer();
    private final NavigationCompassRouteMarkerRenderer markerRenderer = new NavigationCompassRouteMarkerRenderer();
    private final NavigationCompassRouteStartApproachRenderer routeStartApproachRenderer =
            new NavigationCompassRouteStartApproachRenderer();
    private final NavigationRoutePathRenderer routePathRenderer = new NavigationRoutePathRenderer();
    private boolean initialized;

    void drawRouteLayer(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        ensurePaintsInitialized(context);
        drawAccuracyOverlay(canvas, state, cx, cy, routeRadius);
        streetRenderer.draw(canvas, context, state, cx, cy, routeRadius, headingDegrees);
        drawRoute(canvas, context, state, cx, cy, routeRadius, headingDegrees);
        markerRenderer.drawStartPoint(canvas, context, state, cx, cy, routeRadius, headingDegrees);
        markerRenderer.drawHintMarkers(canvas, context, state, cx, cy, routeRadius, headingDegrees);
        routeStartApproachRenderer.draw(canvas, context, state, cx, cy, routeRadius, headingDegrees);
    }

    void drawDestinationPoint(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        markerRenderer.drawDestinationPoint(canvas, context, state, cx, cy, routeRadius, headingDegrees);
    }

    float resolveRouteThresholdStrokeWidthPx(@Nullable NavCompassState state, float routeRadius, float defaultStrokeWidthPx) {
        return NavigationRouteThreshold.resolveStrokeWidthPx(state, routeRadius, defaultStrokeWidthPx);
    }

    int resolveRoutePaintAlpha() {
        return ROUTE_DEFAULT_ALPHA;
    }

    int resolveRouteThresholdPaintAlpha(@Nullable NavCompassState state) {
        return shouldDrawRouteThresholdOverlay(state) ? ROUTE_THRESHOLD_ALPHA : ROUTE_DEFAULT_ALPHA;
    }

    boolean shouldDrawRouteThresholdOverlay(@Nullable NavCompassState state) {
        return NavigationRouteThreshold.shouldDrawOverlay(state);
    }

    Paint straightLinePaintForTest(@NonNull Context context) {
        ensurePaintsInitialized(context);
        return straightLinePaint;
    }

    private void ensurePaintsInitialized(@NonNull Context context) {
        if (initialized) {
            return;
        }
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeWidth(dp(context, ROUTE_STROKE_WIDTH_DP));
        routePaint.setStrokeJoin(Paint.Join.ROUND);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setColor(ContextCompat.getColor(context, R.color.compass_route));
        routePaint.setAlpha(ROUTE_DEFAULT_ALPHA);

        straightLinePaint.set(routePaint);
        straightLinePaint.setStrokeWidth(dp(context, STRAIGHT_LINE_STROKE_WIDTH_DP));
        straightLinePaint.setColor(ContextCompat.getColor(context, R.color.compass_route));
        straightLinePaint.setAlpha(STRAIGHT_LINE_ALPHA);
        straightLinePaint.setPathEffect(new DashPathEffect(
                new float[] {
                        dp(context, STRAIGHT_LINE_DOT_LENGTH_DP),
                        dp(context, STRAIGHT_LINE_DOT_GAP_DP)
                },
                0f
        ));

        routeThresholdPaint.set(routePaint);
        routeThresholdPaint.setAlpha(ROUTE_THRESHOLD_ALPHA);

        passedRoutePaint.setStyle(Paint.Style.STROKE);
        passedRoutePaint.setStrokeWidth(dp(context, ROUTE_STROKE_WIDTH_DP));
        passedRoutePaint.setStrokeJoin(Paint.Join.ROUND);
        passedRoutePaint.setStrokeCap(Paint.Cap.ROUND);
        passedRoutePaint.setColor(ContextCompat.getColor(context, R.color.compass_route_passed));

        accuracyOverlayPaint.setStyle(Paint.Style.FILL);
        accuracyOverlayPaint.setColor(ContextCompat.getColor(context, R.color.compass_accent));
        accuracyOverlayPaint.setAlpha(128);
        initialized = true;
    }

    private void drawAccuracyOverlay(
            @NonNull Canvas canvas,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius
    ) {
        if (state == null
                || state.radiusState.visibleRadiusMeters <= 0f
                || state.radiusState.accuracyRadiusMeters <= 0f) {
            return;
        }

        float overlayRadius = routeRadius
                * (state.radiusState.accuracyRadiusMeters / state.radiusState.visibleRadiusMeters);
        canvas.drawCircle(cx, cy, Math.min(routeRadius, overlayRadius), accuracyOverlayPaint);
    }

    private void drawRoute(
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

        if (state.displayMode.straightLineMode) {
            drawStraightLineRoute(canvas, state, cx, cy, routeRadius, headingDegrees);
            return;
        }

        routePaint.setStrokeWidth(dp(context, ROUTE_STROKE_WIDTH_DP));
        routePaint.setAlpha(resolveRoutePaintAlpha());
        routeThresholdPaint.setStrokeWidth(resolveRouteThresholdStrokeWidthPx(
                state,
                routeRadius,
                dp(context, ROUTE_STROKE_WIDTH_DP)
        ));
        routeThresholdPaint.setAlpha(resolveRouteThresholdPaintAlpha(state));
        passedRoutePaint.setStrokeWidth(dp(context, ROUTE_STROKE_WIDTH_DP));
        float scale = routeRadius / state.radiusState.visibleRadiusMeters;
        if (state.hasRouteGeometry()) {
            drawRouteThresholdGeometrySegment(
                    canvas,
                    state,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    state.remainingRouteStartSamplePointIndex(),
                    state.routeSamplePointCount()
            );
            drawArchivedPassedRouteSegments(canvas, state, cx, cy, scale, headingDegrees);
            drawRouteGeometrySegment(
                    canvas,
                    state,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    0,
                    state.passedRouteSamplePointCount(),
                    passedRoutePaint
            );
            drawRouteGeometrySegment(
                    canvas,
                    state,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    state.remainingRouteStartSamplePointIndex(),
                    state.routeSamplePointCount(),
                    routePaint
            );
            return;
        }
        drawRouteSegment(canvas, state, cx, cy, scale, headingDegrees, state.passedRoutePoints, passedRoutePaint);
        drawRouteThresholdSegment(canvas, state, cx, cy, scale, headingDegrees, state.routePoints);
        drawRouteSegment(canvas, state, cx, cy, scale, headingDegrees, state.routePoints, routePaint);
    }

    private void drawArchivedPassedRouteSegments(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees
    ) {
        CompassPassedRouteSegments archivedSegments = state.archivedPassedRouteSegments();
        for (int segmentIndex = 0; segmentIndex < archivedSegments.segmentCount(); segmentIndex++) {
            drawArchivedPassedRouteSegment(
                    canvas,
                    state,
                    archivedSegments,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    segmentIndex
            );
        }
    }

    private void drawArchivedPassedRouteSegment(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            @NonNull CompassPassedRouteSegments archivedSegments,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int segmentIndex
    ) {
        int pointCount = archivedSegments.samplePointCount(segmentIndex);
        if (pointCount < 2) {
            return;
        }
        drawProjectedRouteSegment(canvas, state, cx, cy, scale, 0, pointCount, passedRoutePaint, (i, out) -> {
            LatLon point = archivedSegments.samplePointAt(segmentIndex, i);
            if (point == null) {
                return false;
            }
            NavigationCompassRouteProjector.projectRoutePoint(state, point, headingDegrees, out);
            return true;
        });
    }

    private void drawStraightLineRoute(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        float scale = routeRadius / state.radiusState.visibleRadiusMeters;
        if (state.hasRouteGeometry()) {
            drawRouteGeometrySegment(
                    canvas,
                    state,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    state.remainingRouteStartSamplePointIndex(),
                    state.routeSamplePointCount(),
                    straightLinePaint
            );
            return;
        }
        drawRouteSegment(canvas, state, cx, cy, scale, headingDegrees, state.routePoints, straightLinePaint);
    }

    private void drawRouteThresholdGeometrySegment(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex
    ) {
        if (!shouldDrawRouteThresholdOverlay(state)) {
            return;
        }
        drawRouteGeometrySegment(canvas, state, cx, cy, scale, headingDegrees, startIndex, endIndex, routeThresholdPaint);
    }

    private void drawRouteThresholdSegment(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            @NonNull List<CompassRoutePoint> points
    ) {
        if (!shouldDrawRouteThresholdOverlay(state)) {
            return;
        }
        drawRouteSegment(canvas, state, cx, cy, scale, headingDegrees, points, routeThresholdPaint);
    }

    private void drawRouteGeometrySegment(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex,
            @NonNull Paint strokePaint
    ) {
        if (startIndex < 0 || endIndex <= startIndex) {
            return;
        }

        drawProjectedRouteSegment(canvas, state, cx, cy, scale, startIndex, endIndex, strokePaint, (i, out) -> {
            LatLon point = state.routeSamplePointAt(i);
            if (point == null) {
                return false;
            }
            NavigationCompassRouteProjector.projectRoutePoint(state, point, headingDegrees, out);
            return true;
        });
    }

    private void drawRouteSegment(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            @NonNull List<CompassRoutePoint> points,
            @NonNull Paint strokePaint
    ) {
        if (points.isEmpty()) {
            return;
        }

        drawProjectedRouteSegment(canvas, state, cx, cy, scale, 0, points.size(), strokePaint, (i, out) -> {
            CompassRoutePoint point = points.get(i);
            NavigationCompassRouteProjector.projectHeadingUp(
                    point.eastMeters,
                    point.northMeters,
                    headingDegrees,
                    out
            );
            return true;
        });
    }

    private void drawProjectedRouteSegment(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            int startIndex,
            int endIndex,
            @NonNull Paint strokePaint,
            @NonNull ProjectedRoutePointSource pointSource
    ) {
        routePathRenderer.drawProjectedRouteSegment(
                canvas,
                cx,
                cy,
                scale,
                startIndex,
                endIndex,
                state.radiusState.visibleRadiusMeters,
                resolveRouteDrawPaddingMeters(state),
                strokePaint,
                pointSource::project
        );
    }

    private float resolveRouteDrawPaddingMeters(@NonNull NavCompassState state) {
        float thresholdPaddingMeters = Math.max(
                state.radiusState.routeThresholdMeters,
                state.radiusState.accuracyRadiusMeters
        );
        return Math.max(24f, thresholdPaddingMeters);
    }

    private float dp(@NonNull Context context, float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    private interface ProjectedRoutePointSource {
        boolean project(int index, @NonNull NavigationRoutePathRenderer.PlotPoint out);
    }
}
