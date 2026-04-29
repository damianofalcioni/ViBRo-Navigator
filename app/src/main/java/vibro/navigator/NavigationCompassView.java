package vibro.navigator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.NavCompassState;
import vibro.navigator.nav.NavigationTextFormatter;

import java.util.List;

public final class NavigationCompassView extends View {

    private static final int DEFAULT_SIZE_DP = 280;
    private static final int OUTER_TICK_COUNT = 24;
    private static final float[] DISTANCE_RING_SCALES = new float[]{0.82f, 0.55f, 0.28f};
    private static final float CENTER_MARKER_DOT_RADIUS_SCALE = 0.02f;
    private static final float HEADING_GUIDE_TOP_SCALE = 0.94f;
    private static final float HEADING_GUIDE_ARROW_WIDTH_DP = 12f;
    private static final float HEADING_GUIDE_ARROW_HEIGHT_DP = 10f;
    private static final float HEADING_ACCURACY_GUIDE_MIN_VISIBLE_DEGREES = 5f;
    private static final float HEADING_ACCURACY_GUIDE_MAX_DEGREES = 85f;
    private static final float DISTANCE_MARK_WIDTH_DP = 6f;
    private static final float DISTANCE_LABEL_OFFSET_DP = 6f;
    private static final float ROUTE_MARKER_RADIUS_DP = 2.5f;
    private static final float DESTINATION_MARKER_RADIUS_DP = 4f;
    private static final float OUTER_DISTANCE_RING_SCALE = DISTANCE_RING_SCALES[0];
    private static final float ROUTE_STROKE_WIDTH_DP = 3f;
    private static final int ROUTE_THRESHOLD_ALPHA = 51;
    private static final int ROUTE_DEFAULT_ALPHA = 255;

    @Nullable
    private NavCompassState compassState;

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardinalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routeThresholdPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint passedRoutePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accuracyOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routeMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headingGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headingAccuracyGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint distanceMarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint distanceLegendRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint distanceLegendLeftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint destinationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path compassClipPath = new Path();
    private final RectF arcBounds = new RectF();
    private final NavigationRoutePathRenderer.PlotPoint projectedPoint = new NavigationRoutePathRenderer.PlotPoint();
    private final NavigationRoutePathRenderer.PlotPoint auxiliaryPoint = new NavigationRoutePathRenderer.PlotPoint();
    private final NavigationRoutePathRenderer.PlotPoint destinationPoint = new NavigationRoutePathRenderer.PlotPoint();
    private final NavigationRoutePathRenderer routePathRenderer = new NavigationRoutePathRenderer();

    public NavigationCompassView(Context context) {
        super(context);
        init();
    }

    public NavigationCompassView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NavigationCompassView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        initBasePaints();
        initTickPaints();
        initRoutePaints();
        initMarkerPaints();
        initGuidePaints();
        initLegendPaints();
    }

    private void initBasePaints() {
        surfacePaint.setStyle(Paint.Style.FILL);
        surfacePaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_surface));

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(2f));
        ringPaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_ring));
    }

    private void initTickPaints() {
        majorTickPaint.setStyle(Paint.Style.STROKE);
        majorTickPaint.setStrokeWidth(dp(2.4f));
        majorTickPaint.setStrokeCap(Paint.Cap.ROUND);
        majorTickPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));

        minorTickPaint.setStyle(Paint.Style.STROKE);
        minorTickPaint.setStrokeWidth(dp(1.8f));
        minorTickPaint.setStrokeCap(Paint.Cap.ROUND);
        minorTickPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        minorTickPaint.setAlpha(220);

        accentTickPaint.setStyle(Paint.Style.STROKE);
        accentTickPaint.setStrokeWidth(dp(2.4f));
        accentTickPaint.setStrokeCap(Paint.Cap.ROUND);
        accentTickPaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_accent));

        cardinalPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        cardinalPaint.setTextAlign(Paint.Align.CENTER);
        cardinalPaint.setFakeBoldText(false);
    }

    private void initRoutePaints() {
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeWidth(dp(ROUTE_STROKE_WIDTH_DP));
        routePaint.setStrokeJoin(Paint.Join.ROUND);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_route));
        routePaint.setAlpha(ROUTE_DEFAULT_ALPHA);

        routeThresholdPaint.set(routePaint);
        routeThresholdPaint.setAlpha(ROUTE_THRESHOLD_ALPHA);

        passedRoutePaint.setStyle(Paint.Style.STROKE);
        passedRoutePaint.setStrokeWidth(dp(ROUTE_STROKE_WIDTH_DP));
        passedRoutePaint.setStrokeJoin(Paint.Join.ROUND);
        passedRoutePaint.setStrokeCap(Paint.Cap.ROUND);
        passedRoutePaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_route_passed));
    }

    private void initMarkerPaints() {
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_center));

        routeMarkerPaint.setStyle(Paint.Style.FILL);
        routeMarkerPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        routeMarkerPaint.setAlpha(128);

        accuracyOverlayPaint.setStyle(Paint.Style.FILL);
        accuracyOverlayPaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_accent));
        accuracyOverlayPaint.setAlpha(128);
    }

    private void initGuidePaints() {
        headingGuidePaint.setStyle(Paint.Style.STROKE);
        headingGuidePaint.setStrokeWidth(dp(1.2f));
        headingGuidePaint.setStrokeCap(Paint.Cap.ROUND);
        headingGuidePaint.setStrokeJoin(Paint.Join.ROUND);
        headingGuidePaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        headingGuidePaint.setAlpha(128);

        headingAccuracyGuidePaint.setStyle(Paint.Style.STROKE);
        headingAccuracyGuidePaint.setStrokeWidth(dp(1.2f));
        headingAccuracyGuidePaint.setStrokeCap(Paint.Cap.ROUND);
        headingAccuracyGuidePaint.setStrokeJoin(Paint.Join.ROUND);
        headingAccuracyGuidePaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        headingAccuracyGuidePaint.setAlpha(128);

        distanceMarkPaint.setStyle(Paint.Style.STROKE);
        distanceMarkPaint.setStrokeWidth(dp(1.2f));
        distanceMarkPaint.setStrokeCap(Paint.Cap.ROUND);
        distanceMarkPaint.setStrokeJoin(Paint.Join.ROUND);
        distanceMarkPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        distanceMarkPaint.setAlpha(128);
    }

    private void initLegendPaints() {
        distanceLegendRightPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        distanceLegendRightPaint.setTextAlign(Paint.Align.LEFT);
        distanceLegendRightPaint.setTextSize(dp(10f));
        distanceLegendRightPaint.setAlpha(128);

        distanceLegendLeftPaint.set(distanceLegendRightPaint);
        distanceLegendLeftPaint.setTextAlign(Paint.Align.RIGHT);

        destinationPaint.setStyle(Paint.Style.FILL);
        destinationPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));

        clipPaint.setStyle(Paint.Style.FILL);
        clipPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
    }

    public void setCompassState(@Nullable NavCompassState compassState) {
        this.compassState = compassState;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = dpInt(DEFAULT_SIZE_DP);
        int width = resolveSize(desired, widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(cx, cy) - dp(10f);
        float routeRadius = radius * 0.88f;
        float headingDegrees = compassState == null ? 0f : compassState.headingDegrees;

        canvas.drawCircle(cx, cy, radius, surfacePaint);
        drawDistanceRings(canvas, cx, cy, radius);
        drawOuterCompass(canvas, cx, cy, radius, headingDegrees);

        int saveCount = canvas.save();
        compassClipPath.reset();
        compassClipPath.addCircle(cx, cy, routeRadius, Path.Direction.CW);
        canvas.clipPath(compassClipPath);
        drawAccuracyOverlay(canvas, cx, cy, routeRadius);
        drawRoute(canvas, cx, cy, routeRadius, headingDegrees);
        drawStartPoint(canvas, cx, cy, routeRadius, headingDegrees);
        drawHintMarkers(canvas, cx, cy, routeRadius, headingDegrees);
        canvas.restoreToCount(saveCount);

        drawHeadingGuide(canvas, cx, cy, radius);
        drawHeadingAccuracyGuides(canvas, cx, cy, radius);
        drawCurrentPositionMarker(canvas, cx, cy, radius);
        drawDistanceLegend(canvas, cx, cy, radius);
        drawDestinationPoint(canvas, cx, cy, routeRadius, headingDegrees);
    }

    private void drawDistanceRings(@NonNull Canvas canvas, float cx, float cy, float radius) {
        for (float ringScale : DISTANCE_RING_SCALES) {
            canvas.drawCircle(cx, cy, radius * ringScale, ringPaint);
        }
    }

    private void drawOuterCompass(@NonNull Canvas canvas, float cx, float cy, float radius, float headingDegrees) {
        canvas.save();
        canvas.rotate(-headingDegrees, cx, cy);
        for (int i = 0; i < OUTER_TICK_COUNT; i++) {
            float angle = (360f / OUTER_TICK_COUNT) * i - 90f;
            double radians = Math.toRadians(angle);
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);
            float outer = radius * 0.94f;
            boolean accented = i % 3 == 1;
            if (i % 6 == 0) {
                continue;
            }
            float inner = outer - radius * 0.026f;
            Paint paint = accented ? accentTickPaint : minorTickPaint;
            canvas.drawLine(
                    cx + inner * cos,
                    cy + inner * sin,
                    cx + outer * cos,
                    cy + outer * sin,
                    paint
            );
        }

        float cardinalOrbitRadius = radius * 0.91f;
        drawCardinal(canvas, cx, cy - cardinalOrbitRadius, "N", radius);
        drawCardinal(canvas, cx + cardinalOrbitRadius, cy, "O", radius);
        drawCardinal(canvas, cx, cy + cardinalOrbitRadius, "S", radius);
        drawCardinal(canvas, cx - cardinalOrbitRadius, cy, "W", radius);
        canvas.restore();
    }

    private void drawCardinal(@NonNull Canvas canvas, float x, float y, @NonNull String label, float radius) {
        cardinalPaint.setTextSize(radius * 0.14f);
        Paint.FontMetrics fontMetrics = cardinalPaint.getFontMetrics();
        float baseline = y - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(label, x, baseline, cardinalPaint);
    }

    private void drawRoute(@NonNull Canvas canvas, float cx, float cy, float routeRadius, float headingDegrees) {
        if (compassState == null || compassState.visibleRadiusMeters <= 0f) {
            return;
        }

        routePaint.setStrokeWidth(dp(ROUTE_STROKE_WIDTH_DP));
        routePaint.setAlpha(resolveRoutePaintAlpha());
        routeThresholdPaint.setStrokeWidth(resolveRouteThresholdStrokeWidthPx(routeRadius));
        routeThresholdPaint.setAlpha(resolveRouteThresholdPaintAlpha());
        passedRoutePaint.setStrokeWidth(dp(ROUTE_STROKE_WIDTH_DP));
        float scale = routeRadius / compassState.visibleRadiusMeters;
        if (compassState.hasRouteGeometry()) {
            drawRouteThresholdGeometrySegment(
                    canvas,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    compassState.remainingRouteStartSamplePointIndex(),
                    compassState.routeSamplePointCount()
            );
            drawRouteGeometrySegment(
                    canvas,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    0,
                    compassState.passedRouteSamplePointCount(),
                    passedRoutePaint
            );
            drawRouteGeometrySegment(
                    canvas,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    compassState.remainingRouteStartSamplePointIndex(),
                    compassState.routeSamplePointCount(),
                    routePaint
            );
            return;
        }
        drawRouteSegment(canvas, cx, cy, scale, headingDegrees, compassState.passedRoutePoints, passedRoutePaint);
        drawRouteThresholdSegment(canvas, cx, cy, scale, headingDegrees, compassState.routePoints);
        drawRouteSegment(canvas, cx, cy, scale, headingDegrees, compassState.routePoints, routePaint);
    }

    private float resolveRouteThresholdStrokeWidthPx(float routeRadius) {
        return NavigationRouteThreshold.resolveStrokeWidthPx(
                compassState,
                routeRadius,
                dp(ROUTE_STROKE_WIDTH_DP)
        );
    }

    private int resolveRoutePaintAlpha() {
        return ROUTE_DEFAULT_ALPHA;
    }

    private int resolveRouteThresholdPaintAlpha() {
        return shouldDrawRouteThresholdOverlay() ? ROUTE_THRESHOLD_ALPHA : ROUTE_DEFAULT_ALPHA;
    }

    private boolean shouldDrawRouteThresholdOverlay() {
        return NavigationRouteThreshold.shouldDrawOverlay(compassState);
    }

    private void drawRouteThresholdGeometrySegment(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex
    ) {
        if (!shouldDrawRouteThresholdOverlay()) {
            return;
        }
        drawRouteGeometrySegment(
                canvas,
                cx,
                cy,
                scale,
                headingDegrees,
                startIndex,
                endIndex,
                routeThresholdPaint
        );
    }

    private void drawRouteThresholdSegment(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            @NonNull List<NavCompassState.RoutePoint> points
    ) {
        if (!shouldDrawRouteThresholdOverlay()) {
            return;
        }
        drawRouteSegment(canvas, cx, cy, scale, headingDegrees, points, routeThresholdPaint);
    }

    private void drawRouteGeometrySegment(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex,
            @NonNull Paint strokePaint
    ) {
        if (compassState == null || startIndex < 0 || endIndex <= startIndex) {
            return;
        }

        drawProjectedRouteSegment(canvas, cx, cy, scale, startIndex, endIndex, strokePaint, (i, out) -> {
            LatLon point = compassState.routeSamplePointAt(i);
            if (point == null) {
                return false;
            }
            projectRoutePoint(point, headingDegrees, out);
            return true;
        });
    }

    private void drawRouteSegment(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            @NonNull List<NavCompassState.RoutePoint> points,
            @NonNull Paint strokePaint
    ) {
        if (points.isEmpty()) {
            return;
        }

        drawProjectedRouteSegment(canvas, cx, cy, scale, 0, points.size(), strokePaint, (i, out) -> {
            NavCompassState.RoutePoint point = points.get(i);
            projectHeadingUp(point.eastMeters, point.northMeters, headingDegrees, out);
            return true;
        });
    }

    private void drawProjectedRouteSegment(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float scale,
            int startIndex,
            int endIndex,
            @NonNull Paint strokePaint,
            @NonNull ProjectedRoutePointSource pointSource
    ) {
        float visibleRadiusMeters = compassState == null ? 0f : compassState.visibleRadiusMeters;
        float drawPaddingMeters = resolveRouteDrawPaddingMeters();
        routePathRenderer.drawProjectedRouteSegment(
                canvas,
                cx,
                cy,
                scale,
                startIndex,
                endIndex,
                visibleRadiusMeters,
                drawPaddingMeters,
                strokePaint,
                pointSource::project
        );
    }

    private float resolveRouteDrawPaddingMeters() {
        if (compassState == null) {
            return 0f;
        }
        float thresholdPaddingMeters = Math.max(compassState.routeThresholdMeters, compassState.accuracyRadiusMeters);
        return Math.max(24f, thresholdPaddingMeters);
    }

    static boolean isRouteSegmentNearVisibleArea(
            float startX,
            float startY,
            float endX,
            float endY,
            float visibleRadiusMeters,
            float paddingMeters
    ) {
        return RouteDrawingMath.isRouteSegmentNearVisibleArea(
                startX,
                startY,
                endX,
                endY,
                visibleRadiusMeters,
                paddingMeters
        );
    }

    static float clampRouteCoordinate(float coordinateMeters, float drawBoundsMeters) {
        return RouteDrawingMath.clampRouteCoordinate(coordinateMeters, drawBoundsMeters);
    }

    private void drawHeadingGuide(@NonNull Canvas canvas, float cx, float cy, float radius) {
        float arrowTipY = cy - radius * HEADING_GUIDE_TOP_SCALE;
        float arrowHalfWidth = dp(HEADING_GUIDE_ARROW_WIDTH_DP) / 2f;
        float arrowBaseY = arrowTipY + dp(HEADING_GUIDE_ARROW_HEIGHT_DP);

        canvas.drawLine(cx, cy, cx, arrowTipY, headingGuidePaint);
        canvas.drawLine(cx, arrowTipY, cx - arrowHalfWidth, arrowBaseY, headingGuidePaint);
        canvas.drawLine(cx, arrowTipY, cx + arrowHalfWidth, arrowBaseY, headingGuidePaint);
    }

    private void drawHeadingAccuracyGuides(@NonNull Canvas canvas, float cx, float cy, float radius) {
        Float visibleHeadingAccuracyDegrees = resolvedVisibleHeadingAccuracyDegrees();
        if (visibleHeadingAccuracyDegrees == null) {
            return;
        }
        drawHeadingAccuracyGuideLine(canvas, cx, cy, radius, -90f - visibleHeadingAccuracyDegrees);
        drawHeadingAccuracyGuideLine(canvas, cx, cy, radius, -90f + visibleHeadingAccuracyDegrees);
    }

    private void drawHeadingAccuracyGuideLine(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float radius,
            float angleDegrees
    ) {
        double radians = Math.toRadians(angleDegrees);
        float guideRadius = radius * DISTANCE_RING_SCALES[0];
        float endX = cx + (float) Math.cos(radians) * guideRadius;
        float endY = cy + (float) Math.sin(radians) * guideRadius;
        canvas.drawLine(cx, cy, endX, endY, headingAccuracyGuidePaint);
    }

    private void drawCurrentPositionMarker(@NonNull Canvas canvas, float cx, float cy, float radius) {
        float markerDotRadius = radius * CENTER_MARKER_DOT_RADIUS_SCALE;
        canvas.drawCircle(cx, cy, markerDotRadius, centerPaint);
    }

    private void drawAccuracyOverlay(@NonNull Canvas canvas, float cx, float cy, float routeRadius) {
        if (compassState == null
                || compassState.visibleRadiusMeters <= 0f
                || compassState.accuracyRadiusMeters <= 0f) {
            return;
        }

        float overlayRadius = routeRadius * (compassState.accuracyRadiusMeters / compassState.visibleRadiusMeters);
        canvas.drawCircle(cx, cy, Math.min(routeRadius, overlayRadius), accuracyOverlayPaint);
    }

    private void drawHintMarkers(@NonNull Canvas canvas, float cx, float cy, float routeRadius, float headingDegrees) {
        if (compassState == null || compassState.visibleRadiusMeters <= 0f) {
            return;
        }

        float scale = routeRadius / compassState.visibleRadiusMeters;
        float markerRadius = dp(ROUTE_MARKER_RADIUS_DP);
        if (compassState.hasRouteGeometry()) {
            drawGeometryHintMarkers(canvas, cx, cy, scale, markerRadius, headingDegrees);
            return;
        }
        if (compassState.hintPoints.isEmpty()) {
            return;
        }
        drawLegacyHintMarkers(canvas, cx, cy, scale, markerRadius, headingDegrees);
    }

    private void drawGeometryHintMarkers(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float scale,
            float markerRadius,
            float headingDegrees
    ) {
        for (int i = 0; i < compassState.hintSamplePointCount(); i++) {
            LatLon point = compassState.hintSamplePointAt(i);
            if (point == null) {
                continue;
            }
            projectRoutePoint(point, headingDegrees, projectedPoint);
            canvas.drawCircle(cx + projectedPoint.x * scale, cy - projectedPoint.y * scale, markerRadius, routeMarkerPaint);
        }
    }

    private void drawLegacyHintMarkers(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float scale,
            float markerRadius,
            float headingDegrees
    ) {
        for (NavCompassState.RoutePoint point : compassState.hintPoints) {
            projectHeadingUp(point.eastMeters, point.northMeters, headingDegrees, projectedPoint);
            canvas.drawCircle(cx + projectedPoint.x * scale, cy - projectedPoint.y * scale, markerRadius, routeMarkerPaint);
        }
    }

    private void drawStartPoint(@NonNull Canvas canvas, float cx, float cy, float routeRadius, float headingDegrees) {
        if (compassState == null || compassState.visibleRadiusMeters <= 0f) {
            return;
        }

        NavCompassState.RoutePoint point = resolveVisibleStartPoint();
        if (point == null) {
            return;
        }
        float scale = routeRadius / compassState.visibleRadiusMeters;
        projectHeadingUp(point.eastMeters, point.northMeters, headingDegrees, projectedPoint);
        canvas.drawCircle(
                cx + projectedPoint.x * scale,
                cy - projectedPoint.y * scale,
                dp(ROUTE_MARKER_RADIUS_DP),
                routeMarkerPaint
        );
    }

    @Nullable
    private NavCompassState.RoutePoint resolveVisibleStartPoint() {
        if (compassState == null) {
            return null;
        }
        if (compassState.hasRouteGeometry()) {
            LatLon point = compassState.routeSamplePointAt(0);
            if (point == null) {
                return null;
            }
            return new NavCompassState.RoutePoint(
                    (float) GeoMath.eastMeters(
                            compassState.currentLatitude(),
                            compassState.currentLongitude(),
                            point.lat,
                            point.lon
                    ),
                    (float) GeoMath.northMeters(compassState.currentLatitude(), point.lat)
            );
        }
        if (!compassState.passedRoutePoints.isEmpty()) {
            return compassState.passedRoutePoints.get(0);
        }
        if (!compassState.routePoints.isEmpty()) {
            return compassState.routePoints.get(0);
        }
        return null;
    }

    private void drawDestinationPoint(@NonNull Canvas canvas, float cx, float cy, float routeRadius, float headingDegrees) {
        if (compassState == null
                || compassState.visibleRadiusMeters <= 0f
                || !compassState.destinationWithinRadius) {
            return;
        }

        NavigationRoutePathRenderer.PlotPoint position =
                resolveDestinationPosition(cx, cy, routeRadius, headingDegrees);
        if (position == null) {
            return;
        }
        canvas.drawCircle(position.x, position.y, dp(DESTINATION_MARKER_RADIUS_DP), destinationPaint);
    }

    private void drawDistanceLegend(@NonNull Canvas canvas, float cx, float cy, float radius) {
        if (compassState == null || compassState.visibleRadiusMeters <= 0f) {
            return;
        }

        Float visibleHeadingAccuracyDegrees = resolvedVisibleHeadingAccuracyDegrees();
        Paint.FontMetrics fontMetrics = distanceLegendRightPaint.getFontMetrics();
        float labelBaselineOffset = -(fontMetrics.ascent + fontMetrics.descent) / 2f;
        float dashHalfWidth = dp(DISTANCE_MARK_WIDTH_DP) / 2f;
        for (float ringScale : DISTANCE_RING_SCALES) {
            float y = cy - radius * ringScale;
            float ringDistanceMeters = resolveLegendRingDistanceMeters(ringScale);
            String distanceLabel = formatDistanceLabel(ringDistanceMeters);
            String secondsLabel = formatRingTimeLabel(ringDistanceMeters);
            if (visibleHeadingAccuracyDegrees != null) {
                drawHeadingAccuracyArc(canvas, cx, cy, radius * ringScale, visibleHeadingAccuracyDegrees);
                NavigationRoutePathRenderer.PlotPoint rightAnchor = resolveHeadingAccuracyRingIntersection(
                        cx,
                        cy,
                        radius * ringScale,
                        -90f + visibleHeadingAccuracyDegrees
                );
                NavigationRoutePathRenderer.PlotPoint leftAnchor = resolveHeadingAccuracyRingIntersection(
                        cx,
                        cy,
                        radius * ringScale,
                        -90f - visibleHeadingAccuracyDegrees
                );
                canvas.drawText(
                        distanceLabel,
                        rightAnchor.x + dp(DISTANCE_LABEL_OFFSET_DP),
                        rightAnchor.y + labelBaselineOffset,
                        distanceLegendRightPaint
                );
                canvas.drawText(
                        secondsLabel,
                        leftAnchor.x - dp(DISTANCE_LABEL_OFFSET_DP),
                        leftAnchor.y + labelBaselineOffset,
                        distanceLegendLeftPaint
                );
            } else {
                float labelX = cx + dashHalfWidth + dp(DISTANCE_LABEL_OFFSET_DP);
                float secondsX = cx - dashHalfWidth - dp(DISTANCE_LABEL_OFFSET_DP);
                canvas.drawLine(cx - dashHalfWidth, y, cx + dashHalfWidth, y, distanceMarkPaint);
                canvas.drawText(
                        distanceLabel,
                        labelX,
                        y + labelBaselineOffset,
                        distanceLegendRightPaint
                );
                canvas.drawText(
                        secondsLabel,
                        secondsX,
                        y + labelBaselineOffset,
                        distanceLegendLeftPaint
                );
            }
        }
    }

    @Nullable
    private Float resolvedVisibleHeadingAccuracyDegrees() {
        if (compassState == null || compassState.headingAccuracyDegrees == null) {
            return null;
        }
        float headingAccuracyDegrees = Math.min(
                HEADING_ACCURACY_GUIDE_MAX_DEGREES,
                Math.max(0f, compassState.headingAccuracyDegrees)
        );
        if (headingAccuracyDegrees <= 0f) {
            return null;
        }
        return Math.max(HEADING_ACCURACY_GUIDE_MIN_VISIBLE_DEGREES, headingAccuracyDegrees);
    }

    private void drawHeadingAccuracyArc(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float arcRadius,
            float visibleHeadingAccuracyDegrees
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
    private NavigationRoutePathRenderer.PlotPoint resolveHeadingAccuracyRingIntersection(
            float cx,
            float cy,
            float ringRadius,
            float angleDegrees
    ) {
        double radians = Math.toRadians(angleDegrees);
        auxiliaryPoint.set(
                cx + (float) Math.cos(radians) * ringRadius,
                cy + (float) Math.sin(radians) * ringRadius
        );
        return auxiliaryPoint;
    }

    @Nullable
    private NavigationRoutePathRenderer.PlotPoint resolveDestinationPosition(
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        if (compassState == null || !compassState.destinationWithinRadius) {
            return null;
        }

        float distance = (float) Math.hypot(compassState.destinationEastMeters, compassState.destinationNorthMeters);
        if (distance < 1f) {
            return null;
        }

        float scale = routeRadius / Math.max(1f, compassState.visibleRadiusMeters);
        projectHeadingUp(
                compassState.destinationEastMeters,
                compassState.destinationNorthMeters,
                headingDegrees,
                destinationPoint
        );
        destinationPoint.set(
                cx + destinationPoint.x * scale,
                cy - destinationPoint.y * scale
        );
        return destinationPoint;
    }

    @NonNull
    private String formatDistanceLabel(float distanceMeters) {
        if (distanceMeters >= 1000f) {
            return getResources().getString(R.string.format_distance_km, distanceMeters / 1000f);
        }
        return getResources().getString(R.string.format_distance_m, distanceMeters);
    }

    @NonNull
    private String formatRingTimeLabel(float distanceMeters) {
        if (compassState == null) {
            return getResources().getString(R.string.nav_status_unavailable);
        }
        int seconds = (int) Math.round(distanceMeters / Math.max(1f, compassState.referenceSpeedMps));
        return NavigationTextFormatter.formatTimeSeconds(getContext(), seconds);
    }

    private float resolveLegendRingDistanceMeters(float ringScale) {
        if (compassState == null || compassState.visibleRadiusMeters <= 0f) {
            return 0f;
        }
        return compassState.visibleRadiusMeters * (ringScale / OUTER_DISTANCE_RING_SCALE);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private int dpInt(int value) {
        return Math.round(dp((float) value));
    }

    private void projectHeadingUp(
            float eastMeters,
            float northMeters,
            float headingDegrees,
            @NonNull NavigationRoutePathRenderer.PlotPoint out
    ) {
        double radians = Math.toRadians(headingDegrees);
        float rotatedEast = (float) (eastMeters * Math.cos(radians) - northMeters * Math.sin(radians));
        float rotatedNorth = (float) (eastMeters * Math.sin(radians) + northMeters * Math.cos(radians));
        out.set(rotatedEast, rotatedNorth);
    }

    private void projectRoutePoint(
            @NonNull LatLon point,
            float headingDegrees,
            @NonNull NavigationRoutePathRenderer.PlotPoint out
    ) {
        if (compassState == null) {
            out.set(0f, 0f);
            return;
        }
        float eastMeters = (float) GeoMath.eastMeters(
                compassState.currentLatitude(),
                compassState.currentLongitude(),
                point.lat,
                point.lon
        );
        float northMeters = (float) GeoMath.northMeters(compassState.currentLatitude(), point.lat);
        projectHeadingUp(eastMeters, northMeters, headingDegrees, out);
    }

    private interface ProjectedRoutePointSource {
        boolean project(int index, @NonNull NavigationRoutePathRenderer.PlotPoint out);
    }
}
