package com.vibenavigator;

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

import com.vibenavigator.nav.NavCompassState;

public final class NavigationCompassView extends View {

    private static final int DEFAULT_SIZE_DP = 280;
    private static final int OUTER_TICK_COUNT = 24;

    @Nullable
    private NavCompassState compassState;

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardinalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routeGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint finishPolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint finishFlagLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint finishFlagDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path compassClipPath = new Path();
    private final Path arrowPath = new Path();
    private final Path routePath = new Path();

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

        surfacePaint.setStyle(Paint.Style.FILL);
        surfacePaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_surface));

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(2f));
        ringPaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_ring));

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

        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeWidth(dp(3f));
        routePaint.setStrokeJoin(Paint.Join.ROUND);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_route));

        routeGlowPaint.setStyle(Paint.Style.STROKE);
        routeGlowPaint.setStrokeWidth(dp(6f));
        routeGlowPaint.setStrokeJoin(Paint.Join.ROUND);
        routeGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        routeGlowPaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_route_glow));

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(ContextCompat.getColor(getContext(), R.color.compass_center));

        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));

        finishPolePaint.setStyle(Paint.Style.STROKE);
        finishPolePaint.setStrokeCap(Paint.Cap.ROUND);
        finishPolePaint.setStrokeWidth(dp(2f));
        finishPolePaint.setColor(ContextCompat.getColor(getContext(), R.color.black));

        finishFlagLightPaint.setStyle(Paint.Style.FILL);
        finishFlagLightPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));

        finishFlagDarkPaint.setStyle(Paint.Style.FILL);
        finishFlagDarkPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));

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
        drawRoute(canvas, cx, cy, routeRadius, headingDegrees);
        canvas.restoreToCount(saveCount);

        canvas.drawCircle(cx, cy, radius * 0.16f, ringPaint);
        canvas.drawCircle(cx, cy, radius * 0.05f, centerPaint);
        drawHeadingArrow(canvas, cx, cy, radius * 0.22f);
        drawDestinationMarker(canvas, cx, cy, routeRadius, headingDegrees);
    }

    private void drawDistanceRings(@NonNull Canvas canvas, float cx, float cy, float radius) {
        canvas.drawCircle(cx, cy, radius * 0.82f, ringPaint);
        canvas.drawCircle(cx, cy, radius * 0.55f, ringPaint);
        canvas.drawCircle(cx, cy, radius * 0.28f, ringPaint);
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
            boolean major = i % 6 == 0;
            if (major) {
                continue;
            }
            float inner = outer - (major ? radius * 0.048f : radius * 0.026f);
            Paint paint = accented ? accentTickPaint : (major ? majorTickPaint : minorTickPaint);
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
        if (compassState == null || compassState.routePoints.isEmpty() || compassState.visibleRadiusMeters <= 0f) {
            return;
        }

        float scale = routeRadius / compassState.visibleRadiusMeters;
        routePath.reset();
        boolean started = false;
        for (NavCompassState.RoutePoint point : compassState.routePoints) {
            float[] projected = projectHeadingUp(point.eastMeters, point.northMeters, headingDegrees);
            float x = cx + projected[0] * scale;
            float y = cy - projected[1] * scale;
            if (!started) {
                routePath.moveTo(x, y);
                started = true;
            } else {
                routePath.lineTo(x, y);
            }
        }
        if (started) {
            canvas.drawPath(routePath, routeGlowPaint);
            canvas.drawPath(routePath, routePaint);
        }
    }

    private void drawHeadingArrow(@NonNull Canvas canvas, float cx, float cy, float arrowSize) {
        arrowPath.reset();
        arrowPath.moveTo(cx, cy - arrowSize);
        arrowPath.lineTo(cx + arrowSize * 0.34f, cy + arrowSize * 0.58f);
        arrowPath.lineTo(cx, cy + arrowSize * 0.30f);
        arrowPath.lineTo(cx - arrowSize * 0.34f, cy + arrowSize * 0.58f);
        arrowPath.close();
        canvas.drawPath(arrowPath, arrowPaint);
    }

    private void drawDestinationMarker(@NonNull Canvas canvas, float cx, float cy, float routeRadius, float headingDegrees) {
        if (compassState == null) {
            return;
        }
        float distance = (float) Math.hypot(compassState.destinationEastMeters, compassState.destinationNorthMeters);
        if (distance < 1f) {
            return;
        }

        float scale = routeRadius / Math.max(1f, compassState.visibleRadiusMeters);
        float[] projected = projectHeadingUp(
                compassState.destinationEastMeters,
                compassState.destinationNorthMeters,
                headingDegrees
        );
        float x = cx + projected[0] * scale;
        float y = cy - projected[1] * scale;
        float iconSize = dp(compassState.destinationWithinRadius ? 18f : 20f);

        if (!compassState.destinationWithinRadius) {
            float dx = x - cx;
            float dy = y - cy;
            float norm = (float) Math.hypot(dx, dy);
            if (norm > 0f) {
                float clamped = routeRadius - iconSize - dp(6f);
                x = cx + dx / norm * clamped;
                y = cy + dy / norm * clamped;
            }
        }

        float poleX = x - iconSize * 0.18f;
        canvas.drawLine(poleX, y + iconSize * 0.42f, poleX, y - iconSize * 0.48f, finishPolePaint);
        drawFinishFlag(canvas, x - iconSize * 0.10f, y - iconSize * 0.48f, iconSize * 0.82f, iconSize * 0.72f);
    }

    private void drawFinishFlag(@NonNull Canvas canvas, float left, float top, float width, float height) {
        float cellWidth = width / 2f;
        float cellHeight = height / 2f;
        canvas.drawRect(left, top, left + cellWidth, top + cellHeight, finishFlagLightPaint);
        canvas.drawRect(left + cellWidth, top, left + width, top + cellHeight, finishFlagDarkPaint);
        canvas.drawRect(left, top + cellHeight, left + cellWidth, top + height, finishFlagDarkPaint);
        canvas.drawRect(left + cellWidth, top + cellHeight, left + width, top + height, finishFlagLightPaint);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private int dpInt(int value) {
        return Math.round(dp((float) value));
    }

    @NonNull
    private float[] projectHeadingUp(float eastMeters, float northMeters, float headingDegrees) {
        double radians = Math.toRadians(headingDegrees);
        float rotatedEast = (float) (eastMeters * Math.cos(radians) - northMeters * Math.sin(radians));
        float rotatedNorth = (float) (eastMeters * Math.sin(radians) + northMeters * Math.cos(radians));
        return new float[]{rotatedEast, rotatedNorth};
    }
}
