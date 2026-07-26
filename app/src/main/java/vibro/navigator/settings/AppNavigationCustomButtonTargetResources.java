package vibro.navigator.settings;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import vibro.navigator.R;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;

public final class AppNavigationCustomButtonTargetResources {
    private static final Entry[] ENTRIES = {
            new Entry(
                    Target.DYNAMIC_GPS_INTERVAL,
                    R.string.label_dynamic_gps_fix_interval_enabled,
                    R.drawable.ic_custom_dynamic_gps_interval_enabled,
                    R.drawable.ic_custom_dynamic_gps_interval_disabled
            ),
            new Entry(
                    Target.LIGHT_THEME,
                    R.string.label_light_theme_enabled,
                    R.drawable.ic_custom_light_theme_enabled,
                    R.drawable.ic_custom_light_theme_disabled
            ),
            new Entry(
                    Target.SURROUNDING_STREETS,
                    R.string.label_compass_surrounding_streets_enabled,
                    R.drawable.ic_custom_surrounding_streets_enabled,
                    R.drawable.ic_custom_surrounding_streets_disabled
            ),
            new Entry(
                    Target.FULLSCREEN_ROUTE,
                    R.string.label_compass_fullscreen_route_enabled,
                    R.drawable.ic_custom_fullscreen_route_enabled,
                    R.drawable.ic_custom_fullscreen_route_disabled
            ),
            new Entry(
                    Target.NOTIFICATIONS,
                    R.string.label_navigation_notifications_enabled,
                    R.drawable.ic_custom_notifications_enabled,
                    R.drawable.ic_custom_notifications_disabled
            ),
            new Entry(
                    Target.SPEECH_DIRECTIONS,
                    R.string.label_maneuver_voice,
                    R.drawable.ic_custom_speech_directions_enabled,
                    R.drawable.ic_custom_speech_directions_disabled
            )
    };

    private AppNavigationCustomButtonTargetResources() {
    }

    @NonNull
    public static Target[] selectableTargets() {
        Target[] targets = new Target[ENTRIES.length];
        for (int i = 0; i < ENTRIES.length; i++) {
            targets[i] = ENTRIES[i].target;
        }
        return targets;
    }

    @StringRes
    public static int labelResId(@NonNull Target target) {
        return entryFor(target).labelResId;
    }

    @DrawableRes
    public static int iconResId(@NonNull Target target, boolean enabled) {
        Entry entry = entryFor(target);
        return enabled ? entry.enabledIconResId : entry.disabledIconResId;
    }

    @NonNull
    private static Entry entryFor(@NonNull Target target) {
        for (Entry entry : ENTRIES) {
            if (entry.target == target) {
                return entry;
            }
        }
        throw new IllegalArgumentException("Unsupported custom-button target=" + target);
    }

    private static final class Entry {
        @NonNull
        private final Target target;
        @StringRes
        private final int labelResId;
        @DrawableRes
        private final int enabledIconResId;
        @DrawableRes
        private final int disabledIconResId;

        private Entry(
                @NonNull Target target,
                @StringRes int labelResId,
                @DrawableRes int enabledIconResId,
                @DrawableRes int disabledIconResId
        ) {
            this.target = target;
            this.labelResId = labelResId;
            this.enabledIconResId = enabledIconResId;
            this.disabledIconResId = disabledIconResId;
        }
    }
}
