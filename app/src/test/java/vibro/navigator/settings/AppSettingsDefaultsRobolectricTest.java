package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.logging.AppLogger;

@RunWith(RobolectricTestRunner.class)
public class AppSettingsDefaultsRobolectricTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void restoreClearsAllSettingOverridesAndOutputFolderSelections() {
        context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("future_setting", true).putInt("future_number", 42).apply();
        AppThemeSettings.setLightThemeEnabled(context, true);
        AppLanguageSettings.setUseSystemLanguageEnabled(context, false, "it");
        AppSettings.setValidatedGooglePoiApiKey(context, "custom-api-key");
        AppGpxSettings.setAutoSaveOnStopEnabled(context, false);
        AppOutputFolderSettings.set(context, AppOutputFolderSettings.Kind.GPX, "content://custom/gpx");
        AppOutputStorageSettings.select(context, "card", "content://custom/downloads");
        context.getSharedPreferences(AppDataBackupContract.PREFS_LOGGING, Context.MODE_PRIVATE)
                .edit().putBoolean("log_enabled", true).apply();

        AppSettingsDefaults.restore(context);

        assertTrue(context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE).getAll().isEmpty());
        assertFalse(AppThemeSettings.isLightThemeEnabled(context));
        assertTrue(AppLanguageSettings.isUseSystemLanguageEnabled(context));
        assertFalse(AppSettings.hasValidGooglePoiApiKey(context));
        assertTrue(AppGpxSettings.isAutoSaveOnStopEnabled(context));
        assertNull(AppOutputFolderSettings.get(context, AppOutputFolderSettings.Kind.GPX));
        assertFalse(AppOutputStorageSettings.useSdCard(context));
        assertEquals("", AppOutputStorageSettings.cardId(context));
        assertFalse(AppLogger.isLoggingEnabled(context));
        assertFalse(context.getSharedPreferences(AppDataBackupContract.PREFS_LOGGING, Context.MODE_PRIVATE)
                .getBoolean("log_enabled", true));
    }

    @Test
    public void restorePreservesNonSettingsData() {
        String[] dataFiles = {"vibenavigator_saved_routes", "vibenavigator_poi_history", "vibenavigator_brouter"};
        for (String file : dataFiles) {
            context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().putString("data", file).apply();
        }

        AppSettingsDefaults.restore(context);

        for (String file : dataFiles) {
            assertEquals(file, context.getSharedPreferences(file, Context.MODE_PRIVATE).getString("data", null));
        }
    }
}
