package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassStreetRenderer {
    private static final float STREET_STROKE_WIDTH_DP = 1.2f;
    private static final int STREET_ALPHA = 180;
    private static final float DRAW_PADDING_METERS = 24f;

    @NonNull
    private final Paint streetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final NavigationRoutePathRenderer pathRenderer = new NavigationRoutePathRenderer();
    private boolean initialized;

    void draw(
            @NonNull Canvas canvas,
            @NonNull Context context,
            @Nullable NavCompassState state,
            float cx,
            float cy,
            float routeRadius,
            float headingDegrees
    ) {
        if (state == null || state.streetOverlay.isEmpty() || state.radiusState.visibleRadiusMeters <= 0f) {
            return;
        }
        ensureInitialized(context);
        float scale = routeRadius / state.radiusState.visibleRadiusMeters;
        for (CompassStreetSegment segment : state.streetOverlay.segments) {
            drawSegment(canvas, state, segment, cx, cy, scale, headingDegrees);
        }
    }

    private void ensureInitialized(@NonNull Context context) {
        if (initialized) {
            return;
        }
        streetPaint.setStyle(Paint.Style.STROKE);
        streetPaint.setStrokeWidth(dp(context, STREET_STROKE_WIDTH_DP));
        streetPaint.setStrokeJoin(Paint.Join.ROUND);
        streetPaint.setStrokeCap(Paint.Cap.ROUND);
        streetPaint.setColor(ContextCompat.getColor(context, R.color.compass_accent));
        streetPaint.setAlpha(STREET_ALPHA);
        initialized = true;
    }

    private void drawSegment(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            @NonNull CompassStreetSegment segment,
            float cx,
            float cy,
            float scale,
            float headingDegrees
    ) {
        pathRenderer.drawProjectedRouteSegment(
                canvas,
                cx,
                cy,
                scale,
                0,
                segment.points.size(),
                state.radiusState.visibleRadiusMeters,
                DRAW_PADDING_METERS,
                streetPaint,
                (index, out) -> project(state, segment.points.get(index), headingDegrees, out)
        );
    }

    private boolean project(
            @NonNull NavCompassState state,
            @NonNull LatLon point,
            float headingDegrees,
            @NonNull NavigationRoutePathRenderer.PlotPoint out
    ) {
        float eastMeters = (float) GeoMath.eastMeters(
                state.currentLatitude(),
                state.currentLongitude(),
                point.lat,
                point.lon
        );
        float northMeters = (float) GeoMath.northMeters(state.currentLatitude(), point.lat);
        NavigationCompassRouteProjector.projectHeadingUp(eastMeters, northMeters, headingDegrees, out);
        return true;
    }

    Paint paintForTest(@NonNull Context context) {
        ensureInitialized(context);
        return streetPaint;
    }

    private float dp(@NonNull Context context, float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }
}
