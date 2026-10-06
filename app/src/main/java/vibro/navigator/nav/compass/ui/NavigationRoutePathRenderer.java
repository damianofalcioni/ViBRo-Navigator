package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.NonNull;

final class NavigationRoutePathRenderer {

    // A north-up square must cover the heading-up drawing bounds at every rotation.
    private static final float ROTATION_BOUNDS_SCALE = (float) Math.sqrt(2d);

    private final Path routePath = new Path();
    private final PlotPoint routeSegmentStartPoint = new PlotPoint();
    private final PlotPoint routeSegmentEndPoint = new PlotPoint();
    private final PlotPoint previousPathEndPoint = new PlotPoint();
    private final RouteDrawingMath.ClippedSegment clippedSegment = new RouteDrawingMath.ClippedSegment();
    private final NavigationRoutePathCache pathCache = new NavigationRoutePathCache();

    void drawProjectedRouteSegment(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float scale,
            int startIndex,
            int endIndex,
            float visibleRadiusMeters,
            float drawPaddingMeters,
            @NonNull Object sourceIdentity,
            int sourceSlot,
            float headingDegrees,
            @NonNull Paint strokePaint,
            @NonNull ProjectedRoutePointSource pointSource
    ) {
        NavigationRoutePathCache.Entry cached = pathCache.find(
                sourceIdentity, sourceSlot, startIndex, endIndex,
                cx, cy, scale, visibleRadiusMeters, drawPaddingMeters
        );
        if (cached == null) {
            buildNorthUpPath(cx, cy, scale, startIndex, endIndex,
                    visibleRadiusMeters, drawPaddingMeters, pointSource);
            cached = pathCache.remember(sourceIdentity, sourceSlot, startIndex, endIndex,
                    routePath, !routePath.isEmpty());
        }
        drawRotatedPath(canvas, cached, cx, cy, scale,
                visibleRadiusMeters + drawPaddingMeters, headingDegrees, strokePaint);
    }

    private void buildNorthUpPath(
            float cx, float cy, float scale, int startIndex, int endIndex,
            float visibleRadiusMeters, float drawPaddingMeters,
            @NonNull ProjectedRoutePointSource pointSource
    ) {
        routePath.reset();
        boolean havePrevious = false;
        boolean activeSubpath = false;
        float drawBoundsMeters = (visibleRadiusMeters + drawPaddingMeters) * ROTATION_BOUNDS_SCALE;
        for (int i = startIndex; i < endIndex; i++) {
            if (!pointSource.project(i, routeSegmentEndPoint)) {
                continue;
            }
            if (!havePrevious) {
                routeSegmentStartPoint.set(routeSegmentEndPoint.x, routeSegmentEndPoint.y);
                havePrevious = true;
                continue;
            }
            boolean appended = appendVisibleSegmentIfNearVisibleArea(
                    cx,
                    cy,
                    scale,
                    drawBoundsMeters,
                    visibleRadiusMeters,
                    drawPaddingMeters,
                    activeSubpath
            );
            activeSubpath = appended;
            routeSegmentStartPoint.set(routeSegmentEndPoint.x, routeSegmentEndPoint.y);
        }
    }

    private static void drawRotatedPath(
            Canvas canvas, NavigationRoutePathCache.Entry entry,
            float cx, float cy, float scale, float boundsMeters, float headingDegrees, Paint paint
    ) {
        int saved = canvas.save();
        try {
            float boundsPixels = boundsMeters * scale;
            canvas.clipRect(cx - boundsPixels, cy - boundsPixels, cx + boundsPixels, cy + boundsPixels);
            canvas.rotate(-headingDegrees, cx, cy);
            entry.draw(canvas, paint);
        } finally {
            canvas.restoreToCount(saved);
        }
    }

    private boolean appendVisibleSegmentIfNearVisibleArea(
            float cx,
            float cy,
            float scale,
            float drawBoundsMeters,
            float visibleRadiusMeters,
            float drawPaddingMeters,
            boolean activeSubpath
    ) {
        if (!RouteDrawingMath.isRouteSegmentNearVisibleArea(
                routeSegmentStartPoint.x,
                routeSegmentStartPoint.y,
                routeSegmentEndPoint.x,
                routeSegmentEndPoint.y,
                visibleRadiusMeters,
                drawPaddingMeters
        )) {
            return false;
        }
        return appendVisibleSegment(cx, cy, scale, drawBoundsMeters, activeSubpath);
    }

    private boolean appendVisibleSegment(
            float cx,
            float cy,
            float scale,
            float drawBoundsMeters,
            boolean activeSubpath
    ) {
        if (!RouteDrawingMath.clipSegmentToBounds(
                routeSegmentStartPoint.x,
                routeSegmentStartPoint.y,
                routeSegmentEndPoint.x,
                routeSegmentEndPoint.y,
                drawBoundsMeters,
                clippedSegment
        )) {
            return false;
        }

        float startX = cx + clippedSegment.startX * scale;
        float startY = cy - clippedSegment.startY * scale;
        float endX = cx + clippedSegment.endX * scale;
        float endY = cy - clippedSegment.endY * scale;
        if (!activeSubpath || !samePoint(startX, startY, previousPathEndPoint.x, previousPathEndPoint.y)) {
            routePath.moveTo(startX, startY);
        }
        routePath.lineTo(endX, endY);
        previousPathEndPoint.set(endX, endY);
        return true;
    }

    private boolean samePoint(float firstX, float firstY, float secondX, float secondY) {
        return Math.abs(firstX - secondX) <= 0.01f
                && Math.abs(firstY - secondY) <= 0.01f;
    }

    static final class PlotPoint {
        float x;
        float y;

        void set(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    /** Supplies east/north meters independently of the display heading. */
    interface ProjectedRoutePointSource {
        boolean project(int index, @NonNull PlotPoint out);
    }
}
