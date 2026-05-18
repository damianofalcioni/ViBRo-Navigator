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

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

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
        assertTrue(AppLogger.isLoggingEnabled(context));
        assertEquals("trekking", profiles.getSelectedProfileKey(context));
        assertEquals("custom", profiles.getCustomProfileName(context));
    }

    @Test
    public void exportJson_containsTypedSharedPreferencePayload() throws Exception {
        AppSettings.setImperialUnitsEnabled(context, true);
        AppSettings.setGooglePoiApiKey(context, GOOGLE_POI_API_KEY);

        JSONObject root = new JSONObject(AppDataBackup.exportJson(context));
        JSONObject sharedPreferences = root.getJSONObject("sharedPreferences");
        JSONObject appSettings = sharedPreferences.getJSONObject("vibro.navigator.settings");
        JSONObject imperialUnits = appSettings.getJSONObject("use_imperial_units");
        JSONObject googlePoiApiKey = appSettings.getJSONObject("google_poi_api_key");

        assertEquals(1, root.getInt("schemaVersion"));
        assertEquals("boolean", imperialUnits.getString("type"));
        assertTrue(imperialUnits.getBoolean("value"));
        assertEquals("string", googlePoiApiKey.getString("type"));
        assertEquals(GOOGLE_POI_API_KEY, googlePoiApiKey.getString("value"));
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
