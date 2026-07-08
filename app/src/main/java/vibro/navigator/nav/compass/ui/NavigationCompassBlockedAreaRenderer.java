package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassBlockedArea;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassBlockedAreaRenderer {
    static final int BLOCKED_AREA_ALPHA = 96;

    private final Paint blockedAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NavigationRoutePathRenderer.PlotPoint projectedCenter =
            new NavigationRoutePathRenderer.PlotPoint();
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
        if (state == null || state.radiusState.visibleRadiusMeters <= 0f || state.blockedAreas.isEmpty()) {
            return;
        }
        ensurePaintInitialized(context);
        float scale = routeRadius / state.radiusState.visibleRadiusMeters;
        for (CompassBlockedArea area : state.blockedAreas) {
            drawArea(canvas, state, cx, cy, scale, headingDegrees, area);
        }
    }

    @NonNull
    Paint blockedAreaPaintForTest(@NonNull Context context) {
        ensurePaintInitialized(context);
        return blockedAreaPaint;
    }

    private void ensurePaintInitialized(@NonNull Context context) {
        if (initialized) {
            return;
        }
        blockedAreaPaint.setStyle(Paint.Style.FILL);
        blockedAreaPaint.setColor(ContextCompat.getColor(context, R.color.compass_blocked_area));
        blockedAreaPaint.setAlpha(BLOCKED_AREA_ALPHA);
        initialized = true;
    }

    private void drawArea(
            @NonNull Canvas canvas,
            @NonNull NavCompassState state,
            float cx,
            float cy,
            float scale,
            float headingDegrees,
            @NonNull CompassBlockedArea area
    ) {
        if (!isDrawable(area)) {
            return;
        }
        NavigationCompassRouteProjector.projectHeadingUp(
                area.eastMeters,
                area.northMeters,
                headingDegrees,
                projectedCenter
        );
        if (isOutsideVisibleRadius(state, area)) {
            return;
        }
        canvas.drawCircle(
                cx + projectedCenter.x * scale,
                cy - projectedCenter.y * scale,
                area.radiusMeters * scale,
                blockedAreaPaint
        );
    }

    private boolean isOutsideVisibleRadius(@NonNull NavCompassState state, @NonNull CompassBlockedArea area) {
        return Math.hypot(area.eastMeters, area.northMeters) - area.radiusMeters
                > state.radiusState.visibleRadiusMeters;
    }

    private boolean isDrawable(@NonNull CompassBlockedArea area) {
        return Float.isFinite(area.eastMeters)
                && Float.isFinite(area.northMeters)
                && Float.isFinite(area.radiusMeters)
                && area.radiusMeters > 0f;
    }
}
