package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassStreetRenderer {
    private static final float STREET_STROKE_WIDTH_DP = 1.2f;

    @NonNull
    private final Paint highwayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final Paint walkingCyclingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final Paint specialRoutingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final NavigationStreetGeometryCache geometryCache = new NavigationStreetGeometryCache();
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
        if (state == null
                || !state.displayMode.movingScaleActive
                || state.streetOverlay.isEmpty()
                || state.radiusState.visibleRadiusMeters <= 0f) {
            geometryCache.clear();
            return;
        }
        if (!LatLon.isValidCoordinate(state.currentLatitude(), state.currentLongitude())) {
            geometryCache.clear();
            return;
        }
        ensureInitialized(context);
        float scale = routeRadius / state.radiusState.visibleRadiusMeters;
        NavigationStreetBatches batches = geometryCache.batchesFor(
                state.streetOverlay,
                state.currentLatitude(),
                state.currentLongitude(),
                state.radiusState.visibleRadiusMeters,
                scale
        );
        float boundsPixels = (
                state.radiusState.visibleRadiusMeters + NavigationStreetGeometryCache.DRAW_PADDING_METERS
        ) * scale;
        drawBatches(canvas, batches, cx, cy, boundsPixels, headingDegrees);
    }

    private void ensureInitialized(@NonNull Context context) {
        if (initialized) {
            return;
        }
        initializePaint(context, highwayPaint, R.attr.vibroCompassStreetHighwayColor);
        initializePaint(context, normalPaint, R.attr.vibroCompassStreetNormalColor);
        initializePaint(context, walkingCyclingPaint, R.attr.vibroCompassStreetWalkingCyclingColor);
        initializePaint(context, specialRoutingPaint, R.attr.vibroCompassStreetSpecialRoutingColor);
        initialized = true;
    }

    private void initializePaint(@NonNull Context context, @NonNull Paint paint, int colorAttr) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(context, STREET_STROKE_WIDTH_DP));
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int color = AndroidAppTheme.color(context, colorAttr);
        paint.setColor(color);
        paint.setAlpha(Color.alpha(color));
    }

    private void drawBatches(
            Canvas canvas,
            NavigationStreetBatches batches,
            float cx,
            float cy,
            float bounds,
            float heading
    ) {
        int saved = canvas.save();
        try {
            canvas.clipRect(cx - bounds, cy - bounds, cx + bounds, cy + bounds);
            canvas.translate(cx, cy);
            canvas.rotate(-heading);
            batches.draw(canvas, CompassStreetCategory.SPECIAL_ROUTING, specialRoutingPaint);
            batches.draw(canvas, CompassStreetCategory.WALKING_CYCLING, walkingCyclingPaint);
            batches.draw(canvas, CompassStreetCategory.NORMAL, normalPaint);
            batches.draw(canvas, CompassStreetCategory.HIGHWAY, highwayPaint);
        } finally {
            canvas.restoreToCount(saved);
        }
    }

    Paint paintForTest(@NonNull Context context, @NonNull CompassStreetCategory category) {
        ensureInitialized(context);
        switch (category) {
            case HIGHWAY:
                return highwayPaint;
            case NORMAL:
                return normalPaint;
            case WALKING_CYCLING:
                return walkingCyclingPaint;
            case SPECIAL_ROUTING:
                return specialRoutingPaint;
            default:
                throw new IllegalArgumentException("Unknown surrounding street category: " + category);
        }
    }

    private float dp(@NonNull Context context, float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }
}
