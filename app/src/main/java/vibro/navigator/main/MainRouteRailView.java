package vibro.navigator.main;

import vibro.navigator.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public final class MainRouteRailView extends View {
    private static final float STRAIGHT_LINE_DOT_LENGTH_DP = 2.0f;
    private static final float STRAIGHT_LINE_DOT_GAP_DP = 6.0f;

    @NonNull
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final Path linePath = new Path();
    @NonNull
    private final Path arrowPath = new Path();
    @NonNull
    private final Rect anchorRect = new Rect();
    @NonNull
    private final Rect selfRect = new Rect();
    @NonNull
    private final List<View> stopAnchors = new ArrayList<>();

    @Nullable
    private View destinationAnchor;
    @Nullable
    private View currentPositionAnchor;

    @NonNull
    private final RailMetrics metrics;
    @NonNull
    private final PathEffect straightLineLineEffect;
    private boolean straightLineMode;

    public MainRouteRailView(@NonNull Context context) {
        this(context, null);
    }

    public MainRouteRailView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MainRouteRailView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        metrics = RailMetrics.create(context);
        straightLineLineEffect = new DashPathEffect(
                new float[] {
                        dp(context, STRAIGHT_LINE_DOT_LENGTH_DP),
                        dp(context, STRAIGHT_LINE_DOT_GAP_DP)
                },
                0f
        );
        int routeColor = ContextCompat.getColor(context, R.color.compass_route);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(routeColor);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeWidth(metrics.lineWidth);
        markerPaint.setColor(routeColor);
        markerPaint.setStrokeWidth(metrics.lineWidth);
    }

    void setRouteAnchors(@NonNull View destinationAnchor, @NonNull View currentPositionAnchor) {
        this.destinationAnchor = destinationAnchor;
        this.currentPositionAnchor = currentPositionAnchor;
        invalidate();
    }

    void setStopAnchors(@NonNull List<View> stopAnchors) {
        this.stopAnchors.clear();
        this.stopAnchors.addAll(stopAnchors);
        invalidate();
    }

    void setStraightLineMode(boolean straightLineMode) {
        if (this.straightLineMode == straightLineMode) {
            return;
        }
        this.straightLineMode = straightLineMode;
        linePaint.setPathEffect(straightLineMode ? straightLineLineEffect : null);
        invalidate();
    }

    Paint linePaintForTest() {
        return linePaint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (destinationAnchor == null || currentPositionAnchor == null) {
            return;
        }
        Float destinationY = anchorCenterY(destinationAnchor);
        Float currentY = anchorCenterY(currentPositionAnchor);
        if (destinationY == null || currentY == null) {
            return;
        }

        float currentMarkerEdgeY = currentMarkerEdgeY(currentY, destinationY);
        drawRailLine(canvas, destinationY, currentMarkerEdgeY);

        drawRouteArrow(canvas, currentMarkerEdgeY, destinationY);
        drawFilledMarker(canvas, destinationY);
        drawStopMarkers(canvas);
        drawCurrentPositionMarker(canvas, currentY);
    }

    void drawRailLineForTest(@NonNull Canvas canvas, float startY, float stopY) {
        drawRailLine(canvas, startY, stopY);
    }

    @Nullable
    Float routeArrowCenterYForTest(float currentMarkerEdgeY, float destinationY) {
        return routeArrowCenterY(currentMarkerEdgeY, destinationY);
    }

    private void drawRailLine(@NonNull Canvas canvas, float startY, float stopY) {
        linePath.reset();
        linePath.moveTo(metrics.railX, startY);
        linePath.lineTo(metrics.railX, stopY);
        canvas.drawPath(linePath, linePaint);
    }

    private float currentMarkerEdgeY(float currentY, float destinationY) {
        float directionToDestination = directionToward(currentY, destinationY);
        return currentY + (directionToDestination * metrics.currentMarkerClearance);
    }

    private void drawRouteArrow(@NonNull Canvas canvas, float currentMarkerEdgeY, float destinationY) {
        Float arrowCenterY = routeArrowCenterY(currentMarkerEdgeY, destinationY);
        if (arrowCenterY == null) {
            return;
        }
        drawArrow(canvas, arrowCenterY, directionToward(currentMarkerEdgeY, destinationY));
    }

    @Nullable
    private Float routeArrowCenterY(float currentMarkerEdgeY, float destinationY) {
        float segmentLength = Math.abs(destinationY - currentMarkerEdgeY);
        if (segmentLength < metrics.minArrowSegmentLength) {
            return null;
        }
        float direction = directionToward(currentMarkerEdgeY, destinationY);
        return currentMarkerEdgeY + (direction * metrics.arrowOffsetFromCurrent);
    }

    private static float directionToward(float startY, float targetY) {
        return targetY < startY ? -1.0f : 1.0f;
    }

    private void drawArrow(@NonNull Canvas canvas, float centerY, float direction) {
        float tipY = centerY + (direction * metrics.arrowHeight / 2.0f);
        float baseY = centerY - (direction * metrics.arrowHeight / 2.0f);
        arrowPath.reset();
        arrowPath.moveTo(metrics.railX, tipY);
        arrowPath.lineTo(metrics.railX - metrics.arrowHalfWidth, baseY);
        arrowPath.lineTo(metrics.railX + metrics.arrowHalfWidth, baseY);
        arrowPath.close();
        markerPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowPath, markerPaint);
    }

    private void drawStopMarkers(@NonNull Canvas canvas) {
        for (View stopAnchor : stopAnchors) {
            Float stopY = anchorCenterY(stopAnchor);
            if (stopY != null) {
                drawFilledMarker(canvas, stopY);
            }
        }
    }

    private void drawFilledMarker(@NonNull Canvas canvas, float centerY) {
        markerPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(metrics.railX, centerY, metrics.markerRadius, markerPaint);
    }

    private void drawCurrentPositionMarker(@NonNull Canvas canvas, float centerY) {
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(metrics.currentMarkerStrokeWidth);
        canvas.drawCircle(metrics.railX, centerY, metrics.currentMarkerRadius, markerPaint);
        markerPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(metrics.railX, centerY, metrics.currentMarkerDotRadius, markerPaint);
        markerPaint.setStrokeWidth(metrics.lineWidth);
    }

    @Nullable
    private Float anchorCenterY(@NonNull View anchor) {
        if (anchor.getWidth() <= 0 || anchor.getHeight() <= 0 || getParent() == null) {
            return null;
        }
        ViewGroup parent = (ViewGroup) getParent();
        anchorRect.set(0, 0, anchor.getWidth(), anchor.getHeight());
        parent.offsetDescendantRectToMyCoords(anchor, anchorRect);
        selfRect.set(0, 0, getWidth(), getHeight());
        parent.offsetDescendantRectToMyCoords(this, selfRect);
        return anchorRect.exactCenterY() - selfRect.top;
    }

    private static float dp(@NonNull Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    private static final class RailMetrics {
        private final float railX;
        private final float lineWidth;
        private final float markerRadius;
        private final float currentMarkerRadius;
        private final float currentMarkerDotRadius;
        private final float currentMarkerStrokeWidth;
        private final float currentMarkerClearance;
        private final float arrowHalfWidth;
        private final float arrowHeight;
        private final float minArrowSegmentLength;
        private final float arrowOffsetFromCurrent;

        private RailMetrics(@NonNull Context context) {
            railX = dp(context, 6.0f);
            lineWidth = dp(context, 1.25f);
            markerRadius = dp(context, 4.5f);
            currentMarkerRadius = dp(context, 6.0f);
            currentMarkerDotRadius = dp(context, 2.4f);
            currentMarkerStrokeWidth = dp(context, 1.5f);
            currentMarkerClearance = currentMarkerRadius + currentMarkerStrokeWidth + lineWidth;
            arrowHalfWidth = dp(context, 4.5f);
            arrowHeight = dp(context, 9.0f);
            arrowOffsetFromCurrent = dp(context, 96.0f);
            minArrowSegmentLength = arrowOffsetFromCurrent + arrowHeight;
        }

        @NonNull
        private static RailMetrics create(@NonNull Context context) {
            return new RailMetrics(context);
        }
    }
}
