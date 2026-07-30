package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.junit.Test;

import vibro.navigator.testutil.InMemorySharedPreferences;

public class AppSettingsDefaultsTest {
    private final SharedPreferences preferences = new InMemorySharedPreferences();

    @Test
    public void freshInstallDefaultsMatchRequestedSettings() {
        assertFalse(AppThemeSettings.isLightThemeEnabled(preferences));
        assertFalse(AppSettingsPreferenceValues.isImperialUnitsEnabled(preferences));
        assertTrue(AppNotificationSettings.areNavigationNotificationsEnabled(preferences));
        assertTrue(AppSettingsPreferenceValues.isManeuverSpeechEnabled(preferences));
        assertTrue(AppLocationSettings.isDynamicGpsFixIntervalEnabled(preferences));
        assertFalse(AppNotificationSettings.isSingleInstructionModeEnabled(preferences));
        assertTrue(AppNavigationCustomButtonSettings.isEnabled(preferences));
        assertEquals(
                AppNavigationCustomButtonSettings.Target.LIGHT_THEME,
                AppNavigationCustomButtonSettings.getTarget(preferences)
        );
        assertTrue(AppGpxSettings.isAutoSaveOnStopEnabled(preferences));
        assertTrue(AppAndroidAutoSettings.isIntegrationEnabled(preferences));
        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(preferences));
        assertFalse(AppCompassSettings.isInstantZoomEnabled(preferences));
        assertTrue(AppCompassSettings.isFullscreenRouteEnabled(preferences));
        assertTrue(AppPoiCategoryPreferences.isMapPoiCategoryFilterEnabled(preferences));
        assertTrue(AppSpeechRecognitionSettings.isEnabled(preferences));
        assertFalse(AppSettingsPreferenceValues.hasValidGooglePoiApiKey(preferences));
        assertFalse(AppSettingsPreferenceValues.isGooglePoiSearchEnabled(preferences));
        assertTrue(AppSettingsPreferenceValues.isFusedLocationEnabled(preferences));
    }

    @Test
    public void explicitSpeechDirectionsDisabledValueStillWins() {
        AppSettingsPreferenceValues.setManeuverSpeechEnabled(preferences, false);

        assertFalse(AppSettingsPreferenceValues.isManeuverSpeechEnabled(preferences));
    }
}
