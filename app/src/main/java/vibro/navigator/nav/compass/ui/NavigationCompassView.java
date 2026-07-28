package vibro.navigator.nav.compass.ui;


import vibro.navigator.R;
import vibro.navigator.nav.compass.NavCompassState;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.android.theme.AndroidAppTheme;

public final class NavigationCompassView extends View {

    private static final int DEFAULT_SIZE_DP = 280;
    private static final int OUTER_TICK_COUNT = 24;
    private static final float OUTER_DISTANCE_RING_SCALE = 0.91f;
    private static final float[] DISTANCE_RING_SCALES = new float[]{OUTER_DISTANCE_RING_SCALE, 0.61f, 0.30f};
    private static final float CENTER_MARKER_DOT_RADIUS_SCALE = 0.02f;
    private static final float HEADING_GUIDE_ARROW_WIDTH_DP = NavigationCompassOrientationCueRenderer.MARKER_WIDTH_DP;
    private static final float HEADING_GUIDE_ARROW_HEIGHT_DP = NavigationCompassOrientationCueRenderer.MARKER_HEIGHT_DP;
    private static final float HEADING_ACCURACY_GUIDE_MIN_VISIBLE_DEGREES = 5f;
    private static final float HEADING_ACCURACY_GUIDE_MAX_DEGREES = 85f;
    static final float OUTER_COMPASS_LAYER_INNER_SCALE = OUTER_DISTANCE_RING_SCALE;
    private static final float OUTER_COMPASS_LAYER_OUTER_SCALE = 0.97f;
    private static final float OUTER_COMPASS_LAYER_RADIUS_SCALE = (OUTER_COMPASS_LAYER_INNER_SCALE + 1f) / 2f;
    static final float HEADING_GUIDE_ARROW_TIP_SCALE = 1f;
    private static final float OUTER_COMPASS_TICK_LENGTH_SCALE = 0.018f;
    static final float OUTER_COMPASS_LAYER_STROKE_SCALE = 1f - OUTER_DISTANCE_RING_SCALE;
    static final float CARDINAL_TEXT_SIZE_SCALE = 0.09f;
    private static final float DISTANCE_MARK_WIDTH_DP = 6f;
    private static final float DISTANCE_LABEL_OFFSET_DP = 6f;
    private static final float FULLSCREEN_ROUTE_TOP_INSET_DP = 16f;
    private static final float FULLSCREEN_CENTER_BOTTOM_INSET_DP = 88f;
    private static final float FULLSCREEN_ORIENTATION_CUE_RADIUS_SCALE = 0.5f;

    @Nullable
    private NavCompassState compassState;
    private boolean navigationPaused;

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pausedRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardinalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headingGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headingAccuracyGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint distanceMarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint distanceLegendRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint distanceLegendLeftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path compassClipPath = new Path();
    private final NavigationCompassRouteRenderer routeRenderer = new NavigationCompassRouteRenderer();
    private final NavigationCompassOrientationCueRenderer orientationCueRenderer =
            new NavigationCompassOrientationCueRenderer();
    private final NavigationCompassLegendRenderer legendRenderer = new NavigationCompassLegendRenderer();
    private final NavigationCompassCalibrationRing calibrationRing = new NavigationCompassCalibrationRing(this);
    private final NavigationCompassFullscreenMode fullscreenMode = new NavigationCompassFullscreenMode();

    public NavigationCompassView(Context context) {
        super(context);
        init(null);
    }

    public NavigationCompassView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public NavigationCompassView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        setWillNotDraw(false);
        fullscreenMode.init(getContext(), attrs);

        initBasePaints();
        initTickPaints();
        initMarkerPaints();
        initGuidePaints();
        initLegendPaints();
    }

    private void initBasePaints() {
        surfacePaint.setStyle(Paint.Style.FILL);
        surfacePaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassSurfaceColor));

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(2f));
        ringPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassRingColor));

        pausedRingPaint.setStyle(Paint.Style.STROKE);
        pausedRingPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassPausedRingColor));
        pausedRingPaint.setAlpha(NavigationCompassCalibrationRing.BACKGROUND_ALPHA);

        calibrationRing.init();
    }

    private void initTickPaints() {
        majorTickPaint.setStyle(Paint.Style.STROKE);
        majorTickPaint.setStrokeWidth(dp(2.4f));
        majorTickPaint.setStrokeCap(Paint.Cap.ROUND);
        majorTickPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));

        minorTickPaint.setStyle(Paint.Style.STROKE);
        minorTickPaint.setStrokeWidth(dp(1.8f));
        minorTickPaint.setStrokeCap(Paint.Cap.ROUND);
        minorTickPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));
        minorTickPaint.setAlpha(220);

        accentTickPaint.setStyle(Paint.Style.STROKE);
        accentTickPaint.setStrokeWidth(dp(2.4f));
        accentTickPaint.setStrokeCap(Paint.Cap.ROUND);
        accentTickPaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_accent));

        cardinalPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));
        cardinalPaint.setTextAlign(Paint.Align.CENTER);
        cardinalPaint.setFakeBoldText(false);
    }

    private void initMarkerPaints() {
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassCenterColor));
    }

    private void initGuidePaints() {
        headingGuidePaint.setStyle(Paint.Style.STROKE);
        headingGuidePaint.setStrokeWidth(dp(1.2f));
        headingGuidePaint.setStrokeCap(Paint.Cap.ROUND);
        headingGuidePaint.setStrokeJoin(Paint.Join.ROUND);
        headingGuidePaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));
        headingGuidePaint.setAlpha(128);

        headingAccuracyGuidePaint.setStyle(Paint.Style.STROKE);
        headingAccuracyGuidePaint.setStrokeWidth(dp(1.2f));
        headingAccuracyGuidePaint.setStrokeCap(Paint.Cap.ROUND);
        headingAccuracyGuidePaint.setStrokeJoin(Paint.Join.ROUND);
        headingAccuracyGuidePaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));
        headingAccuracyGuidePaint.setAlpha(128);

        distanceMarkPaint.setStyle(Paint.Style.STROKE);
        distanceMarkPaint.setStrokeWidth(dp(1.2f));
        distanceMarkPaint.setStrokeCap(Paint.Cap.ROUND);
        distanceMarkPaint.setStrokeJoin(Paint.Join.ROUND);
        distanceMarkPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));
        distanceMarkPaint.setAlpha(128);
    }

    private void initLegendPaints() {
        distanceLegendRightPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));
        distanceLegendRightPaint.setTextAlign(Paint.Align.LEFT);
        distanceLegendRightPaint.setTextSize(dp(10f));
        distanceLegendRightPaint.setAlpha(128);

        distanceLegendLeftPaint.set(distanceLegendRightPaint);
        distanceLegendLeftPaint.setTextAlign(Paint.Align.RIGHT);
    }

    public void setCompassState(@Nullable NavCompassState compassState) {
        this.compassState = compassState;
        calibrationRing.update(compassState);
        invalidate();
    }

    public void setNavigationPaused(boolean navigationPaused) {
        if (this.navigationPaused == navigationPaused) {
            return;
        }
        this.navigationPaused = navigationPaused;
        invalidate();
    }

    public void setFullscreenRouteModeEnabled(boolean enabled) {
        fullscreenMode.setEnabled(enabled);
        requestLayout();
        invalidate();
    }

    boolean isNavigationPausedForTest() {
        return navigationPaused;
    }

    public void setFullscreenCenterYHint(float centerY) {
        if (fullscreenMode.setCenterYHint(centerY)) {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = dpInt(DEFAULT_SIZE_DP);
        int width = resolveSize(desired, widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        NavigationCompassFullscreenMode.Measurement measurement = fullscreenMode.resolveMeasurement(width, height);
        setMeasuredDimension(measurement.width, measurement.height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float headingDegrees = compassState == null ? 0f : compassState.displayMode.headingDegrees;

        if (fullscreenMode.isEnabled()) {
            drawFullscreenCompass(canvas, width, height, headingDegrees);
            return;
        }

        float radius = fullscreenMode.resolveCompassRadius(cx, cy, dp(10f));
        float routeRadius = radius * OUTER_COMPASS_LAYER_INNER_SCALE;
        canvas.drawCircle(cx, cy, radius, surfacePaint);
        drawDistanceRings(canvas, cx, cy, radius);
        calibrationRing.draw(
                canvas,
                getContext(),
                cx,
                cy,
                outerCompassLayerRadius(radius),
                radius * OUTER_COMPASS_LAYER_STROKE_SCALE
        );
        NavigationCompassPausedRingRenderer.draw(
                canvas,
                navigationPaused,
                cx,
                cy,
                radius,
                outerCompassLayerRadius(radius),
                OUTER_COMPASS_LAYER_STROKE_SCALE,
                pausedRingPaint
        );
        drawOuterCompass(canvas, cx, cy, radius, headingDegrees);

        int saveCount = canvas.save();
        compassClipPath.reset();
        compassClipPath.addCircle(cx, cy, routeRadius, Path.Direction.CW);
        canvas.clipPath(compassClipPath);
        routeRenderer.drawRouteLayer(
                canvas,
                getContext(),
                compassState,
                cx,
                cy,
                routeRadius,
                headingDegrees
        );
        canvas.restoreToCount(saveCount);

        orientationCueRenderer.draw(canvas, getContext(), compassState, cx, cy, radius, headingDegrees);
        drawHeadingGuide(canvas, cx, cy, radius);
        drawHeadingAccuracyGuides(canvas, cx, cy, radius);
        drawCurrentPositionMarker(canvas, cx, cy, radius);
        drawDistanceLegend(canvas, cx, cy, radius, OUTER_DISTANCE_RING_SCALE, true);
        routeRenderer.drawDestinationPoint(canvas, getContext(), compassState, cx, cy, routeRadius, headingDegrees);
    }

    private void drawFullscreenCompass(
            @NonNull Canvas canvas,
            float width,
            float height,
            float headingDegrees
    ) {
        float cx = width / 2f;
        float cy = fullscreenMode.resolveCenterY(height, dp(FULLSCREEN_CENTER_BOTTOM_INSET_DP));
        float markerRadius = fullscreenMode.resolveCompassRadius(cx, cy, dp(10f));
        float routeRadius = fullscreenMode.resolveRouteRadius(cy, dp(FULLSCREEN_ROUTE_TOP_INSET_DP));
        float headingGuideRadius = fullscreenMode.resolveHeadingGuideRadius(
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT,
                routeRadius,
                markerRadius
        );
        float legendOuterScale = fullscreenMode.resolveLegendOuterScale(routeRadius, headingGuideRadius);
        float cueRadius = markerRadius * FULLSCREEN_ORIENTATION_CUE_RADIUS_SCALE;

        routeRenderer.drawRouteLayer(canvas, getContext(), compassState, cx, cy, routeRadius, headingDegrees);
        routeRenderer.drawDestinationPoint(canvas, getContext(), compassState, cx, cy, routeRadius, headingDegrees);
        drawHeadingGuide(canvas, cx, cy, headingGuideRadius);
        drawDistanceLegend(canvas, cx, cy, headingGuideRadius, legendOuterScale, false);
        drawCurrentPositionMarker(canvas, cx, cy, markerRadius);
        orientationCueRenderer.draw(canvas, getContext(), compassState, cx, cy, cueRadius, headingDegrees);
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
            float outer = radius * OUTER_COMPASS_LAYER_OUTER_SCALE;
            boolean accented = i % 3 == 1;
            if (i % 6 == 0) {
                continue;
            }
            float inner = outer - radius * OUTER_COMPASS_TICK_LENGTH_SCALE;
            Paint paint = accented ? accentTickPaint : minorTickPaint;
            canvas.drawLine(
                    cx + inner * cos,
                    cy + inner * sin,
                    cx + outer * cos,
                    cy + outer * sin,
                    paint
            );
        }

        float cardinalOrbitRadius = outerCompassLayerRadius(radius);
        drawCardinal(canvas, cx, cy - cardinalOrbitRadius, "N", radius);
        drawCardinal(canvas, cx + cardinalOrbitRadius, cy, "O", radius);
        drawCardinal(canvas, cx, cy + cardinalOrbitRadius, "S", radius);
        drawCardinal(canvas, cx - cardinalOrbitRadius, cy, "W", radius);
        canvas.restore();
    }

    float outerCompassLayerRadius(float radius) {
        return radius * OUTER_COMPASS_LAYER_RADIUS_SCALE;
    }

    private void drawCardinal(@NonNull Canvas canvas, float x, float y, @NonNull String label, float radius) {
        cardinalPaint.setTextSize(radius * CARDINAL_TEXT_SIZE_SCALE);
        Paint.FontMetrics fontMetrics = cardinalPaint.getFontMetrics();
        float baseline = y - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(label, x, baseline, cardinalPaint);
    }

    private void drawHeadingGuide(@NonNull Canvas canvas, float cx, float cy, float radius) {
        float arrowTipY = cy - radius * HEADING_GUIDE_ARROW_TIP_SCALE;
        float arrowHalfWidth = dp(HEADING_GUIDE_ARROW_WIDTH_DP) / 2f;
        float arrowBaseRadius = Math.max(
                radius * OUTER_COMPASS_LAYER_INNER_SCALE,
                radius - dp(HEADING_GUIDE_ARROW_HEIGHT_DP)
        );
        float arrowBaseY = cy - arrowBaseRadius;

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

    private void drawDistanceLegend(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float radius,
            float outerDistanceRingScale,
            boolean showHeadingAccuracy
    ) {
        legendRenderer.draw(
                canvas,
                getContext(),
                compassState,
                cx,
                cy,
                radius,
                DISTANCE_RING_SCALES,
                outerDistanceRingScale,
                dp(DISTANCE_MARK_WIDTH_DP),
                dp(DISTANCE_LABEL_OFFSET_DP),
                distanceMarkPaint,
                distanceLegendRightPaint,
                distanceLegendLeftPaint,
                headingAccuracyGuidePaint,
                showHeadingAccuracy
        );
    }

    @Nullable
    private Float resolvedVisibleHeadingAccuracyDegrees() {
        return NavigationCompassLegendRenderer.resolvedVisibleHeadingAccuracyDegrees(
                compassState,
                HEADING_ACCURACY_GUIDE_MIN_VISIBLE_DEGREES,
                HEADING_ACCURACY_GUIDE_MAX_DEGREES
        );
    }

    @Override
    protected void onDetachedFromWindow() {
        calibrationRing.detach();
        super.onDetachedFromWindow();
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private int dpInt(int value) {
        return Math.round(dp((float) value));
    }
}
