package vibro.navigator.settings;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

final class AppDataBackupValueJson {

    private AppDataBackupValueJson() {
    }

    @NonNull
    static JSONObject toJson(@NonNull Object value) throws JSONException {
        String type = typeFor(value);
        JSONObject out = new JSONObject();
        out.put(AppDataBackupContract.KEY_TYPE, type);
        out.put(AppDataBackupContract.KEY_VALUE, exportedValue(type, value));
        return out;
    }

    @NonNull
    static AppDataBackupPreferenceValue fromJson(@NonNull String key, @NonNull JSONObject rawValue)
            throws JSONException {
        String type = rawValue.getString(AppDataBackupContract.KEY_TYPE);
        return new AppDataBackupPreferenceValue(key, importedValue(type, rawValue));
    }

    @NonNull
    private static String typeFor(@NonNull Object value) throws JSONException {
        if (value instanceof String) {
            return AppDataBackupContract.TYPE_STRING;
        } else if (value instanceof Boolean) {
            return AppDataBackupContract.TYPE_BOOLEAN;
        } else if (value instanceof Integer) {
            return AppDataBackupContract.TYPE_INTEGER;
        }
        return extendedTypeFor(value);
    }

    @NonNull
    private static String extendedTypeFor(@NonNull Object value) throws JSONException {
        if (value instanceof Long) {
            return AppDataBackupContract.TYPE_LONG;
        } else if (value instanceof Float) {
            return AppDataBackupContract.TYPE_FLOAT;
        } else if (value instanceof Set<?>) {
            return AppDataBackupContract.TYPE_STRING_SET;
        }
        throw new JSONException("Unsupported preference value type " + value.getClass().getName());
    }

    @NonNull
    private static Object exportedValue(@NonNull String type, @NonNull Object value) {
        if (AppDataBackupContract.TYPE_FLOAT.equals(type)) {
            return ((Float) value).doubleValue();
        }
        if (AppDataBackupContract.TYPE_STRING_SET.equals(type)) {
            return stringSetToJson((Set<?>) value);
        }
        return value;
    }

    @NonNull
    private static Object importedValue(@NonNull String type, @NonNull JSONObject rawValue) throws JSONException {
        if (isSimpleType(type)) {
            return simpleImportedValue(type, rawValue);
        }
        return extendedImportedValue(type, rawValue);
    }

    private static boolean isSimpleType(@NonNull String type) {
        return AppDataBackupContract.TYPE_STRING.equals(type)
                || AppDataBackupContract.TYPE_BOOLEAN.equals(type)
                || AppDataBackupContract.TYPE_INTEGER.equals(type);
    }

    @NonNull
    private static Object simpleImportedValue(@NonNull String type, @NonNull JSONObject rawValue)
            throws JSONException {
        switch (type) {
            case AppDataBackupContract.TYPE_STRING:
                return rawValue.getString(AppDataBackupContract.KEY_VALUE);
            case AppDataBackupContract.TYPE_BOOLEAN:
                return rawValue.getBoolean(AppDataBackupContract.KEY_VALUE);
            case AppDataBackupContract.TYPE_INTEGER:
                return rawValue.getInt(AppDataBackupContract.KEY_VALUE);
            default:
                throw new JSONException("Unsupported preference value type " + type);
        }
    }

    @NonNull
    private static Object extendedImportedValue(@NonNull String type, @NonNull JSONObject rawValue)
            throws JSONException {
        switch (type) {
            case AppDataBackupContract.TYPE_LONG:
                return rawValue.getLong(AppDataBackupContract.KEY_VALUE);
            case AppDataBackupContract.TYPE_FLOAT:
                return (float) rawValue.getDouble(AppDataBackupContract.KEY_VALUE);
            case AppDataBackupContract.TYPE_STRING_SET:
                return jsonToStringSet(rawValue.getJSONArray(AppDataBackupContract.KEY_VALUE));
            default:
                throw new JSONException("Unsupported preference value type " + type);
        }
    }

    @NonNull
    private static JSONArray stringSetToJson(@NonNull Set<?> values) {
        JSONArray array = new JSONArray();
        for (Object value : values) {
            if (value instanceof String) {
                array.put(value);
            }
        }
        return array;
    }

    @NonNull
    private static Set<String> jsonToStringSet(@NonNull JSONArray array) {
        Set<String> values = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, null);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }
}
