package vibro.navigator.settings;

import android.content.Context;

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
                    AppSettings::isManeuverSpeechEnabled,
                    AppSettings::setManeuverSpeechEnabled
            )
    };

    private AppNavigationCustomButtonTargetState() {
    }

    public static boolean isEnabled(@NonNull Context context, @NonNull Target target) {
        return entryFor(target).reader.isEnabled(context);
    }

    public static void setEnabled(@NonNull Context context, @NonNull Target target, boolean enabled) {
        entryFor(target).writer.setEnabled(context, enabled);
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
        boolean isEnabled(@NonNull Context context);
    }

    private interface Writer {
        void setEnabled(@NonNull Context context, boolean enabled);
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
