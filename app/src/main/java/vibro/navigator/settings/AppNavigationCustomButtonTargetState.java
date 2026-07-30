package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;

public final class AppNavigationCustomButtonTargetState {
    private static final Entry[] ENTRIES = {
            new Entry(
                    Target.DYNAMIC_GPS_INTERVAL,
                    AppLocationSettings::isDynamicGpsFixIntervalEnabled,
                    AppLocationSettings::setDynamicGpsFixIntervalEnabled
            ),
            new Entry(
                    Target.LIGHT_THEME,
                    AppThemeSettings::isLightThemeEnabled,
                    AppThemeSettings::setLightThemeEnabled
            ),
            new Entry(
                    Target.SURROUNDING_STREETS,
                    AppCompassSettings::isSurroundingStreetsEnabled,
                    AppCompassSettings::setSurroundingStreetsEnabled
            ),
            new Entry(
                    Target.FULLSCREEN_ROUTE,
                    AppCompassSettings::isFullscreenRouteEnabled,
                    AppCompassSettings::setFullscreenRouteEnabled
            ),
            new Entry(
                    Target.NOTIFICATIONS,
                    AppNotificationSettings::areNavigationNotificationsEnabled,
                    AppNotificationSettings::setNavigationNotificationsEnabled
            ),
            new Entry(
                    Target.SPEECH_DIRECTIONS,
                    AppSettingsPreferenceValues::isManeuverSpeechEnabled,
                    AppSettingsPreferenceValues::setManeuverSpeechEnabled
            )
    };

    private AppNavigationCustomButtonTargetState() {
    }

    public static boolean isEnabled(@NonNull Context context, @NonNull Target target) {
        return isEnabled(prefs(context), target);
    }

    static boolean isEnabled(@NonNull SharedPreferences preferences, @NonNull Target target) {
        return entryFor(target).reader.isEnabled(preferences);
    }

    public static void setEnabled(@NonNull Context context, @NonNull Target target, boolean enabled) {
        setEnabled(prefs(context), target, enabled);
    }

    static void setEnabled(@NonNull SharedPreferences preferences, @NonNull Target target, boolean enabled) {
        entryFor(target).writer.setEnabled(preferences, enabled);
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

    private interface Reader {
        boolean isEnabled(@NonNull SharedPreferences preferences);
    }

    private interface Writer {
        void setEnabled(@NonNull SharedPreferences preferences, boolean enabled);
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }

    private static final class Entry {
        @NonNull
        private final Target target;
        @NonNull
        private final Reader reader;
        @NonNull
        private final Writer writer;

        private Entry(
                @NonNull Target target,
                @NonNull Reader reader,
                @NonNull Writer writer
        ) {
            this.target = target;
            this.reader = reader;
            this.writer = writer;
        }
    }
}
