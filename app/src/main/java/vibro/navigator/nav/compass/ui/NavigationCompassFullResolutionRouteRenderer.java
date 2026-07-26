package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRoutePoint;
import vibro.navigator.nav.compass.NavCompassState;

import java.util.List;

final class NavigationCompassFullResolutionRouteRenderer {
    @NonNull
    private final Paint routePaint;
    @NonNull
    private final Paint passedRoutePaint;
    @NonNull
    private final Paint routeThresholdPaint;
    @NonNull
    private final Paint straightLinePaint;
    @NonNull
    private final Paint passedStraightLinePaint;
    @NonNull
    private final NavigationRoutePathRenderer routePathRenderer = new NavigationRoutePathRenderer();
    @NonNull
    private final NavigationCompassBeelineRouteRenderer beelineRouteRenderer =
            new NavigationCompassBeelineRouteRenderer(this);
    @NonNull
    private final NavigationCompassStoredRouteSegmentRenderer storedSegmentRenderer =
            new NavigationCompassStoredRouteSegmentRenderer();

    NavigationCompassFullResolutionRouteRenderer(
            @NonNull Paint routePaint,
            @NonNull Paint passedRoutePaint,
            @NonNull Paint routeThresholdPaint,
            @NonNull Paint straightLinePaint,
            @NonNull Paint passedStraightLinePaint
    ) {
        this.routePaint = routePaint;
        this.passedRoutePaint = passedRoutePaint;
        this.routeThresholdPaint = routeThresholdPaint;
        this.straightLinePaint = straightLinePaint;
        this.passedStraightLinePaint = passedStraightLinePaint;
    }

    void drawRoute(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            float drawPaddingMeters,
            boolean drawThreshold
    ) {
        if (drawThreshold) {
            beelineRouteRenderer.drawRemainingFullRoute(
                    canvas,
                    state,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    routeThresholdPaint,
                    null
            );
        }
        storedSegmentRenderer.draw(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                drawPaddingMeters,
                passedRoutePaint,
                straightLinePaint
        );
        beelineRouteRenderer.drawFullRouteRanges(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                0,
                state.fullRouteView.passedPointCount(),
                passedRoutePaint,
                passedStraightLinePaint
        );
        beelineRouteRenderer.drawRemainingFullRoute(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                routePaint,
                straightLinePaint
        );
    }

    void drawStraightLineRoute(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            float drawPaddingMeters
    ) {
        storedSegmentRenderer.draw(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                drawPaddingMeters,
                passedStraightLinePaint,
                passedStraightLinePaint
        );
        drawRemainingRoute(canvas, state, cx, cy, scale, headingDegrees, straightLinePaint);
    }

    void drawSampledRoute(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            float drawPaddingMeters,
            boolean drawThreshold
    ) {
        if (drawThreshold) {
            beelineRouteRenderer.drawSampledRouteRange(
                    canvas,
                    state,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    state.remainingRouteStartSamplePointIndex(),
                    state.routeSamplePointCount(),
                    routeThresholdPaint,
                    null
            );
        }
        storedSegmentRenderer.draw(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                drawPaddingMeters,
                passedRoutePaint,
                straightLinePaint
        );
        beelineRouteRenderer.drawSampledRouteRange(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                0,
                state.passedRouteSamplePointCount(),
                passedRoutePaint,
                passedStraightLinePaint
        );
        beelineRouteRenderer.drawSampledRouteRange(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                state.remainingRouteStartSamplePointIndex(),
                state.routeSamplePointCount(),
                routePaint,
                straightLinePaint
        );
    }

    void drawProjectedRoute(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            boolean drawThreshold
    ) {
        drawProjectedPoints(canvas, state, cx, cy, scale, headingDegrees, state.passedRoutePoints, passedRoutePaint);
        if (drawThreshold) {
            drawProjectedPoints(
                    canvas,
                    state,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    state.routePoints,
                    routeThresholdPaint
            );
        }
        drawProjectedPoints(canvas, state, cx, cy, scale, headingDegrees, state.routePoints, routePaint);
    }

    void drawSampledStraightLineRoute(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            float drawPaddingMeters
    ) {
        storedSegmentRenderer.draw(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                drawPaddingMeters,
                passedStraightLinePaint,
                passedStraightLinePaint
        );
        drawSampledRange(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                state.remainingRouteStartSamplePointIndex(),
                state.routeSamplePointCount(),
                straightLinePaint
        );
    }

    void drawProjectedStraightLineRoute(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees
    ) {
        drawProjectedPoints(canvas, state, cx, cy, scale, headingDegrees, state.routePoints, straightLinePaint);
    }

    private void drawRemainingRoute(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            @NonNull Paint paint
    ) {
        drawRanges(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                state.fullRouteView.remainingStartPointIndex(),
                state.fullRouteView.pointCount(),
                paint
        );
    }

    private void drawRanges(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex,
            @NonNull Paint paint
    ) {
        for (int rangeIndex = 0; rangeIndex < state.fullRouteView.rangeCount(); rangeIndex++) {
            drawRange(
                    canvas,
                    state,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    Math.max(startIndex, state.fullRouteView.rangeStartIndexAt(rangeIndex)),
                    Math.min(endIndex, state.fullRouteView.rangeEndIndexAt(rangeIndex)),
                    paint
            );
        }
    }

    void drawSampledRange(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex,
            @NonNull Paint paint
    ) {
        if (endIndex <= startIndex) {
            return;
        }
        routePathRenderer.drawProjectedRouteSegment(
                canvas,
                cx,
                cy,
                scale,
                startIndex,
                endIndex,
                state.radiusState.visibleRadiusMeters,
                resolveRouteDrawPaddingMeters(state),
                paint,
                (index, out) -> projectSampledPoint(state, index, headingDegrees, out)
        );
    }

    private void drawProjectedPoints(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            @NonNull List<CompassRoutePoint> points,
            @NonNull Paint paint
    ) {
        if (points.isEmpty()) {
            return;
        }
        routePathRenderer.drawProjectedRouteSegment(
                canvas,
                cx,
                cy,
                scale,
                0,
                points.size(),
                state.radiusState.visibleRadiusMeters,
                resolveRouteDrawPaddingMeters(state),
                paint,
                (index, out) -> projectProjectedPoint(points.get(index), headingDegrees, out)
        );
    }

    void drawRange(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex,
            @NonNull Paint paint
    ) {
        if (endIndex <= startIndex) {
            return;
        }
        routePathRenderer.drawProjectedRouteSegment(
                canvas,
                cx,
                cy,
                scale,
                startIndex,
                endIndex,
                state.radiusState.visibleRadiusMeters,
                resolveRouteDrawPaddingMeters(state),
                paint,
                (index, out) -> projectPoint(state, index, headingDegrees, out)
        );
    }

    private boolean projectPoint(
            @NonNull NavCompassState state,
            int index,
            float headingDegrees,
            @NonNull NavigationRoutePathRenderer.PlotPoint out
    ) {
        LatLon point = state.fullRouteView.pointAt(index);
        if (point == null) {
            return false;
        }
        NavigationCompassRouteProjector.projectRoutePoint(state, point, headingDegrees, out);
        return true;
    }

    private boolean projectSampledPoint(
            @NonNull NavCompassState state,
            int index,
            float headingDegrees,
            @NonNull NavigationRoutePathRenderer.PlotPoint out
    ) {
        LatLon point = state.routeSamplePointAt(index);
        if (point == null) {
            return false;
        }
        NavigationCompassRouteProjector.projectRoutePoint(state, point, headingDegrees, out);
        return true;
    }

    private boolean projectProjectedPoint(
            @NonNull CompassRoutePoint point,
            float headingDegrees,
            @NonNull NavigationRoutePathRenderer.PlotPoint out
    ) {
        NavigationCompassRouteProjector.projectHeadingUp(
                point.eastMeters,
                point.northMeters,
                headingDegrees,
                out
        );
        return true;
    }

    private static float resolveRouteDrawPaddingMeters(@NonNull NavCompassState state) {
        return Math.max(
                24f,
                Math.max(state.radiusState.routeThresholdMeters, state.radiusState.accuracyRadiusMeters)
        );
    }
}
