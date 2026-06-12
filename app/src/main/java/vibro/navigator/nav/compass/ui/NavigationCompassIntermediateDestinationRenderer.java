package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassIntermediateDestinationRenderer {
    private final NavigationRoutePathRenderer.PlotPoint projectedPoint =
            new NavigationRoutePathRenderer.PlotPoint();

    void draw(
            @NonNull Canvas canvas,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees,
            float markerRadius,
            float reachedRadiusPx,
            @NonNull Paint destinationPaint,
            @NonNull Paint reachedRadiusPaint
    ) {
        if (state == null || state.radiusState.visibleRadiusMeters <= 0f) {
            return;
        }
        CompassRouteGeometry geometry = state.routeGeometry();
        if (geometry == null) {
            return;
        }
        drawGeometryPoints(
                canvas,
                state,
                geometry,
                cx,
                cy,
                routeRadius,
                headingDegrees,
                markerRadius,
                reachedRadiusPx,
                destinationPaint,
                reachedRadiusPaint
        );
    }

    private void drawGeometryPoints(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            @NonNull CompassRouteGeometry geometry,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees,
            float markerRadius,
            float reachedRadiusPx,
            @NonNull Paint destinationPaint,
            @NonNull Paint reachedRadiusPaint
    ) {
        for (int i = 0; i < geometry.intermediateSamplePointCount(); i++) {
            LatLon point = geometry.intermediateSamplePointAt(i);
            if (point == null) {
                continue;
            }
            if (!isPointWithinVisibleRadius(state, point)) {
                continue;
            }
            projectRoutePoint(state, point, cx, cy, routeRadius, headingDegrees);
            if (reachedRadiusPx > 0f) {
                canvas.drawCircle(
                        projectedPoint.x,
                        projectedPoint.y,
                        Math.min(routeRadius, reachedRadiusPx),
                        reachedRadiusPaint
                );
            }
            canvas.drawCircle(projectedPoint.x, projectedPoint.y, markerRadius, destinationPaint);
        }
    }

    private void projectRoutePoint(
            @NonNull NavCompassState state,
            @NonNull LatLon point,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        float scale = routeRadius / state.radiusState.visibleRadiusMeters;
        NavigationCompassRouteProjector.projectRoutePoint(state, point, headingDegrees, projectedPoint);
        projectedPoint.set(
                cx + projectedPoint.x * scale,
                cy - projectedPoint.y * scale
        );
    }

    private boolean isPointWithinVisibleRadius(
            @NonNull NavCompassState state,
            @NonNull LatLon point
    ) {
        float eastMeters = projectedEastMeters(state, point);
        float northMeters = projectedNorthMeters(state, point);
        return Math.hypot(eastMeters, northMeters) <= state.radiusState.visibleRadiusMeters;
    }

    private static float projectedEastMeters(
            @NonNull NavCompassState state,
            @NonNull LatLon point
    ) {
        return (float) GeoMath.eastMeters(
                state.currentLatitude(),
                state.currentLongitude(),
                point.lat,
                point.lon
        );
    }

    private static float projectedNorthMeters(
            @NonNull NavCompassState state,
            @NonNull LatLon point
    ) {
        return (float) GeoMath.northMeters(state.currentLatitude(), point.lat);
    }
}
