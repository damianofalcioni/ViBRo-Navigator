package vibro.navigator;


import vibro.navigator.nav.compass.NavCompassState;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;

final class NavigationCompassRouteMarkerRenderer {

    private static final float ROUTE_MARKER_RADIUS_DP = 2.5f;
    private static final float DESTINATION_MARKER_RADIUS_DP = 4f;

    private final Paint routeMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint destinationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NavigationRoutePathRenderer.PlotPoint projectedPoint = new NavigationRoutePathRenderer.PlotPoint();
    private final NavigationRoutePathRenderer.PlotPoint destinationPoint = new NavigationRoutePathRenderer.PlotPoint();
    private boolean initialized;

    void drawDestinationPoint(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        ensurePaintsInitialized(context);
        NavigationRoutePathRenderer.PlotPoint position =
                resolveDestinationPosition(state, cx, cy, routeRadius, headingDegrees);
        if (position == null) {
            return;
        }
        canvas.drawCircle(position.x, position.y, dp(context, DESTINATION_MARKER_RADIUS_DP), destinationPaint);
    }

    @Nullable
    NavigationRoutePathRenderer.PlotPoint resolveDestinationPosition(
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        if (state == null || !state.destinationWithinRadius) {
            return null;
        }

        float distance = (float) Math.hypot(state.destinationEastMeters, state.destinationNorthMeters);
        if (distance < 1f) {
            return null;
        }

        float scale = routeRadius / Math.max(1f, state.visibleRadiusMeters);
        NavigationCompassRouteProjector.projectHeadingUp(
                state.destinationEastMeters,
                state.destinationNorthMeters,
                headingDegrees,
                destinationPoint
        );
        destinationPoint.set(
                cx + destinationPoint.x * scale,
                cy - destinationPoint.y * scale
        );
        return destinationPoint;
    }

    void drawHintMarkers(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        if (state == null || state.visibleRadiusMeters <= 0f) {
            return;
        }
        ensurePaintsInitialized(context);
        float scale = routeRadius / state.visibleRadiusMeters;
        float markerRadius = dp(context, ROUTE_MARKER_RADIUS_DP);
        if (state.hasRouteGeometry()) {
            drawGeometryHintMarkers(canvas, state, cx, cy, scale, markerRadius, headingDegrees);
            return;
        }
        if (!state.hintPoints.isEmpty()) {
            drawLegacyHintMarkers(canvas, state, cx, cy, scale, markerRadius, headingDegrees);
        }
    }

    void drawStartPoint(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        if (state == null || state.visibleRadiusMeters <= 0f) {
            return;
        }
        ensurePaintsInitialized(context);
        NavCompassState.RoutePoint point = resolveVisibleStartPoint(state);
        if (point == null) {
            return;
        }
        float scale = routeRadius / state.visibleRadiusMeters;
        NavigationCompassRouteProjector.projectHeadingUp(
                point.eastMeters,
                point.northMeters,
                headingDegrees,
                projectedPoint
        );
        canvas.drawCircle(
                cx + projectedPoint.x * scale,
                cy - projectedPoint.y * scale,
                dp(context, ROUTE_MARKER_RADIUS_DP),
                routeMarkerPaint
        );
    }

    private void ensurePaintsInitialized(@NonNull Context context) {
        if (initialized) {
            return;
        }
        routeMarkerPaint.setStyle(Paint.Style.FILL);
        routeMarkerPaint.setColor(ContextCompat.getColor(context, R.color.white));
        routeMarkerPaint.setAlpha(128);

        destinationPaint.setStyle(Paint.Style.FILL);
        destinationPaint.setColor(ContextCompat.getColor(context, R.color.white));
        initialized = true;
    }

    private void drawGeometryHintMarkers(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float markerRadius,
            float headingDegrees
    ) {
        for (int i = 0; i < state.hintSamplePointCount(); i++) {
            LatLon point = state.hintSamplePointAt(i);
            if (point == null) {
                continue;
            }
            NavigationCompassRouteProjector.projectRoutePoint(state, point, headingDegrees, projectedPoint);
            canvas.drawCircle(cx + projectedPoint.x * scale, cy - projectedPoint.y * scale, markerRadius, routeMarkerPaint);
        }
    }

    private void drawLegacyHintMarkers(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float markerRadius,
            float headingDegrees
    ) {
        for (NavCompassState.RoutePoint point : state.hintPoints) {
            NavigationCompassRouteProjector.projectHeadingUp(
                    point.eastMeters,
                    point.northMeters,
                    headingDegrees,
                    projectedPoint
            );
            canvas.drawCircle(cx + projectedPoint.x * scale, cy - projectedPoint.y * scale, markerRadius, routeMarkerPaint);
        }
    }

    @Nullable
    private NavCompassState.RoutePoint resolveVisibleStartPoint(@NonNull NavCompassState state) {
        if (state.hasRouteGeometry()) {
            LatLon point = state.routeSamplePointAt(0);
            if (point == null) {
                return null;
            }
            return new NavCompassState.RoutePoint(
                    (float) GeoMath.eastMeters(
                            state.currentLatitude(),
                            state.currentLongitude(),
                            point.lat,
                            point.lon
                    ),
                    (float) GeoMath.northMeters(state.currentLatitude(), point.lat)
            );
        }
        if (!state.passedRoutePoints.isEmpty()) {
            return state.passedRoutePoints.get(0);
        }
        if (!state.routePoints.isEmpty()) {
            return state.routePoints.get(0);
        }
        return null;
    }

    private float dp(@NonNull Context context, float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }
}
