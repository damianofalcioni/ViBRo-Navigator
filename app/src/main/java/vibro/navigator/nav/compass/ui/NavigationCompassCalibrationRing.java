package vibro.navigator.nav.compass.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.nav.compass.NavCompassState;

final class NavigationCompassCalibrationRing {
    private static final float HEADING_CALIBRATION_OK_MAX_DEGREES = 25f;
    private static final long CALIBRATION_OK_RING_VISIBLE_MS = 2_000L;

    @NonNull
    private final View owner;
    @NonNull
    private final Paint calibrationRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final Runnable hideCalibrationOkRingRunnable = new Runnable() {
        @Override
        public void run() {
            hideCalibrationOkRing();
        }
    };

    @Nullable
    private Boolean lastHeadingCalibrationNeeded;
    private boolean visible;
    private boolean calibrationNeeded;

    NavigationCompassCalibrationRing(@NonNull View owner) {
        this.owner = owner;
    }

    void init(float strokeWidthPx) {
        calibrationRingPaint.setStyle(Paint.Style.STROKE);
        calibrationRingPaint.setStrokeWidth(strokeWidthPx);
        calibrationRingPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    void update(@Nullable NavCompassState compassState) {
        if (compassState == null) {
            clear();
            return;
        }
        if (needsHeadingCalibration(compassState)) {
            showCalibrationNeeded();
            return;
        }
        showCalibrationOkIfNeeded();
    }

    void draw(
            @NonNull Canvas canvas,
            @NonNull Context context,
            float cx,
            float cy,
            float radius,
            float radiusOffsetPx
    ) {
        if (!visible) {
            return;
        }
        calibrationRingPaint.setColor(ContextCompat.getColor(
                context,
                calibrationNeeded ? R.color.danger : R.color.success
        ));
        canvas.drawCircle(cx, cy, radius + radiusOffsetPx, calibrationRingPaint);
    }

    void detach() {
        owner.removeCallbacks(hideCalibrationOkRingRunnable);
    }

    static boolean needsHeadingCalibration(@Nullable NavCompassState compassState) {
        if (compassState == null) {
            return false;
        }
        Float headingAccuracyDegrees = compassState.displayMode.headingAccuracyDegrees;
        return headingAccuracyDegrees != null
                && (!Float.isFinite(headingAccuracyDegrees)
                || headingAccuracyDegrees > HEADING_CALIBRATION_OK_MAX_DEGREES);
    }

    boolean isVisibleForTest() {
        return visible;
    }

    boolean isCalibrationNeededForTest() {
        return calibrationNeeded;
    }

    private void clear() {
        owner.removeCallbacks(hideCalibrationOkRingRunnable);
        lastHeadingCalibrationNeeded = null;
        visible = false;
        calibrationNeeded = false;
    }

    private void showCalibrationNeeded() {
        owner.removeCallbacks(hideCalibrationOkRingRunnable);
        lastHeadingCalibrationNeeded = true;
        visible = true;
        calibrationNeeded = true;
    }

    private void showCalibrationOkIfNeeded() {
        if (lastHeadingCalibrationNeeded == null || lastHeadingCalibrationNeeded) {
            visible = true;
            calibrationNeeded = false;
            owner.removeCallbacks(hideCalibrationOkRingRunnable);
            owner.postDelayed(hideCalibrationOkRingRunnable, CALIBRATION_OK_RING_VISIBLE_MS);
        }
        lastHeadingCalibrationNeeded = false;
    }

    private void hideCalibrationOkRing() {
        if (calibrationNeeded || !visible) {
            return;
        }
        visible = false;
        owner.invalidate();
    }
}
