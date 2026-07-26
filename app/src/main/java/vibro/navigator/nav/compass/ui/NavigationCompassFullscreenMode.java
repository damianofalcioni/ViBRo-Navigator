package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;

final class NavigationCompassFullscreenMode {
    private boolean enabled;
    private float centerYHint = Float.NaN;

    void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            enabled = false;
            return;
        }
        TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.NavigationCompassView);
        try {
            enabled = values.getBoolean(R.styleable.NavigationCompassView_vibroFullscreenRouteMode, false);
        } finally {
            values.recycle();
        }
    }

    boolean isEnabled() {
        return enabled;
    }

    @NonNull
    Measurement resolveMeasurement(int width, int height) {
        if (enabled) {
            return new Measurement(width, height);
        }
        int size = Math.min(width, height);
        return new Measurement(size, size);
    }

    float resolveCompassRadius(float cx, float cy, float compactInset) {
        return Math.max(0f, Math.min(cx, cy) - compactInset);
    }

    float resolveRouteRadius(float cy, float topInset) {
        return Math.max(0f, cy - topInset);
    }

    float resolveCenterY(float height, float bottomInset) {
        return resolveCenterY(height, bottomInset, centerYHint);
    }

    float resolveCenterY(float height, float bottomInset, float centerYHint) {
        if (isPositiveFinite(centerYHint)) {
            return Math.max(height / 2f, Math.min(height, centerYHint));
        }
        return Math.max(height / 2f, height - bottomInset);
    }

    boolean setCenterYHint(float centerY) {
        float sanitized = isPositiveFinite(centerY) ? centerY : Float.NaN;
        if (sameCenterYHint(sanitized)) {
            return false;
        }
        centerYHint = sanitized;
        return true;
    }

    float resolveLegendOuterScale(float routeRadius, float compassRadius) {
        if (compassRadius <= 0f) {
            return 1f;
        }
        return routeRadius / compassRadius;
    }

    float resolveHeadingGuideRadius(boolean portraitOrientation, float routeRadius, float compassRadius) {
        return Math.max(0f, portraitOrientation ? compassRadius : routeRadius);
    }

    private boolean sameCenterYHint(float value) {
        if (Float.isNaN(centerYHint) && Float.isNaN(value)) {
            return true;
        }
        return Math.abs(centerYHint - value) <= 0.5f;
    }

    private static boolean isPositiveFinite(float value) {
        return value > 0f && !Float.isNaN(value) && !Float.isInfinite(value);
    }

    static final class Measurement {
        final int width;
        final int height;

        Measurement(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
