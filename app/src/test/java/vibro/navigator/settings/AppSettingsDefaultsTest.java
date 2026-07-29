package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.logging.AppLogger;

@RunWith(RobolectricTestRunner.class)
public class AppSettingsDefaultsTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        context.getSharedPreferences("app_logging", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        AppLogger.init(context);
    }

    @Test
    public void freshInstallDefaultsMatchRequestedSettings() {
        assertFalse(AppThemeSettings.isLightThemeEnabled(context));
        assertFalse(AppSettings.isImperialUnitsEnabled(context));
        assertTrue(AppNotificationSettings.areNavigationNotificationsEnabled(context));
        assertTrue(AppSettings.isManeuverSpeechEnabled(context));
        assertTrue(AppLocationSettings.isDynamicGpsFixIntervalEnabled(context));
        assertFalse(AppNotificationSettings.isSingleInstructionModeEnabled(context));
        assertTrue(AppNavigationCustomButtonSettings.isEnabled(context));
        assertEquals(
                AppNavigationCustomButtonSettings.Target.LIGHT_THEME,
                AppNavigationCustomButtonSettings.getTarget(context)
        );
        assertTrue(AppGpxSettings.isAutoSaveOnStopEnabled(context));
        assertTrue(AppAndroidAutoSettings.isIntegrationEnabled(context));
        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(context));
        assertFalse(AppCompassSettings.isInstantZoomEnabled(context));
        assertTrue(AppCompassSettings.isFullscreenRouteEnabled(context));
        assertTrue(AppSettings.isMapPoiCategoryFilterEnabled(context));
        assertTrue(AppSpeechRecognitionSettings.isEnabled(context));
        assertFalse(AppSettings.hasValidGooglePoiApiKey(context));
        assertFalse(AppSettings.isGooglePoiSearchEnabled(context));
        assertTrue(AppSettings.isFusedLocationEnabled(context));
        assertFalse(AppLogger.isLoggingEnabled(context));
    }

    @Test
    public void explicitSpeechDirectionsDisabledValueStillWins() {
        AppSettings.setManeuverSpeechEnabled(context, false);

        assertFalse(AppSettings.isManeuverSpeechEnabled(context));
    }
}
