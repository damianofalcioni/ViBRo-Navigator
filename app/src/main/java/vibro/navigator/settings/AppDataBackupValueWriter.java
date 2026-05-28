package vibro.navigator.settings;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

final class AppDataBackupValueWriter {

    private AppDataBackupValueWriter() {
    }

    static void put(
            @NonNull SharedPreferences.Editor editor,
            @NonNull AppDataBackupPreferenceValue preferenceValue
    ) {
        Object raw = preferenceValue.value;
        String key = preferenceValue.key;
        if (raw instanceof String) {
            editor.putString(key, (String) raw);
        } else if (raw instanceof Boolean) {
            editor.putBoolean(key, (Boolean) raw);
        } else if (raw instanceof Integer) {
            editor.putInt(key, (Integer) raw);
        } else if (raw instanceof Long) {
            editor.putLong(key, (Long) raw);
        } else if (raw instanceof Float) {
            editor.putFloat(key, (Float) raw);
        } else if (raw instanceof Set<?>) {
            editor.putStringSet(key, stringSet((Set<?>) raw));
        }
    }

    @NonNull
    private static Set<String> stringSet(@NonNull Set<?> rawValues) {
        Set<String> values = new HashSet<>();
        for (Object value : rawValues) {
            if (value instanceof String) {
                values.add((String) value);
            }
        }
        return values;
    }
}
