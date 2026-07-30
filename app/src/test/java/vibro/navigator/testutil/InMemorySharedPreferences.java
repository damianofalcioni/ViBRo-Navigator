package vibro.navigator.testutil;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class InMemorySharedPreferences implements SharedPreferences {
    private final Map<String, Object> values = new LinkedHashMap<>();

    @NonNull
    @Override
    public Map<String, ?> getAll() {
        synchronized (values) {
            return new LinkedHashMap<>(values);
        }
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        Object value = valueFor(key);
        return value instanceof String ? (String) value : defValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        Object value = valueFor(key);
        if (value instanceof Set<?>) {
            Set<String> strings = new HashSet<>();
            for (Object item : (Set<?>) value) {
                if (!(item instanceof String)) {
                    return defValues;
                }
                strings.add((String) item);
            }
            return strings;
        }
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = valueFor(key);
        return value instanceof Integer ? (Integer) value : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = valueFor(key);
        return value instanceof Long ? (Long) value : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = valueFor(key);
        return value instanceof Float ? (Float) value : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = valueFor(key);
        return value instanceof Boolean ? (Boolean) value : defValue;
    }

    @Override
    public boolean contains(String key) {
        synchronized (values) {
            return values.containsKey(key);
        }
    }

    @NonNull
    @Override
    public Editor edit() {
        return new EditorImpl();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    @Nullable
    private Object valueFor(String key) {
        synchronized (values) {
            return values.get(key);
        }
    }

    private final class EditorImpl implements Editor {
        private final Map<String, Object> writes = new HashMap<>();
        private final Set<String> removals = new HashSet<>();
        private boolean clear;

        @NonNull
        @Override
        public Editor putString(String key, @Nullable String value) {
            if (value == null) {
                return remove(key);
            }
            writes.put(key, value);
            removals.remove(key);
            return this;
        }

        @NonNull
        @Override
        public Editor putStringSet(String key, @Nullable Set<String> value) {
            if (value == null) {
                return remove(key);
            }
            writes.put(key, new HashSet<>(value));
            removals.remove(key);
            return this;
        }

        @NonNull
        @Override
        public Editor putInt(String key, int value) {
            writes.put(key, value);
            removals.remove(key);
            return this;
        }

        @NonNull
        @Override
        public Editor putLong(String key, long value) {
            writes.put(key, value);
            removals.remove(key);
            return this;
        }

        @NonNull
        @Override
        public Editor putFloat(String key, float value) {
            writes.put(key, value);
            removals.remove(key);
            return this;
        }

        @NonNull
        @Override
        public Editor putBoolean(String key, boolean value) {
            writes.put(key, value);
            removals.remove(key);
            return this;
        }

        @NonNull
        @Override
        public Editor remove(String key) {
            writes.remove(key);
            removals.add(key);
            return this;
        }

        @NonNull
        @Override
        public Editor clear() {
            clear = true;
            writes.clear();
            removals.clear();
            return this;
        }

        @Override
        public boolean commit() {
            synchronized (values) {
                if (clear) {
                    values.clear();
                }
                for (String key : removals) {
                    values.remove(key);
                }
                values.putAll(writes);
            }
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
