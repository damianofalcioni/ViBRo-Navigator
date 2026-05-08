package vibro.navigator.nav.compass.ui;


import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassRoutePoint;
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
    private static final int DESTINATION_REACHED_RADIUS_ALPHA = 51;

    private final Paint routeMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint destinationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint destinationReachedRadiusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NavigationRoutePathRenderer.PlotPoint projectedPoint = new NavigationRoutePathRenderer.PlotPoint();
    private final NavigationRoutePathRenderer.PlotPoint destinationPoint = new NavigationRoutePathRenderer.PlotPoint();
    private final NavigationCompassIntermediateDestinationRenderer intermediateDestinationRenderer =
            new NavigationCompassIntermediateDestinationRenderer();
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
        intermediateDestinationRenderer.draw(
                canvas,
                state,
                cx,
                cy,
                routeRadius,
                headingDegrees,
                dp(context, DESTINATION_MARKER_RADIUS_DP),
                resolveDestinationReachedRadiusPx(state, routeRadius),
                destinationPaint,
                destinationReachedRadiusPaint
        );
        drawDestinationReachedRadius(canvas, state, cx, cy, routeRadius, headingDegrees);
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
        if (state == null || !state.progressLabels.destinationWithinRadius) {
            return null;
        }

        float distance = (float) Math.hypot(
                state.progressLabels.destinationEastMeters,
                state.progressLabels.destinationNorthMeters
        );
        if (distance < 1f) {
            return null;
        }

        float scale = routeRadius / Math.max(1f, state.radiusState.visibleRadiusMeters);
        NavigationCompassRouteProjector.projectHeadingUp(
                state.progressLabels.destinationEastMeters,
                state.progressLabels.destinationNorthMeters,
                headingDegrees,
                destinationPoint
        );
        destinationPoint.set(
                cx + destinationPoint.x * scale,
                cy - destinationPoint.y * scale
        );
        return destinationPoint;
    }

    float resolveDestinationReachedRadiusPx(@Nullable NavCompassState state, float routeRadius) {
        if (state == null
                || state.radiusState.visibleRadiusMeters <= 0f
                || state.progressLabels.destinationReachedRadiusMeters <= 0f) {
            return 0f;
        }
        return routeRadius
                * (state.progressLabels.destinationReachedRadiusMeters / state.radiusState.visibleRadiusMeters);
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
        if (state == null || state.radiusState.visibleRadiusMeters <= 0f) {
            return;
        }
        ensurePaintsInitialized(context);
        float scale = routeRadius / state.radiusState.visibleRadiusMeters;
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
        if (state == null || state.radiusState.visibleRadiusMeters <= 0f) {
            return;
        }
        ensurePaintsInitialized(context);
        CompassRoutePoint point = resolveVisibleStartPoint(state);
        if (point == null) {
            return;
        }
        float scale = routeRadius / state.radiusState.visibleRadiusMeters;
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

        destinationReachedRadiusPaint.setStyle(Paint.Style.FILL);
        destinationReachedRadiusPaint.setColor(ContextCompat.getColor(context, R.color.compass_route));
        destinationReachedRadiusPaint.setAlpha(DESTINATION_REACHED_RADIUS_ALPHA);
        initialized = true;
    }

    private void drawDestinationReachedRadius(
            @NonNull Canvas canvas,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        NavigationRoutePathRenderer.PlotPoint position = resolveDestinationCenterPosition(
                state,
                cx,
                cy,
                routeRadius,
                headingDegrees
        );
        float radiusPx = resolveDestinationReachedRadiusPx(state, routeRadius);
        if (position == null || radiusPx <= 0f) {
            return;
        }
        canvas.drawCircle(position.x, position.y, Math.min(routeRadius, radiusPx), destinationReachedRadiusPaint);
    }

    @Nullable
    private NavigationRoutePathRenderer.PlotPoint resolveDestinationCenterPosition(
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        if (state == null || !state.progressLabels.destinationWithinRadius) {
            return null;
        }
        float scale = routeRadius / Math.max(1f, state.radiusState.visibleRadiusMeters);
        NavigationCompassRouteProjector.projectHeadingUp(
                state.progressLabels.destinationEastMeters,
                state.progressLabels.destinationNorthMeters,
                headingDegrees,
                destinationPoint
        );
        destinationPoint.set(
                cx + destinationPoint.x * scale,
                cy - destinationPoint.y * scale
        );
        return destinationPoint;
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
        for (CompassRoutePoint point : state.hintPoints) {
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
    private CompassRoutePoint resolveVisibleStartPoint(@NonNull NavCompassState state) {
        if (state.hasRouteGeometry()) {
            LatLon point = state.routeSamplePointAt(0);
            if (point == null) {
                return null;
            }
            return new CompassRoutePoint(
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
