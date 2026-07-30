package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.junit.Test;

import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.testutil.InMemorySharedPreferences;

public class AppNavigationCustomButtonSettingsTest {
    private final SharedPreferences preferences = new InMemorySharedPreferences();

    @Test
    public void defaultsToVisibleLightThemeButton() {
        assertTrue(AppNavigationCustomButtonSettings.isEnabled(preferences));
        assertEquals(Target.LIGHT_THEME, AppNavigationCustomButtonSettings.getTarget(preferences));
    }

    @Test
    public void persistsEnabledAndSelectedTarget() {
        AppNavigationCustomButtonSettings.setEnabled(preferences, true);
        AppNavigationCustomButtonSettings.setTarget(preferences, Target.SPEECH_DIRECTIONS);

        assertTrue(AppNavigationCustomButtonSettings.isEnabled(preferences));
        assertEquals(Target.SPEECH_DIRECTIONS, AppNavigationCustomButtonSettings.getTarget(preferences));
    }

    @Test
    public void unknownSavedTargetFallsBackToLightTheme() {
        preferences.edit()
                .putString("navigation_custom_button_target", "unknown")
                .commit();

        assertEquals(Target.LIGHT_THEME, AppNavigationCustomButtonSettings.getTarget(preferences));
    }

    @Test
    public void targetStateReadsAndWritesFullscreenRouteSetting() {
        AppCompassSettings.setFullscreenRouteEnabled(preferences, false);

        AppNavigationCustomButtonTargetState.setEnabled(preferences, Target.FULLSCREEN_ROUTE, true);

        assertTrue(AppCompassSettings.isFullscreenRouteEnabled(preferences));
        assertTrue(AppNavigationCustomButtonTargetState.isEnabled(preferences, Target.FULLSCREEN_ROUTE));
    }

    @Test
    public void targetStateReadsAndWritesNavigationNotificationSetting() {
        AppNotificationSettings.setNavigationNotificationsEnabled(preferences, true);

        AppNavigationCustomButtonTargetState.setEnabled(preferences, Target.NOTIFICATIONS, false);

        assertFalse(AppNotificationSettings.areNavigationNotificationsEnabled(preferences));
        assertFalse(AppNavigationCustomButtonTargetState.isEnabled(preferences, Target.NOTIFICATIONS));
    }
}
