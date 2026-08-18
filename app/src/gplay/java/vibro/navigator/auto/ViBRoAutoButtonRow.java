package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.model.NavState;

final class ViBRoAutoButtonRow {

    static final float BUTTON_SIZE_DP = 56f;

    private static final float BUTTON_ICON_SIZE_DP = 32f;
    private static final float BUTTON_OUTLINE_WIDTH_DP = 1.4f;

    private final CarContext carContext;
    private final ViBRoAutoSurfaceRenderer.Controls controls;
    private final Paint buttonFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF blockedButtonBounds = new RectF();
    private final RectF stopButtonBounds = new RectF();
    private final RectF pauseButtonBounds = new RectF();
    private final Drawable blockedIcon;
    private final Drawable stopIcon;
    private final Drawable pauseIcon;
    private final Drawable playIcon;

    ViBRoAutoButtonRow(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls
    ) {
        this.carContext = carContext;
        this.controls = controls;
        blockedIcon = requireDrawable(R.drawable.ic_blocked_road);
        stopIcon = requireDrawable(R.drawable.ic_stop);
        pauseIcon = requireDrawable(R.drawable.ic_pause);
        playIcon = requireDrawable(R.drawable.ic_play);
        initPaints();
    }

    void draw(@NonNull Canvas canvas, @NonNull NavState state, float left, float bottom, float width, float scale) {
        float size = buttonSizePx(scale);
        float gap = Math.max(dp(10f, scale), (width - size * 3f) / 4f);
        float top = bottom - size;
        setButtonBounds(blockedButtonBounds, left + gap, top, size);
        setButtonBounds(stopButtonBounds, blockedButtonBounds.right + gap, top, size);
        setButtonBounds(pauseButtonBounds, stopButtonBounds.right + gap, top, size);
        drawButton(canvas, blockedButtonBounds, blockedIcon, isBlockedRoadEnabled(state), scale);
        drawButton(canvas, stopButtonBounds, stopIcon, true, scale);
        drawButton(canvas, pauseButtonBounds, state.pauseStatus.paused ? playIcon : pauseIcon, true, scale);
    }

    boolean handleClick(float x, float y, @NonNull NavState state) {
        if (isBlockedRoadEnabled(state) && blockedButtonBounds.contains(x, y)) {
            controls.onBlockedRoad();
            return true;
        }
        if (stopButtonBounds.contains(x, y)) {
            controls.onStopNavigation();
            return true;
        }
        if (pauseButtonBounds.contains(x, y)) {
            controls.onTogglePaused();
            return true;
        }
        return false;
    }

    private void initPaints() {
        buttonFillPaint.setStyle(Paint.Style.FILL);
        buttonFillPaint.setColor(AndroidAppTheme.color(carContext, R.attr.vibroSurfaceColor));

        buttonOutlinePaint.setStyle(Paint.Style.STROKE);
        buttonOutlinePaint.setStrokeWidth(dp(BUTTON_OUTLINE_WIDTH_DP, 1f));
        buttonOutlinePaint.setColor(AndroidAppTheme.color(carContext, R.attr.vibroOutlineColor));
    }

    float buttonSizePx(float scale) {
        return dp(BUTTON_SIZE_DP, scale);
    }

    private void setButtonBounds(@NonNull RectF bounds, float left, float top, float size) {
        bounds.set(left, top, left + size, top + size);
    }

    private void drawButton(
            @NonNull Canvas canvas,
            @NonNull RectF bounds,
            @NonNull Drawable icon,
            boolean enabled,
            float scale
    ) {
        buttonOutlinePaint.setStrokeWidth(dp(BUTTON_OUTLINE_WIDTH_DP, scale));
        buttonFillPaint.setColor(AndroidAppTheme.color(
                carContext,
                enabled ? R.attr.vibroSurfaceColor : R.attr.vibroSurfaceStrongColor
        ));
        buttonOutlinePaint.setColor(AndroidAppTheme.color(
                carContext,
                enabled ? R.attr.vibroOutlineColor : R.attr.vibroTextSecondaryColor
        ));
        buttonFillPaint.setAlpha(enabled ? 255 : 140);
        buttonOutlinePaint.setAlpha(enabled ? 255 : 160);
        canvas.drawOval(bounds, buttonFillPaint);
        canvas.drawOval(bounds, buttonOutlinePaint);
        drawIcon(canvas, bounds, icon, enabled, scale);
    }

    private void drawIcon(
            @NonNull Canvas canvas,
            @NonNull RectF bounds,
            @NonNull Drawable icon,
            boolean enabled,
            float scale
    ) {
        int iconSize = Math.round(dp(BUTTON_ICON_SIZE_DP, scale));
        int iconLeft = Math.round(bounds.centerX() - iconSize / 2f);
        int iconTop = Math.round(bounds.centerY() - iconSize / 2f);
        icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
        resetIconTint(icon);
        if (!enabled) {
            icon.setTint(AndroidAppTheme.color(carContext, R.attr.vibroTextSecondaryColor));
        }
        icon.setAlpha(enabled ? 255 : 115);
        icon.draw(canvas);
        resetIconTint(icon);
        icon.setAlpha(255);
    }

    private static void resetIconTint(@NonNull Drawable icon) {
        icon.setTintList(null);
        icon.clearColorFilter();
    }

    private static boolean isBlockedRoadEnabled(@NonNull NavState state) {
        return state.routeStatus.blockedRoadActionAvailable && !state.pauseStatus.paused;
    }

    @NonNull
    private Drawable requireDrawable(int resId) {
        Drawable drawable = androidx.core.content.ContextCompat.getDrawable(carContext, resId);
        if (drawable == null) {
            throw new IllegalStateException("Missing drawable " + resId);
        }
        return drawable.mutate();
    }

    private float dp(float value, float scale) {
        return ViBRoAutoRenderScale.dp(carContext, value, scale);
    }
}
