package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassBeelineRouteRenderer {
    @NonNull
    private final NavigationCompassFullResolutionRouteRenderer routeRenderer;

    NavigationCompassBeelineRouteRenderer(
            @NonNull NavigationCompassFullResolutionRouteRenderer routeRenderer
    ) {
        this.routeRenderer = routeRenderer;
    }

    void drawRemainingFullRoute(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            @Nullable Paint routeSegmentPaint,
            @Nullable Paint beelineSegmentPaint
    ) {
        drawFullRouteRanges(
                canvas,
                state,
                cx,
                cy,
                scale,
                headingDegrees,
                state.fullRouteView.remainingStartPointIndex(),
                state.fullRouteView.pointCount(),
                routeSegmentPaint,
                beelineSegmentPaint
        );
    }

    void drawFullRouteRanges(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex,
            @Nullable Paint routeSegmentPaint,
            @Nullable Paint beelineSegmentPaint
    ) {
        for (int rangeIndex = 0; rangeIndex < state.fullRouteView.rangeCount(); rangeIndex++) {
            drawFullRouteRange(
                    canvas,
                    state,
                    cx,
                    cy,
                    scale,
                    headingDegrees,
                    Math.max(startIndex, state.fullRouteView.rangeStartIndexAt(rangeIndex)),
                    Math.min(endIndex, state.fullRouteView.rangeEndIndexAt(rangeIndex)),
                    routeSegmentPaint,
                    beelineSegmentPaint
            );
        }
    }

    void drawSampledRouteRange(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex,
            @Nullable Paint routeSegmentPaint,
            @Nullable Paint beelineSegmentPaint
    ) {
        int runStartIndex = startIndex;
        while (runStartIndex + 1 < endIndex) {
            boolean beeline = isSampledBeelineSegment(state, runStartIndex);
            int runEndIndex = sampledRunEndIndex(state, runStartIndex, endIndex, beeline);
            Paint paint = beeline ? beelineSegmentPaint : routeSegmentPaint;
            if (paint != null) {
                routeRenderer.drawSampledRange(
                        canvas,
                        state,
                        cx,
                        cy,
                        scale,
                        headingDegrees,
                        runStartIndex,
                        runEndIndex,
                        paint
                );
            }
            runStartIndex = runEndIndex - 1;
        }
    }

    private void drawFullRouteRange(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            int startIndex,
            int endIndex,
            @Nullable Paint routeSegmentPaint,
            @Nullable Paint beelineSegmentPaint
    ) {
        int runStartIndex = startIndex;
        while (runStartIndex + 1 < endIndex) {
            boolean beeline = isFullRouteBeelineSegment(state, runStartIndex);
            int runEndIndex = fullRouteRunEndIndex(state, runStartIndex, endIndex, beeline);
            Paint paint = beeline ? beelineSegmentPaint : routeSegmentPaint;
            if (paint != null) {
                routeRenderer.drawRange(
                        canvas,
                        state,
                        cx,
                        cy,
                        scale,
                        headingDegrees,
                        runStartIndex,
                        runEndIndex,
                        paint
                );
            }
            runStartIndex = runEndIndex - 1;
        }
    }

    private static int sampledRunEndIndex(
            @NonNull NavCompassState state,
            int startIndex,
            int endIndex,
            boolean beeline
    ) {
        int segmentIndex = startIndex + 1;
        while (segmentIndex + 1 < endIndex
                && isSampledBeelineSegment(state, segmentIndex) == beeline) {
            segmentIndex++;
        }
        return segmentIndex + 1;
    }

    private static int fullRouteRunEndIndex(
            @NonNull NavCompassState state,
            int startIndex,
            int endIndex,
            boolean beeline
    ) {
        int segmentIndex = startIndex + 1;
        while (segmentIndex + 1 < endIndex
                && isFullRouteBeelineSegment(state, segmentIndex) == beeline) {
            segmentIndex++;
        }
        return segmentIndex + 1;
    }

    private static boolean isSampledBeelineSegment(
            @NonNull NavCompassState state,
            int startPointIndex
    ) {
        CompassRouteGeometry geometry = state.routeGeometry();
        return geometry != null && geometry.beelineSegments().isSampledSegment(startPointIndex);
    }

    private static boolean isFullRouteBeelineSegment(
            @NonNull NavCompassState state,
            int startPointIndex
    ) {
        CompassRouteGeometry geometry = state.routeGeometry();
        return geometry != null && geometry.beelineSegments().isFullRouteSegment(startPointIndex);
    }
}
