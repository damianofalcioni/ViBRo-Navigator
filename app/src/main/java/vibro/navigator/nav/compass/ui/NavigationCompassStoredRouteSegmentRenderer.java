package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassPassedRouteSegments;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassStoredRouteSegmentRenderer {
    private final NavigationRoutePathRenderer routePathRenderer = new NavigationRoutePathRenderer();

    void draw(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            float drawPaddingMeters,
            @NonNull Paint passedRoutePaint,
            @NonNull Paint bridgePaint
    ) {
        drawSegments(
                canvas,
                state,
                state.archivedPassedRouteSegments(),
                cx,
                cy,
                scale,
                headingDegrees,
                drawPaddingMeters,
                passedRoutePaint
        );
        CompassRouteGeometry geometry = state.routeGeometry();
        if (geometry != null) {
            drawSegments(
                    canvas,
                    state,
                    geometry.recalculationBridgeSegments(),
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    drawPaddingMeters,
                    bridgePaint
            );
        }
    }

    private void drawSegments(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            @NonNull CompassPassedRouteSegments segments,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            float drawPaddingMeters,
            @NonNull Paint strokePaint
    ) {
        for (int segmentIndex = 0; segmentIndex < segments.segmentCount(); segmentIndex++) {
            drawSegment(
                    canvas,
                    state,
                    segments,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    drawPaddingMeters,
                    strokePaint,
                    segmentIndex
            );
        }
    }

    private void drawSegment(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            @NonNull CompassPassedRouteSegments segments,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            float drawPaddingMeters,
            @NonNull Paint strokePaint,
            int segmentIndex
    ) {
        int pointCount = segments.samplePointCount(segmentIndex);
        if (pointCount < 2) {
            return;
        }
        routePathRenderer.drawProjectedRouteSegment(
                canvas,
                cx,
                cy,
                scale,
                0,
                pointCount,
                state.radiusState.visibleRadiusMeters,
                drawPaddingMeters,
                strokePaint,
                (i, out) -> projectSegmentPoint(state, segments, segmentIndex, i, headingDegrees, out)
        );
    }

    private boolean projectSegmentPoint(
            @NonNull NavCompassState state,
            @NonNull CompassPassedRouteSegments segments,
            int segmentIndex,
            int pointIndex,
            float headingDegrees,
            @NonNull NavigationRoutePathRenderer.PlotPoint out
    ) {
        LatLon point = segments.samplePointAt(segmentIndex, pointIndex);
        if (point == null) {
            return false;
        }
        NavigationCompassRouteProjector.projectRoutePoint(state, point, headingDegrees, out);
        return true;
    }
}
