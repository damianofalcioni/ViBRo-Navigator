package vibro.navigator.android.theme;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.settings.AppThemeSettings;

public final class AndroidAppTheme {

    private static final int[][] DARK_DEFAULT_COLORS = {
            {R.attr.vibroBackgroundColor, R.color.black},
            {R.attr.vibroTextPrimaryColor, R.color.white},
            {R.attr.vibroTextSecondaryColor, R.color.gray_700},
            {R.attr.vibroTextHintColor, R.color.gray_700},
            {R.attr.vibroSurfaceColor, R.color.surface_800},
            {R.attr.vibroSurfaceStrongColor, R.color.gray_900},
            {R.attr.vibroOutlineColor, R.color.outline},
            {R.attr.vibroRippleColor, R.color.ripple_light},
            {R.attr.vibroIconColor, R.color.white},
            {R.attr.vibroIconContrastColor, R.color.black},
            {R.attr.vibroSelectedSurfaceColor, R.color.dark_selected_surface},
            {R.attr.vibroMapPoiPanelColor, R.color.dark_map_poi_panel},
            {R.attr.vibroLogoLineColor, R.color.white},
            {R.attr.vibroCompassSurfaceColor, R.color.compass_surface},
            {R.attr.vibroCompassRingColor, R.color.compass_ring},
            {R.attr.vibroCompassMarkColor, R.color.white},
            {R.attr.vibroCompassCenterColor, R.color.compass_center},
            {R.attr.vibroCompassPausedRingColor, R.color.compass_paused_ring}
    };

    private AndroidAppTheme() {
    }

    public static boolean apply(@NonNull Activity activity) {
        boolean lightThemeEnabled = AppThemeSettings.isLightThemeEnabled(activity);
        activity.setTheme(lightThemeEnabled
                ? R.style.Theme_ViBRoNavigator_Light
                : R.style.Theme_ViBRoNavigator);
        return lightThemeEnabled;
    }

    public static boolean recreateIfThemeChanged(@NonNull Activity activity, boolean appliedLightTheme) {
        if (appliedLightTheme == AppThemeSettings.isLightThemeEnabled(activity) || activity.isFinishing()) {
            return false;
        }
        activity.recreate();
        return true;
    }

    @ColorInt
    public static int color(@NonNull Context context, @AttrRes int attrResId) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attrResId, value, true) && value.resourceId != 0) {
            return ContextCompat.getColor(context, value.resourceId);
        }
        if (value.data != 0) {
            return value.data;
        }
        return ContextCompat.getColor(context, darkDefaultColorResId(attrResId));
    }

    @ColorRes
    private static int darkDefaultColorResId(@AttrRes int attrResId) {
        for (int[] defaultColor : DARK_DEFAULT_COLORS) {
            if (defaultColor[0] == attrResId) {
                return defaultColor[1];
            }
        }
        throw new IllegalArgumentException("Theme attribute not defined: " + attrResId);
    }
}
