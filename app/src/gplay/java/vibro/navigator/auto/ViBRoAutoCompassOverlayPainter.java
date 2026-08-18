package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.format.NavigationSpeedLimitFormatter;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.route.RouteSpeedLimit;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNavigationCustomButtonTargetResources;
import vibro.navigator.settings.AppNavigationCustomButtonTargetState;

final class ViBRoAutoCompassOverlayPainter {

    private static final float CONTROL_SIZE_DP = 44f;
    private static final float CONTROL_RADIUS_DP = 22f;
    private static final float CONTROL_ICON_SIZE_DP = 24f;
    private static final float SPEED_BADGE_STROKE_DP = 5f;
    private static final float BUTTON_OUTLINE_WIDTH_DP = 1f;

    private final CarContext carContext;
    private final ViBRoAutoSurfaceRenderer.Controls controls;
    private final RectF customButtonBounds = new RectF();
    private final RectF speedLimitBounds = new RectF();
    private final Paint buttonFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint speedFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint speedStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint speedTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @Nullable
    private Drawable customIcon;
    private int customIconResId;
    private boolean customButtonVisible;

    ViBRoAutoCompassOverlayPainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls
    ) {
        this.carContext = carContext;
        this.controls = controls;
        initPaints();
    }

    void draw(@NonNull Canvas canvas, @NonNull NavState state, @NonNull RectF compassBounds, float scale) {
        float size = dp(CONTROL_SIZE_DP, scale);
        buttonOutlinePaint.setStrokeWidth(dp(BUTTON_OUTLINE_WIDTH_DP, scale));
        speedStrokePaint.setStrokeWidth(dp(SPEED_BADGE_STROKE_DP, scale));
        speedTextPaint.setTextSize(sp(16f, scale));
        speedLimitBounds.set(
                compassBounds.left,
                compassBounds.top,
                compassBounds.left + size,
                compassBounds.top + size
        );
        customButtonBounds.set(
                compassBounds.right - size,
                compassBounds.top,
                compassBounds.right,
                compassBounds.top + size
        );
        drawCustomButton(canvas, customButtonBounds, scale);
        drawSpeedLimit(canvas, speedLimitBounds, state.routeStatus.speedLimit);
    }

    boolean handleClick(float x, float y) {
        if (customButtonVisible && customButtonBounds.contains(x, y)) {
            controls.onToggleCustomButton();
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

        speedFillPaint.setStyle(Paint.Style.FILL);
        speedFillPaint.setColor(ContextCompat.getColor(carContext, R.color.white));

        speedStrokePaint.setStyle(Paint.Style.STROKE);
        speedStrokePaint.setStrokeWidth(dp(SPEED_BADGE_STROKE_DP, 1f));
        speedStrokePaint.setColor(ContextCompat.getColor(carContext, R.color.danger));

        speedTextPaint.setColor(ContextCompat.getColor(carContext, R.color.black));
        speedTextPaint.setTextAlign(Paint.Align.CENTER);
        speedTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        speedTextPaint.setSubpixelText(true);
        speedTextPaint.setTextSize(sp(16f, 1f));
    }

    private void drawIconButton(@NonNull Canvas canvas, @NonNull RectF bounds, @NonNull Drawable icon, float scale) {
        float radius = dp(CONTROL_RADIUS_DP, scale);
        canvas.drawRoundRect(bounds, radius, radius, buttonFillPaint);
        canvas.drawRoundRect(bounds, radius, radius, buttonOutlinePaint);
        drawIcon(canvas, bounds, icon, scale);
    }

    private void drawCustomButton(@NonNull Canvas canvas, @NonNull RectF bounds, float scale) {
        customButtonVisible = AppNavigationCustomButtonSettings.isEnabled(carContext);
        if (!customButtonVisible) {
            return;
        }
        Target target = AppNavigationCustomButtonSettings.getTarget(carContext);
        boolean enabled = AppNavigationCustomButtonTargetState.isEnabled(carContext, target);
        drawIconButton(
                canvas,
                bounds,
                customIcon(AppNavigationCustomButtonTargetResources.iconResId(target, enabled)),
                scale
        );
    }

    private void drawIcon(@NonNull Canvas canvas, @NonNull RectF bounds, @NonNull Drawable icon, float scale) {
        int iconSize = Math.round(dp(CONTROL_ICON_SIZE_DP, scale));
        int iconLeft = Math.round(bounds.centerX() - iconSize / 2f);
        int iconTop = Math.round(bounds.centerY() - iconSize / 2f);
        icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
        icon.draw(canvas);
    }

    private void drawSpeedLimit(
            @NonNull Canvas canvas,
            @NonNull RectF bounds,
            @Nullable RouteSpeedLimit speedLimit
    ) {
        if (speedLimit == null) {
            return;
        }
        canvas.drawOval(bounds, speedFillPaint);
        canvas.drawOval(bounds, speedStrokePaint);
        drawSpeedLimitText(canvas, bounds, NavigationSpeedLimitFormatter.formatBadge(speedLimit));
    }

    private void drawSpeedLimitText(@NonNull Canvas canvas, @NonNull RectF bounds, @NonNull String text) {
        Paint.FontMetrics metrics = speedTextPaint.getFontMetrics();
        float baseline = bounds.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(text, bounds.centerX(), baseline, speedTextPaint);
    }

    @NonNull
    private Drawable requireDrawable(int resId) {
        Drawable drawable = ContextCompat.getDrawable(carContext, resId);
        if (drawable == null) {
            throw new IllegalStateException("Missing drawable " + resId);
        }
        return drawable.mutate();
    }

    @NonNull
    private Drawable customIcon(int iconResId) {
        if (customIcon == null || customIconResId != iconResId) {
            customIcon = requireDrawable(iconResId);
            customIconResId = iconResId;
        }
        return customIcon;
    }

    private float dp(float value, float scale) {
        return ViBRoAutoRenderScale.dp(carContext, value, scale);
    }

    private float sp(float value, float scale) {
        return ViBRoAutoRenderScale.sp(carContext, value, scale);
    }
}
