package vibro.navigator.testutil;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SharedPreferencesTestContext extends ContextWrapper {
    @NonNull
    private final Map<String, SharedPreferences> preferencesByName = new LinkedHashMap<>();

    public SharedPreferencesTestContext() {
        super(null);
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        synchronized (preferencesByName) {
            SharedPreferences preferences = preferencesByName.get(name);
            if (preferences == null) {
                preferences = new InMemorySharedPreferences();
                preferencesByName.put(name, preferences);
            }
            return preferences;
        }
    }
}
