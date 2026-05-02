package vibro.navigator;


import vibro.navigator.nav.compass.NavCompassState;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;


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
    private static final float OUTER_DISTANCE_RING_SCALE = DISTANCE_RING_SCALES[0];

    @Nullable
    private NavCompassState compassState;

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
    private final NavigationCompassLegendRenderer legendRenderer = new NavigationCompassLegendRenderer();

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

    private void initMarkerPaints() {
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_center));
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

        drawHeadingGuide(canvas, cx, cy, radius);
        drawHeadingAccuracyGuides(canvas, cx, cy, radius);
        drawCurrentPositionMarker(canvas, cx, cy, radius);
        drawDistanceLegend(canvas, cx, cy, radius);
        routeRenderer.drawDestinationPoint(canvas, getContext(), compassState, cx, cy, routeRadius, headingDegrees);
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

    private void drawDistanceLegend(@NonNull Canvas canvas, float cx, float cy, float radius) {
        legendRenderer.draw(
                canvas,
                getContext(),
                compassState,
                cx,
                cy,
                radius,
                DISTANCE_RING_SCALES,
                OUTER_DISTANCE_RING_SCALE,
                dp(DISTANCE_MARK_WIDTH_DP),
                dp(DISTANCE_LABEL_OFFSET_DP),
                distanceMarkPaint,
                distanceLegendRightPaint,
                distanceLegendLeftPaint,
                headingAccuracyGuidePaint
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

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private int dpInt(int value) {
        return Math.round(dp((float) value));
    }
}
