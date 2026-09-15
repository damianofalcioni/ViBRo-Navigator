package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    private final NavigationStreetPathCache pathCache = new NavigationStreetPathCache();
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
            pathCache.clear();
            return;
        }
        if (!LatLon.isValidCoordinate(state.currentLatitude(), state.currentLongitude())) {
            pathCache.clear();
            return;
        }
        ensureInitialized(context);
        float scale = routeRadius / state.radiusState.visibleRadiusMeters;
        NavigationStreetPaths paths = pathCache.pathsFor(
                state.streetOverlay,
                state.currentLatitude(),
                state.currentLongitude(),
                state.radiusState.visibleRadiusMeters,
                scale
        );
        float boundsPixels = (
                state.radiusState.visibleRadiusMeters + NavigationStreetPathCache.DRAW_PADDING_METERS
        ) * scale;
        drawPaths(canvas, paths, cx, cy, boundsPixels, headingDegrees);
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

    private void drawPaths(
            Canvas canvas,
            NavigationStreetPaths paths,
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
            drawPath(canvas, paths.pathFor(CompassStreetCategory.SPECIAL_ROUTING), specialRoutingPaint);
            drawPath(canvas, paths.pathFor(CompassStreetCategory.WALKING_CYCLING), walkingCyclingPaint);
            drawPath(canvas, paths.pathFor(CompassStreetCategory.NORMAL), normalPaint);
            drawPath(canvas, paths.pathFor(CompassStreetCategory.HIGHWAY), highwayPaint);
        } finally {
            canvas.restoreToCount(saved);
        }
    }

    private static void drawPath(Canvas canvas, Path path, Paint paint) {
        if (!path.isEmpty()) {
            canvas.drawPath(path, paint);
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
