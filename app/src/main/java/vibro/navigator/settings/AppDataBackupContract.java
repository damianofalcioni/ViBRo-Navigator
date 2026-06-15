package vibro.navigator.settings;

import androidx.annotation.NonNull;

final class AppDataBackupContract {

    static final int SCHEMA_VERSION = 1;
    static final String KEY_SCHEMA_VERSION = "schemaVersion";
    static final String KEY_SHARED_PREFERENCES = "sharedPreferences";
    static final String KEY_TYPE = "type";
    static final String KEY_VALUE = "value";
    static final String TYPE_STRING = "string";
    static final String TYPE_BOOLEAN = "boolean";
    static final String TYPE_INTEGER = "integer";
    static final String TYPE_LONG = "long";
    static final String TYPE_FLOAT = "float";
    static final String TYPE_STRING_SET = "stringSet";
    private static final String PREFS_APP_SETTINGS = "vibro.navigator.settings";
    private static final String PREFS_POI_HISTORY = "vibenavigator_poi_history";
    private static final String PREFS_BROUTER = "vibenavigator_brouter";
    private static final String PREFS_LOGGING = "app_logging";
    private static final String PREFS_SAVED_ROUTES = "vibenavigator_saved_routes";
    static final String[] OPTIONAL_BACKED_UP_PREFERENCES = {
            PREFS_SAVED_ROUTES
    };
    static final String[] BACKED_UP_PREFERENCES = {
            PREFS_APP_SETTINGS,
            PREFS_POI_HISTORY,
            PREFS_BROUTER,
            PREFS_LOGGING,
            PREFS_SAVED_ROUTES
    };

    private AppDataBackupContract() {
    }

    static boolean isOptionalPreference(@NonNull String prefsName) {
        for (String optionalPrefsName : OPTIONAL_BACKED_UP_PREFERENCES) {
            if (optionalPrefsName.equals(prefsName)) {
                return true;
            }
        }
        return false;
    }
}
