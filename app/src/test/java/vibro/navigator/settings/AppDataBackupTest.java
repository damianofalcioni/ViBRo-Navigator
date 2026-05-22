package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppDataBackupTest {

    private static final String[] PREFS = {
            "vibro.navigator.settings",
            "vibenavigator_poi_history",
            "vibenavigator_brouter",
            "app_logging"
    };
    private static final String GOOGLE_POI_API_KEY = "google-key";
    private static final String BACKUP_TYPE = "type";
    private static final String BACKUP_VALUE = "value";
    private static final String CATEGORY_FUEL = "Fuel";
    private static final String CATEGORY_RESTAURANT = "Restaurant";

    private Context context;

    @Before
    public void setUp() {
        Application application = ApplicationProvider.getApplicationContext();
        context = application;
        clearPrefs();
        AppLogger.init(context);
    }

    @Test
    public void exportImport_restoresKnownStoredData() throws Exception {
        BRouterProfilesRepository profiles = new BRouterProfilesRepository();
        Poi originalPoi = new Poi("Coffee", 48.2082d, 16.3738d);
        new PoiHistoryStore(context).addOrPromote(originalPoi);
        AppSettings.setFusedLocationEnabled(context, false);
        AppSettings.setImperialUnitsEnabled(context, true);
        AppSettings.setGooglePoiApiKey(context, GOOGLE_POI_API_KEY);
        AppSettings.setManeuverVoiceName(context, AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT);
        AppSettings.setMapPoiCategoryFilterEnabled(context, true);
        AppSettings.setMapPoiCategorySettings(context, Arrays.asList(
                new AppPoiCategorySetting(CATEGORY_FUEL, true),
                new AppPoiCategorySetting(CATEGORY_RESTAURANT, false)
        ));
        AppLogger.setLoggingEnabled(context, true);
        profiles.saveSelectedProfileKey(context, "trekking");
        profiles.saveCustomProfile(context, Uri.parse("content://profiles/custom.brf"), "custom");

        String json = AppDataBackup.exportJson(context);
        clearPrefs();
        AppLogger.init(context);

        AppDataBackup.importJson(context, json);

        List<Poi> restoredPois = new PoiHistoryStore(context).list();
        assertEquals(1, restoredPois.size());
        assertEquals(originalPoi.name, restoredPois.get(0).name);
        assertEquals(originalPoi.lat, restoredPois.get(0).lat, 0.0d);
        assertEquals(originalPoi.lon, restoredPois.get(0).lon, 0.0d);
        assertFalse(AppSettings.isFusedLocationEnabled(context));
        assertTrue(AppSettings.isImperialUnitsEnabled(context));
        assertEquals(GOOGLE_POI_API_KEY, AppSettings.getGooglePoiApiKey(context));
        assertEquals(AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT, AppSettings.getManeuverVoiceName(context));
        assertTrue(AppSettings.isMapPoiCategoryFilterEnabled(context));
        assertEquals(Arrays.asList(CATEGORY_FUEL, CATEGORY_RESTAURANT), AppSettings.getMapPoiCategoryNames(context));
        assertEquals(Arrays.asList(CATEGORY_FUEL), AppSettings.getEnabledMapPoiCategoryNames(context));
        assertTrue(AppLogger.isLoggingEnabled(context));
        assertEquals("trekking", profiles.getSelectedProfileKey(context));
        assertEquals("custom", profiles.getCustomProfileName(context));
    }

    @Test
    public void exportJson_containsTypedSharedPreferencePayload() throws Exception {
        AppSettings.setImperialUnitsEnabled(context, true);
        AppSettings.setGooglePoiApiKey(context, GOOGLE_POI_API_KEY);
        AppSettings.setManeuverVoiceName(context, AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT);
        AppSettings.setMapPoiCategoryFilterEnabled(context, true);
        AppSettings.setMapPoiCategorySettings(context, Arrays.asList(
                new AppPoiCategorySetting(CATEGORY_FUEL, true),
                new AppPoiCategorySetting(CATEGORY_RESTAURANT, false)
        ));

        JSONObject root = new JSONObject(AppDataBackup.exportJson(context));
        JSONObject sharedPreferences = root.getJSONObject("sharedPreferences");
        JSONObject appSettings = sharedPreferences.getJSONObject("vibro.navigator.settings");
        JSONObject imperialUnits = appSettings.getJSONObject("use_imperial_units");
        JSONObject googlePoiApiKey = appSettings.getJSONObject("google_poi_api_key");
        JSONObject maneuverVoiceName = appSettings.getJSONObject("maneuver_voice_name");
        JSONObject mapPoiCategoryFilterEnabled = appSettings.getJSONObject("map_poi_category_filter_enabled");
        JSONObject mapPoiCategoryNames = appSettings.getJSONObject("map_poi_category_names");

        assertEquals(1, root.getInt("schemaVersion"));
        assertEquals("boolean", imperialUnits.getString(BACKUP_TYPE));
        assertTrue(imperialUnits.getBoolean(BACKUP_VALUE));
        assertEquals("string", googlePoiApiKey.getString(BACKUP_TYPE));
        assertEquals(GOOGLE_POI_API_KEY, googlePoiApiKey.getString(BACKUP_VALUE));
        assertEquals("string", maneuverVoiceName.getString(BACKUP_TYPE));
        assertEquals(AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT, maneuverVoiceName.getString(BACKUP_VALUE));
        assertEquals("boolean", mapPoiCategoryFilterEnabled.getString(BACKUP_TYPE));
        assertTrue(mapPoiCategoryFilterEnabled.getBoolean(BACKUP_VALUE));
        assertEquals("string", mapPoiCategoryNames.getString(BACKUP_TYPE));
        JSONArray categories = new JSONArray(mapPoiCategoryNames.getString(BACKUP_VALUE));
        assertEquals(CATEGORY_FUEL, categories.getJSONObject(0).getString("name"));
        assertTrue(categories.getJSONObject(0).getBoolean("enabled"));
        assertEquals(CATEGORY_RESTAURANT, categories.getJSONObject(1).getString("name"));
        assertFalse(categories.getJSONObject(1).getBoolean("enabled"));
    }

    @Test
    public void importJson_rejectsInvalidPayloadWithoutChangingExistingData() {
        AppSettings.setImperialUnitsEnabled(context, true);

        try {
            AppDataBackup.importJson(context, "{\"schemaVersion\":999,\"sharedPreferences\":{}}");
        } catch (JSONException expected) {
            assertTrue(AppSettings.isImperialUnitsEnabled(context));
            return;
        }

        throw new AssertionError("Expected invalid backup to be rejected");
    }

    private void clearPrefs() {
        for (String prefsName : PREFS) {
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit();
        }
    }
}
