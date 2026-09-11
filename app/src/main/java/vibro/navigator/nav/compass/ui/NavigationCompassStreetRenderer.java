package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassStreetRenderer {
    private static final float STREET_STROKE_WIDTH_DP = 1.2f;
    private static final int STREET_ALPHA = 180;

    @NonNull
    private final Paint streetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        Path path = pathCache.pathFor(state.streetOverlay, state.currentLatitude(), state.currentLongitude(),
                state.radiusState.visibleRadiusMeters, scale);
        float boundsPixels = (state.radiusState.visibleRadiusMeters + NavigationStreetPathCache.DRAW_PADDING_METERS) * scale;
        drawPath(canvas, path, cx, cy, boundsPixels, headingDegrees);
    }

    private void ensureInitialized(@NonNull Context context) {
        if (initialized) {
            return;
        }
        streetPaint.setStyle(Paint.Style.STROKE);
        streetPaint.setStrokeWidth(dp(context, STREET_STROKE_WIDTH_DP));
        streetPaint.setStrokeJoin(Paint.Join.ROUND);
        streetPaint.setStrokeCap(Paint.Cap.ROUND);
        streetPaint.setColor(AndroidAppTheme.color(context, android.R.attr.colorControlActivated));
        streetPaint.setAlpha(STREET_ALPHA);
        initialized = true;
    }

    private void drawPath(Canvas canvas, Path path, float cx, float cy, float bounds, float heading) {
        if (path.isEmpty()) {
            return;
        }
        int saved = canvas.save();
        try {
            canvas.clipRect(cx - bounds, cy - bounds, cx + bounds, cy + bounds);
            canvas.translate(cx, cy);
            canvas.rotate(-heading);
            canvas.drawPath(path, streetPaint);
        } finally {
            canvas.restoreToCount(saved);
        }
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
