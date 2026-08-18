package vibro.navigator.auto;

import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;

final class ViBRoAutoRenderScale {
    private static final float BASELINE_LANDSCAPE_CONTENT_HEIGHT_DP = 400f;
    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 2.75f;

    private ViBRoAutoRenderScale() {
    }

    static float fromContentHeight(@NonNull CarContext carContext, float heightPx) {
        float baselinePx = dp(carContext, BASELINE_LANDSCAPE_CONTENT_HEIGHT_DP);
        if (baselinePx <= 0f) {
            return MIN_SCALE;
        }
        return clamp(heightPx / baselinePx, MIN_SCALE, MAX_SCALE);
    }

    static float dp(@NonNull CarContext carContext, float value, float scale) {
        return dp(carContext, value) * scale;
    }

    static float sp(@NonNull CarContext carContext, float value, float scale) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                carContext.getResources().getDisplayMetrics()
        ) * scale;
    }

    private static float dp(@NonNull CarContext carContext, float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                carContext.getResources().getDisplayMetrics()
        );
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
