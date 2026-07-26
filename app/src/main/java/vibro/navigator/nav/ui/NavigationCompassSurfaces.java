package vibro.navigator.nav.ui;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.ui.NavigationCompassView;

final class NavigationCompassSurfaces {
    private static final int FOREGROUND_PANEL_ALPHA = 210;
    private static final float FOREGROUND_PANEL_RADIUS_DP = 8f;
    private static final float FOREGROUND_PANEL_HORIZONTAL_PADDING_DP = 10f;
    private static final float FOREGROUND_PANEL_VERTICAL_PADDING_DP = 6f;
    private static final float FULLSCREEN_CENTER_ANCHOR_OFFSET_DP = 64f;

    @NonNull
    private final Activity activity;
    @NonNull
    private final NavigationCompassView compactCompass;
    @NonNull
    private final NavigationCompassView fullscreenCompass;
    @NonNull
    private final ForegroundPanelList foregroundPanels = new ForegroundPanelList();
    private final int[] fullscreenWindowLocation = new int[2];
    private final int[] anchorWindowLocation = new int[2];
    private final boolean fullscreenOverlaysForeground;

    @Nullable
    private View firstFullscreenCenterAnchor;
    @Nullable
    private View secondFullscreenCenterAnchor;
    @Nullable
    private Boolean lastFullscreenRouteMode;

    NavigationCompassSurfaces(
            @NonNull Activity activity,
            @NonNull View directionsBlock,
            @NonNull View destination
    ) {
        this.activity = activity;
        compactCompass = activity.findViewById(R.id.navigationCompassView);
        fullscreenCompass = activity.findViewById(R.id.navigationFullscreenCompassView);
        fullscreenOverlaysForeground = fullscreenCompass.getParent() != compactCompass.getParent();
        foregroundPanels.add(directionsBlock);
        foregroundPanels.add(destination);
    }

    void includeForegroundText(@NonNull View view) {
        foregroundPanels.add(view);
    }

    void alignFullscreenCenterWith(@NonNull View firstAnchor, @NonNull View secondAnchor) {
        firstFullscreenCenterAnchor = firstAnchor;
        secondFullscreenCenterAnchor = secondAnchor;
    }

    void setOnClickListener(@NonNull View.OnClickListener listener) {
        compactCompass.setOnClickListener(listener);
        fullscreenCompass.setOnClickListener(listener);
    }

    void render(
            boolean fullscreenRouteMode,
            boolean navigationPaused,
            @Nullable NavCompassState compassState
    ) {
        applyFullscreenRouteMode(fullscreenRouteMode);
        updateFullscreenCenterY();
        compactCompass.setNavigationPaused(navigationPaused);
        fullscreenCompass.setNavigationPaused(navigationPaused);
        compactCompass.setCompassState(compassState);
        fullscreenCompass.setCompassState(compassState);
    }

    private void applyFullscreenRouteMode(boolean enabled) {
        if (lastFullscreenRouteMode != null && lastFullscreenRouteMode == enabled) {
            return;
        }
        lastFullscreenRouteMode = enabled;
        fullscreenCompass.setVisibility(enabled ? View.VISIBLE : View.GONE);
        compactCompass.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);
        if (enabled) {
            fullscreenCompass.post(this::updateFullscreenCenterY);
        } else {
            fullscreenCompass.setFullscreenCenterYHint(Float.NaN);
        }
        foregroundPanels.apply(
                enabled && fullscreenOverlaysForeground,
                this::newForegroundPanelBackground,
                foregroundPadding()
        );
    }

    private void updateFullscreenCenterY() {
        if (!Boolean.TRUE.equals(lastFullscreenRouteMode)) {
            fullscreenCompass.setFullscreenCenterYHint(Float.NaN);
            return;
        }
        float centerY = resolveAnchorCenterY();
        if (isPositiveFinite(centerY)) {
            fullscreenCompass.setFullscreenCenterYHint(centerY - dp(FULLSCREEN_CENTER_ANCHOR_OFFSET_DP));
        } else {
            fullscreenCompass.setFullscreenCenterYHint(Float.NaN);
        }
    }

    private float resolveAnchorCenterY() {
        float sum = 0f;
        int count = 0;
        float firstCenterY = resolveAnchorCenterY(firstFullscreenCenterAnchor);
        if (isPositiveFinite(firstCenterY)) {
            sum += firstCenterY;
            count++;
        }
        float secondCenterY = resolveAnchorCenterY(secondFullscreenCenterAnchor);
        if (isPositiveFinite(secondCenterY)) {
            sum += secondCenterY;
            count++;
        }
        return count == 0 ? Float.NaN : sum / count;
    }

    private float resolveAnchorCenterY(@Nullable View anchor) {
        if (anchor == null || fullscreenCompass.getHeight() <= 0 || anchor.getHeight() <= 0) {
            return Float.NaN;
        }
        fullscreenCompass.getLocationOnScreen(fullscreenWindowLocation);
        anchor.getLocationOnScreen(anchorWindowLocation);
        return anchorWindowLocation[1]
                - fullscreenWindowLocation[1]
                + (anchor.getHeight() / 2f);
    }

    @NonNull
    private GradientDrawable newForegroundPanelBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(withAlpha(AndroidAppTheme.color(activity, R.attr.vibroSurfaceColor)));
        background.setCornerRadius(dp(FOREGROUND_PANEL_RADIUS_DP));
        return background;
    }

    @NonNull
    private ForegroundPanelList.Padding foregroundPadding() {
        return new ForegroundPanelList.Padding(
                dpInt(FOREGROUND_PANEL_HORIZONTAL_PADDING_DP),
                dpInt(FOREGROUND_PANEL_VERTICAL_PADDING_DP)
        );
    }

    private static int withAlpha(int color) {
        return (color & 0x00FFFFFF) | (FOREGROUND_PANEL_ALPHA << 24);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }

    private int dpInt(float value) {
        return Math.round(dp(value));
    }

    private static boolean isPositiveFinite(float value) {
        return value > 0f && !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
