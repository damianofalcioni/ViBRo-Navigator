package vibro.navigator.settings;

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
    static final String[] BACKED_UP_PREFERENCES = {
            "vibro.navigator.settings",
            "vibenavigator_poi_history",
            "vibenavigator_brouter",
            "app_logging"
    };

    private AppDataBackupContract() {
    }
}
