package vibro.navigator.main;

import vibro.navigator.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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

    @NonNull
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

    public MainRouteRailView(@NonNull Context context) {
        this(context, null);
    }

    public MainRouteRailView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MainRouteRailView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        metrics = RailMetrics.create(context);
        int routeColor = ContextCompat.getColor(context, R.color.compass_route);
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
        canvas.drawLine(metrics.railX, destinationY, metrics.railX, currentMarkerEdgeY, linePaint);

        drawRouteArrow(canvas, currentMarkerEdgeY, destinationY);
        drawFilledMarker(canvas, destinationY);
        drawStopMarkers(canvas);
        drawCurrentPositionMarker(canvas, currentY);
    }

    private float currentMarkerEdgeY(float currentY, float destinationY) {
        float directionToDestination = destinationY < currentY ? -1.0f : 1.0f;
        return currentY + (directionToDestination * metrics.currentMarkerClearance);
    }

    private void drawRouteArrow(@NonNull Canvas canvas, float currentMarkerEdgeY, float destinationY) {
        float segmentLength = Math.abs(destinationY - currentMarkerEdgeY);
        if (segmentLength < metrics.minArrowSegmentLength) {
            return;
        }
        float direction = destinationY < currentMarkerEdgeY ? -1.0f : 1.0f;
        float arrowOffset = Math.min(segmentLength / 3.0f, metrics.maxArrowOffsetFromCurrent);
        drawArrow(canvas, currentMarkerEdgeY + (direction * arrowOffset), direction);
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
        private final float maxArrowOffsetFromCurrent;

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
            minArrowSegmentLength = dp(context, 42.0f);
            maxArrowOffsetFromCurrent = dp(context, 96.0f);
        }

        @NonNull
        private static RailMetrics create(@NonNull Context context) {
            return new RailMetrics(context);
        }
    }
}
