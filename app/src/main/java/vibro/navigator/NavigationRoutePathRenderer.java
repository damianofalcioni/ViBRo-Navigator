package vibro.navigator;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.NonNull;

final class NavigationRoutePathRenderer {

    private final Path routePath = new Path();
    private final PlotPoint routeSegmentStartPoint = new PlotPoint();
    private final PlotPoint routeSegmentEndPoint = new PlotPoint();

    void drawProjectedRouteSegment(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float scale,
            int startIndex,
            int endIndex,
            float visibleRadiusMeters,
            float drawPaddingMeters,
            @NonNull Paint strokePaint,
            @NonNull ProjectedRoutePointSource pointSource
    ) {
        routePath.reset();
        boolean havePrevious = false;
        boolean activeSubpath = false;
        boolean hasVisibleSegment = false;
        float drawBoundsMeters = visibleRadiusMeters + drawPaddingMeters;
        for (int i = startIndex; i < endIndex; i++) {
            if (!pointSource.project(i, routeSegmentEndPoint)) {
                continue;
            }
            if (!havePrevious) {
                routeSegmentStartPoint.set(routeSegmentEndPoint.x, routeSegmentEndPoint.y);
                havePrevious = true;
                continue;
            }
            if (RouteDrawingMath.isRouteSegmentNearVisibleArea(
                    routeSegmentStartPoint.x,
                    routeSegmentStartPoint.y,
                    routeSegmentEndPoint.x,
                    routeSegmentEndPoint.y,
                    visibleRadiusMeters,
                    drawPaddingMeters
            )) {
                activeSubpath = appendVisibleSegment(cx, cy, scale, drawBoundsMeters, activeSubpath);
                hasVisibleSegment = true;
            } else {
                activeSubpath = false;
            }
            routeSegmentStartPoint.set(routeSegmentEndPoint.x, routeSegmentEndPoint.y);
        }
        if (hasVisibleSegment) {
            canvas.drawPath(routePath, strokePaint);
        }
    }

    private boolean appendVisibleSegment(
            float cx,
            float cy,
            float scale,
            float drawBoundsMeters,
            boolean activeSubpath
    ) {
        float startX = cx + RouteDrawingMath.clampRouteCoordinate(routeSegmentStartPoint.x, drawBoundsMeters) * scale;
        float startY = cy - RouteDrawingMath.clampRouteCoordinate(routeSegmentStartPoint.y, drawBoundsMeters) * scale;
        float endX = cx + RouteDrawingMath.clampRouteCoordinate(routeSegmentEndPoint.x, drawBoundsMeters) * scale;
        float endY = cy - RouteDrawingMath.clampRouteCoordinate(routeSegmentEndPoint.y, drawBoundsMeters) * scale;
        if (!activeSubpath) {
            routePath.moveTo(startX, startY);
        } else {
            routePath.lineTo(startX, startY);
        }
        routePath.lineTo(endX, endY);
        return true;
    }

    static final class PlotPoint {
        float x;
        float y;

        void set(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    interface ProjectedRoutePointSource {
        boolean project(int index, @NonNull PlotPoint out);
    }
}
