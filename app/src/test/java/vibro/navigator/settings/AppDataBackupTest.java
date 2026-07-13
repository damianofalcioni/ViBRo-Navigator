package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavigationRoutingMode;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class AppDataBackupTest {

    private static final String[] PREFS = {
            "vibro.navigator.settings",
            "vibenavigator_poi_history",
            "vibenavigator_brouter",
            "app_logging",
            "vibenavigator_saved_routes"
    };
    private static final String PREFS_SAVED_ROUTES = "vibenavigator_saved_routes";
    private static final String KEY_SAVED_ROUTE_ITEMS = "items";
    private static final String KEY_SHARED_PREFERENCES = "sharedPreferences";
    private static final String GOOGLE_POI_API_KEY = "google-key";
    private static final String BACKUP_TYPE = "type";
    private static final String BACKUP_TYPE_BOOLEAN = "boolean";
    private static final String BACKUP_TYPE_STRING = "string";
    private static final String BACKUP_TYPE_STRING_SET = "stringSet";
    private static final String BACKUP_VALUE = "value";
    private static final String CATEGORY_FUEL = "Fuel";
    private static final String CATEGORY_RESTAURANT = "Restaurant";
    private static final String PROFILE_TREKKING = "trekking";
    private static final String PARAM_AVOID_PATH = "avoid_path";
    private static final String SAVED_ROUTE_PAYLOAD = "[{\"id\":\"route-1\",\"name\":\"Route One\","
            + "\"createdAtMillis\":1,\"destination\":{\"name\":\"Destination\",\"lat\":48.2082,"
            + "\"lon\":16.3738},\"stops\":[{\"name\":\"Stop\",\"lat\":47.0707,\"lon\":15.4395}]}]";

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
        BRouterProfilesRepository profiles = AndroidBRouterProfilesRepositoryFactory.create();
        Poi originalPoi = new Poi("Coffee", 48.2082d, 16.3738d);
        new PoiHistoryStore(context).addOrPromote(originalPoi);
        AppSettings.setFusedLocationEnabled(context, false);
        AppGpxSettings.setAutoSaveOnStopEnabled(context, true);
        AppLocationSettings.setDynamicGpsFixIntervalEnabled(context, false);
        AppSettings.setImperialUnitsEnabled(context, true);
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
        AppNotificationSettings.setNavigationNotificationsEnabled(context, false);
        AppMainUiSettings.setRoutingMode(context, NavigationRoutingMode.ROUND_TRIP);
        AppThemeSettings.setLightThemeEnabled(context, true);
        AppSettings.setValidatedGooglePoiApiKey(context, GOOGLE_POI_API_KEY);
        AppSettings.setGooglePoiSearchEnabled(context, false);
        AppAndroidAutoSettings.setIntegrationEnabled(context, false);
        AppSettings.setManeuverVoiceName(context, AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT);
        AppSettings.setMapPoiCategoryFilterEnabled(context, true);
        AppSettings.setMapPoiCategorySettings(context, Arrays.asList(
                new AppPoiCategorySetting(CATEGORY_FUEL, true),
                new AppPoiCategorySetting(CATEGORY_RESTAURANT, false)
        ));
        AppLogger.setLoggingEnabled(context, true);
        profiles.saveSelectedProfileKey(context, PROFILE_TREKKING);
        profiles.saveCustomProfile(context, Uri.parse("content://profiles/custom.brf"), "custom");
        Map<String, String> profileParams = new HashMap<>();
        profileParams.put(PARAM_AVOID_PATH, "1");
        profiles.saveProfileParameterValues(
                context,
                PROFILE_TREKKING,
                Collections.singletonList(new BRouterProfileParameter(
                        PARAM_AVOID_PATH,
                        "Avoid paths",
                        "0",
                        BRouterProfileParameter.ValueType.BOOLEAN,
                        null
                )),
                profileParams
        );
        context.getSharedPreferences(PREFS_SAVED_ROUTES, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_ROUTE_ITEMS, SAVED_ROUTE_PAYLOAD)
                .commit();

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
        assertTrue(AppGpxSettings.isAutoSaveOnStopEnabled(context));
        assertFalse(AppLocationSettings.isDynamicGpsFixIntervalEnabled(context));
        assertTrue(AppSettings.isImperialUnitsEnabled(context));
        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(context));
        assertFalse(AppNotificationSettings.areNavigationNotificationsEnabled(context));
        assertEquals(NavigationRoutingMode.ROUND_TRIP, AppMainUiSettings.getRoutingMode(context));
        assertTrue(AppThemeSettings.isLightThemeEnabled(context));
        assertEquals(GOOGLE_POI_API_KEY, AppSettings.getGooglePoiApiKey(context));
        assertTrue(AppSettings.hasValidGooglePoiApiKey(context));
        assertFalse(AppSettings.isGooglePoiSearchEnabled(context));
        assertFalse(AppAndroidAutoSettings.isIntegrationEnabled(context));
        assertEquals(AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT, AppSettings.getManeuverVoiceName(context));
        assertTrue(AppSettings.isMapPoiCategoryFilterEnabled(context));
        assertEquals(Arrays.asList(CATEGORY_FUEL, CATEGORY_RESTAURANT), AppSettings.getMapPoiCategoryNames(context));
        assertEquals(Arrays.asList(CATEGORY_FUEL), AppSettings.getEnabledMapPoiCategoryNames(context));
        assertTrue(AppLogger.isLoggingEnabled(context));
        assertEquals(PROFILE_TREKKING, profiles.getSelectedProfileKey(context));
        assertEquals("custom", profiles.getCustomProfileName(context));
        assertEquals(PARAM_AVOID_PATH + "=1",
                profiles.getProfileParameterOverridesExtraParams(context, PROFILE_TREKKING));
        assertEquals(SAVED_ROUTE_PAYLOAD, context.getSharedPreferences(PREFS_SAVED_ROUTES, Context.MODE_PRIVATE)
                .getString(KEY_SAVED_ROUTE_ITEMS, ""));
    }

    @Test
    public void exportJson_containsTypedSharedPreferencePayload() throws Exception {
        AppSettings.setImperialUnitsEnabled(context, true);
        AppGpxSettings.setAutoSaveOnStopEnabled(context, true);
        AppLocationSettings.setDynamicGpsFixIntervalEnabled(context, false);
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
        AppNotificationSettings.setNavigationNotificationsEnabled(context, false);
        AppMainUiSettings.setRoutingMode(context, NavigationRoutingMode.STRAIGHT_LINE);
        AppThemeSettings.setLightThemeEnabled(context, true);
        AppSettings.setValidatedGooglePoiApiKey(context, GOOGLE_POI_API_KEY);
        AppSettings.setGooglePoiSearchEnabled(context, false);
        AppAndroidAutoSettings.setIntegrationEnabled(context, false);
        AppSettings.setManeuverVoiceName(context, AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT);
        AppSettings.setMapPoiCategoryFilterEnabled(context, true);
        AppSettings.setMapPoiCategorySettings(context, Arrays.asList(
                new AppPoiCategorySetting(CATEGORY_FUEL, true),
                new AppPoiCategorySetting(CATEGORY_RESTAURANT, false)
        ));

        JSONObject root = new JSONObject(AppDataBackup.exportJson(context));
        JSONObject sharedPreferences = root.getJSONObject(KEY_SHARED_PREFERENCES);
        JSONObject appSettings = sharedPreferences.getJSONObject("vibro.navigator.settings");
        JSONObject compassSurroundingStreets = appSettings.getJSONObject("compass_surrounding_streets_enabled");
        JSONObject navigationNotifications = appSettings.getJSONObject("navigation_notifications_enabled");
        JSONObject mainUiRoutingMode = appSettings.getJSONObject("main_ui_routing_mode");
        JSONObject lightTheme = appSettings.getJSONObject("light_theme");
        JSONObject googlePoiApiKey = appSettings.getJSONObject("google_poi_api_key");
        JSONObject googlePoiApiKeyValid = appSettings.getJSONObject("google_poi_api_key_valid");
        JSONObject googlePoiSearchEnabled = appSettings.getJSONObject("google_poi_search_enabled");
        JSONObject androidAutoIntegrationEnabled = appSettings.getJSONObject("android_auto_integration_enabled");
        JSONObject maneuverVoiceName = appSettings.getJSONObject("maneuver_voice_name");
        JSONObject mapPoiCategoryFilterEnabled = appSettings.getJSONObject("map_poi_category_filter_enabled");
        JSONObject mapPoiCategoryNames = appSettings.getJSONObject("map_poi_category_names");

        assertEquals(1, root.getInt("schemaVersion"));
        assertBooleanPreference(appSettings, "use_imperial_units", true);
        assertBooleanPreference(appSettings, "auto_save_gpx_on_stop_enabled", true);
        assertBooleanPreference(appSettings, "dynamic_gps_fix_interval_enabled", false);
        assertEquals(BACKUP_TYPE_BOOLEAN, compassSurroundingStreets.getString(BACKUP_TYPE));
        assertTrue(compassSurroundingStreets.getBoolean(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_BOOLEAN, navigationNotifications.getString(BACKUP_TYPE));
        assertFalse(navigationNotifications.getBoolean(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_STRING, mainUiRoutingMode.getString(BACKUP_TYPE));
        assertEquals(NavigationRoutingMode.STRAIGHT_LINE.serializedName(), mainUiRoutingMode.getString(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_BOOLEAN, lightTheme.getString(BACKUP_TYPE));
        assertTrue(lightTheme.getBoolean(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_STRING, googlePoiApiKey.getString(BACKUP_TYPE));
        assertEquals(GOOGLE_POI_API_KEY, googlePoiApiKey.getString(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_BOOLEAN, googlePoiApiKeyValid.getString(BACKUP_TYPE));
        assertTrue(googlePoiApiKeyValid.getBoolean(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_BOOLEAN, googlePoiSearchEnabled.getString(BACKUP_TYPE));
        assertFalse(googlePoiSearchEnabled.getBoolean(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_BOOLEAN, androidAutoIntegrationEnabled.getString(BACKUP_TYPE));
        assertFalse(androidAutoIntegrationEnabled.getBoolean(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_STRING, maneuverVoiceName.getString(BACKUP_TYPE));
        assertEquals(AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT, maneuverVoiceName.getString(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_BOOLEAN, mapPoiCategoryFilterEnabled.getString(BACKUP_TYPE));
        assertTrue(mapPoiCategoryFilterEnabled.getBoolean(BACKUP_VALUE));
        assertEquals(BACKUP_TYPE_STRING, mapPoiCategoryNames.getString(BACKUP_TYPE));
        JSONArray categories = new JSONArray(mapPoiCategoryNames.getString(BACKUP_VALUE));
        assertEquals(CATEGORY_FUEL, categories.getJSONObject(0).getString("name"));
        assertTrue(categories.getJSONObject(0).getBoolean("enabled"));
        assertEquals(CATEGORY_RESTAURANT, categories.getJSONObject(1).getString("name"));
        assertFalse(categories.getJSONObject(1).getBoolean("enabled"));
    }

    @Test
    public void importJson_acceptsOlderBackupWithoutSavedRoutes() throws Exception {
        JSONObject root = new JSONObject(AppDataBackup.exportJson(context));
        root.getJSONObject(KEY_SHARED_PREFERENCES).remove(PREFS_SAVED_ROUTES);
        context.getSharedPreferences(PREFS_SAVED_ROUTES, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_ROUTE_ITEMS, SAVED_ROUTE_PAYLOAD)
                .commit();

        AppDataBackup.importJson(context, root.toString());

        assertFalse(context.getSharedPreferences(PREFS_SAVED_ROUTES, Context.MODE_PRIVATE)
                .contains(KEY_SAVED_ROUTE_ITEMS));
    }

    @Test
    public void importJson_rejectsInvalidPayloadWithoutChangingExistingData() {
        assertImportRejectedWithoutChangingExistingData("{\"schemaVersion\":999,\"sharedPreferences\":{}}");
    }

    @Test
    public void importJson_rejectsMissingPreferenceFileWithoutChangingExistingData() throws Exception {
        JSONObject root = new JSONObject(AppDataBackup.exportJson(context));
        root.getJSONObject("sharedPreferences").remove(PREFS[1]);

        assertImportRejectedWithoutChangingExistingData(root.toString());
    }

    @Test
    public void importJson_rejectsNonStringStringSetEntryWithoutChangingExistingData() throws Exception {
        JSONObject root = new JSONObject(AppDataBackup.exportJson(context));
        JSONObject appSettings = root.getJSONObject(KEY_SHARED_PREFERENCES)
                .getJSONObject(PREFS[0]);
        appSettings.put(
                "bad_string_set",
                new JSONObject()
                        .put(BACKUP_TYPE, BACKUP_TYPE_STRING_SET)
                        .put(BACKUP_VALUE, new JSONArray().put("valid").put(7))
        );

        assertImportRejectedWithoutChangingExistingData(root.toString());
    }

    @Test
    public void importJson_rollsBackAlreadyReplacedPreferencesWhenWriteFails() throws Exception {
        Poi backupPoi = new Poi("Backup", 48.2082d, 16.3738d);
        AppSettings.setImperialUnitsEnabled(context, false);
        new PoiHistoryStore(context).addOrPromote(backupPoi);
        String backupJson = AppDataBackup.exportJson(context);

        clearPrefs();
        Poi existingPoi = new Poi("Existing", 45.4642d, 9.19d);
        AppSettings.setImperialUnitsEnabled(context, true);
        new PoiHistoryStore(context).addOrPromote(existingPoi);

        try {
            AppDataBackup.importJson(new OneShotFailingCommitContext(context, PREFS[1]), backupJson);
        } catch (JSONException expected) {
            assertTrue(AppSettings.isImperialUnitsEnabled(context));
            List<Poi> restoredPois = new PoiHistoryStore(context).list();
            assertEquals(1, restoredPois.size());
            assertEquals(existingPoi.name, restoredPois.get(0).name);
            return;
        }

        throw new AssertionError("Expected failed preference write to reject import");
    }

    private void clearPrefs() {
        for (String prefsName : PREFS) {
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit();
        }
    }

    private void assertImportRejectedWithoutChangingExistingData(String json) {
        AppSettings.setImperialUnitsEnabled(context, true);

        try {
            AppDataBackup.importJson(context, json);
        } catch (JSONException expected) {
            assertTrue(AppSettings.isImperialUnitsEnabled(context));
            return;
        }

        throw new AssertionError("Expected invalid backup to be rejected");
    }

    private static void assertBooleanPreference(
            JSONObject preferences,
            String key,
            boolean expectedValue
    ) throws JSONException {
        JSONObject preference = preferences.getJSONObject(key);
        assertEquals(BACKUP_TYPE_BOOLEAN, preference.getString(BACKUP_TYPE));
        assertEquals(expectedValue, preference.getBoolean(BACKUP_VALUE));
    }

    private static final class OneShotFailingCommitContext extends ContextWrapper {
        private final String failingPrefsName;
        private boolean failed;

        OneShotFailingCommitContext(Context base, String failingPrefsName) {
            super(base);
            this.failingPrefsName = failingPrefsName;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            SharedPreferences preferences = super.getSharedPreferences(name, mode);
            return failingPrefsName.equals(name)
                    ? new OneShotFailingSharedPreferences(preferences, this)
                    : preferences;
        }

        boolean shouldFailCommit() {
            if (failed) {
                return false;
            }
            failed = true;
            return true;
        }
    }

    private static final class OneShotFailingSharedPreferences implements SharedPreferences {
        private final SharedPreferences delegate;
        private final OneShotFailingCommitContext context;

        OneShotFailingSharedPreferences(
                SharedPreferences delegate,
                OneShotFailingCommitContext context
        ) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public Map<String, ?> getAll() {
            return delegate.getAll();
        }

        @Override
        public String getString(String key, String defValue) {
            return delegate.getString(key, defValue);
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return delegate.getStringSet(key, defValues);
        }

        @Override
        public int getInt(String key, int defValue) {
            return delegate.getInt(key, defValue);
        }

        @Override
        public long getLong(String key, long defValue) {
            return delegate.getLong(key, defValue);
        }

        @Override
        public float getFloat(String key, float defValue) {
            return delegate.getFloat(key, defValue);
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return delegate.getBoolean(key, defValue);
        }

        @Override
        public boolean contains(String key) {
            return delegate.contains(key);
        }

        @Override
        public Editor edit() {
            return new OneShotFailingEditor(delegate.edit(), context);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            delegate.registerOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            delegate.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    private static final class OneShotFailingEditor implements SharedPreferences.Editor {
        private final SharedPreferences.Editor delegate;
        private final OneShotFailingCommitContext context;

        OneShotFailingEditor(
                SharedPreferences.Editor delegate,
                OneShotFailingCommitContext context
        ) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            delegate.putString(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            delegate.putStringSet(key, values);
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            delegate.putInt(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            delegate.putLong(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            delegate.putFloat(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            delegate.putBoolean(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            delegate.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            delegate.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return !context.shouldFailCommit() && delegate.commit();
        }

        @Override
        public void apply() {
            if (!context.shouldFailCommit()) {
                delegate.apply();
            }
        }
    }
}
