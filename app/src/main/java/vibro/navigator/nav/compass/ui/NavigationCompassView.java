package vibro.navigator.nav.compass.ui;


import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
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
    private static final float OUTER_DISTANCE_RING_SCALE = 0.91f;
    private static final float[] DISTANCE_RING_SCALES = new float[]{OUTER_DISTANCE_RING_SCALE, 0.61f, 0.30f};
    private static final float[] FARTHEST_DISTANCE_RING_SCALE = new float[]{OUTER_DISTANCE_RING_SCALE};
    private static final float CENTER_MARKER_DOT_RADIUS_SCALE = 0.02f;
    private static final float HEADING_GUIDE_ARROW_WIDTH_DP = NavigationCompassOrientationCueRenderer.MARKER_WIDTH_DP;
    private static final float HEADING_GUIDE_ARROW_HEIGHT_DP = NavigationCompassOrientationCueRenderer.MARKER_HEIGHT_DP;
    private static final float HEADING_ACCURACY_GUIDE_MIN_VISIBLE_DEGREES = 5f;
    private static final float HEADING_ACCURACY_GUIDE_MAX_DEGREES = 85f;
    static final float OUTER_COMPASS_LAYER_INNER_SCALE = OUTER_DISTANCE_RING_SCALE;
    private static final float OUTER_COMPASS_LAYER_RADIUS_SCALE = (OUTER_COMPASS_LAYER_INNER_SCALE + 1f) / 2f;
    static final float HEADING_GUIDE_ARROW_TIP_SCALE = 1f;
    static final float OUTER_COMPASS_LAYER_STROKE_SCALE = 1f - OUTER_DISTANCE_RING_SCALE;
    static final float CARDINAL_TEXT_SIZE_SCALE = NavigationCompassOuterRingRenderer.CARDINAL_TEXT_SIZE_SCALE;
    private static final float DISTANCE_MARK_WIDTH_DP = 6f;
    private static final float DISTANCE_LABEL_OFFSET_DP = 6f;
    private static final float FULLSCREEN_ROUTE_TOP_INSET_DP = 16f;
    private static final float FULLSCREEN_CENTER_BOTTOM_INSET_DP = 88f;
    private static final float FULLSCREEN_ORIENTATION_CUE_RADIUS_SCALE = 0.5f;

    @Nullable
    private NavCompassState compassState;
    private boolean navigationPaused;
    private float perspectiveProgress;

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pausedRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardinalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NavigationCompassOuterRingRenderer outerRingRenderer =
            new NavigationCompassOuterRingRenderer(minorTickPaint, accentTickPaint, cardinalPaint);
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
    private final PerspectiveRenderer perspectiveRenderer = new PerspectiveRenderer();

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

    public void setPerspectiveProgress(float progress) {
        float bounded = Float.isFinite(progress) ? Math.max(0f, Math.min(1f, progress)) : 0f;
        if (perspectiveProgress != bounded) {
            perspectiveProgress = bounded;
            invalidate();
        }
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
            if (perspectiveProgress > 0f) {
                perspectiveRenderer.drawFullscreen(canvas, width, height, headingDegrees);
            } else {
                drawFullscreenCompass(canvas, width, height, headingDegrees);
            }
            return;
        }

        if (perspectiveProgress > 0f) {
            perspectiveRenderer.drawCompact(canvas, cx, cy, headingDegrees);
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
        outerRingRenderer.draw(canvas, cx, cy, radius, outerCompassLayerRadius(radius), headingDegrees);

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
        drawDistanceLegend(canvas, cx, cy, radius, DISTANCE_RING_SCALES,
                OUTER_DISTANCE_RING_SCALE, true,
                NavigationCompassLegendRenderer.visibleRadiusMeters(compassState));
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
        boolean portraitOrientation =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        float headingGuideRadius = fullscreenMode.resolveHeadingGuideRadius(
                portraitOrientation,
                routeRadius,
                markerRadius
        );
        float legendOuterScale = fullscreenMode.resolveLegendOuterScale(routeRadius, headingGuideRadius);
        float cueRadius = markerRadius * FULLSCREEN_ORIENTATION_CUE_RADIUS_SCALE;

        routeRenderer.drawRouteLayer(canvas, getContext(), compassState, cx, cy, routeRadius, headingDegrees);
        routeRenderer.drawDestinationPoint(canvas, getContext(), compassState, cx, cy, routeRadius, headingDegrees);
        drawHeadingGuide(canvas, cx, cy, headingGuideRadius);
        drawDistanceLegend(
                canvas,
                cx,
                cy,
                headingGuideRadius,
                fullscreenMode.resolveLegendRingScales(
                        portraitOrientation,
                        DISTANCE_RING_SCALES,
                        FARTHEST_DISTANCE_RING_SCALE
                ),
                legendOuterScale,
                false,
                NavigationCompassLegendRenderer.visibleRadiusMeters(compassState)
        );
        drawCurrentPositionMarker(canvas, cx, cy, markerRadius);
        orientationCueRenderer.draw(canvas, getContext(), compassState, cx, cy, cueRadius, headingDegrees);
    }

    private final class PerspectiveRenderer {
        private final NavigationCompassPerspective perspective = new NavigationCompassPerspective();

        void drawCompact(@NonNull Canvas canvas, float cx, float cy, float headingDegrees) {
            float radius = fullscreenMode.resolveCompassRadius(cx, cy, dp(10f));
            float routeRadius = radius * OUTER_COMPASS_LAYER_INNER_SCALE;
            float sourceScale = CompassPerspectiveScale.maximumViewportMultiplier();
            float visibleScale = CompassPerspectiveScale.viewportMultiplier(perspectiveProgress);
            float sourceRouteRadius = routeRadius * sourceScale;
            canvas.drawCircle(cx, cy, radius, surfacePaint);
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
            outerRingRenderer.draw(canvas, cx, cy, radius, outerCompassLayerRadius(radius), headingDegrees);
            if (perspective.configure(cx, cy, sourceRouteRadius, perspectiveProgress)) {
                int saveCount = canvas.save();
                compassClipPath.reset();
                compassClipPath.addCircle(cx, cy, routeRadius, Path.Direction.CW);
                canvas.clipPath(compassClipPath);
                drawPerspectivePlane(canvas, cx, cy, sourceRouteRadius,
                        radius * visibleScale, headingDegrees);
                canvas.restoreToCount(saveCount);
                orientationCueRenderer.draw(canvas, getContext(), compassState, cx, cy, radius, headingDegrees);
                drawProjectedHeadingGuides(canvas, cx, cy, radius * visibleScale, routeRadius);
                legendRenderer.drawPerspective(
                        canvas,
                        getContext(),
                        compassState,
                        perspective,
                        cx,
                        cy,
                        radius * visibleScale,
                        perspectiveVisibleRadiusMeters(visibleScale),
                        DISTANCE_RING_SCALES,
                        OUTER_DISTANCE_RING_SCALE,
                        dp(DISTANCE_LABEL_OFFSET_DP),
                        distanceLegendRightPaint,
                        distanceLegendLeftPaint
                );
            }
            drawCurrentPositionMarker(canvas, cx, cy, radius);
        }

        void drawFullscreen(
                @NonNull Canvas canvas,
                float width,
                float height,
                float headingDegrees
        ) {
            float cx = width / 2f;
            float cy = fullscreenMode.resolveCenterY(height, dp(FULLSCREEN_CENTER_BOTTOM_INSET_DP));
            float markerRadius = fullscreenMode.resolveCompassRadius(cx, cy, dp(10f));
            float routeRadius = fullscreenMode.resolveRouteRadius(cy, dp(FULLSCREEN_ROUTE_TOP_INSET_DP));
            float sourceScale = CompassPerspectiveScale.maximumViewportMultiplier();
            float visibleScale = CompassPerspectiveScale.viewportMultiplier(perspectiveProgress);
            float sourceRouteRadius = routeRadius * sourceScale;
            if (!perspective.configure(cx, cy, sourceRouteRadius, perspectiveProgress)) {
                return;
            }
            drawPerspectivePlane(canvas, cx, cy, sourceRouteRadius, 0f, headingDegrees);
            boolean portraitOrientation =
                    getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            float headingGuideRadius = fullscreenMode.resolveHeadingGuideRadius(
                    portraitOrientation,
                    routeRadius,
                    markerRadius
            );
            drawProjectedHeadingGuides(canvas, cx, cy, headingGuideRadius * visibleScale, 0f);
            drawDistanceLegend(
                    canvas,
                    cx,
                    cy,
                    headingGuideRadius,
                    FARTHEST_DISTANCE_RING_SCALE,
                    fullscreenMode.resolveLegendOuterScale(routeRadius, headingGuideRadius),
                    false,
                    perspectiveVisibleRadiusMeters(visibleScale)
            );
            drawCurrentPositionMarker(canvas, cx, cy, markerRadius);
            orientationCueRenderer.draw(
                    canvas,
                    getContext(),
                    compassState,
                    cx,
                    cy,
                    markerRadius * FULLSCREEN_ORIENTATION_CUE_RADIUS_SCALE,
                    headingDegrees
            );
        }

        private void drawPerspectivePlane(
                @NonNull Canvas canvas,
                float cx,
                float cy,
                float routeRadius,
                float ringRadius,
                float headingDegrees
        ) {
            int saveCount = canvas.save();
            perspective.concat(canvas);
            if (ringRadius > 0f) {
                drawDistanceRings(canvas, cx, cy, ringRadius);
            }
            routeRenderer.drawRouteLayer(canvas, getContext(), compassState, cx, cy, routeRadius, headingDegrees);
            routeRenderer.drawDestinationPoint(canvas, getContext(), compassState, cx, cy, routeRadius, headingDegrees);
            canvas.restoreToCount(saveCount);
        }

        private float perspectiveVisibleRadiusMeters(float visibleScale) {
            return NavigationCompassLegendRenderer.visibleRadiusMeters(compassState)
                    * visibleScale / CompassPerspectiveScale.maximumViewportMultiplier();
        }

        private void drawProjectedHeadingGuides(
                @NonNull Canvas canvas,
                float cx,
                float cy,
                float sourceRadius,
                float accuracyClipRadius
        ) {
            int saveCount = canvas.save();
            perspective.concat(canvas);
            drawHeadingGuide(canvas, cx, cy, sourceRadius);
            canvas.restoreToCount(saveCount);
            if (accuracyClipRadius > 0f) {
                saveCount = canvas.save();
                compassClipPath.reset();
                compassClipPath.addCircle(cx, cy, accuracyClipRadius, Path.Direction.CW);
                canvas.clipPath(compassClipPath);
                perspective.concat(canvas);
                drawHeadingAccuracyGuides(canvas, cx, cy, sourceRadius);
                canvas.restoreToCount(saveCount);
            }
        }
    }

    private void drawDistanceRings(@NonNull Canvas canvas, float cx, float cy, float radius) {
        for (float ringScale : DISTANCE_RING_SCALES) {
            canvas.drawCircle(cx, cy, radius * ringScale, ringPaint);
        }
    }

    float outerCompassLayerRadius(float radius) {
        return radius * OUTER_COMPASS_LAYER_RADIUS_SCALE;
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
            @NonNull float[] ringScales,
            float outerDistanceRingScale,
            boolean showHeadingAccuracy,
            float visibleRadiusMeters
    ) {
        legendRenderer.draw(
                canvas,
                getContext(),
                compassState,
                visibleRadiusMeters,
                cx,
                cy,
                radius,
                ringScales,
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
