package vibro.navigator.brouter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

public final class BRouterSegmentSourceSettings {
    private static final String PREFS = "vibenavigator_brouter";
    private static final String TREE_URI = "segments_tree_uri";
    private static final String REVISION = "segments_source_revision";

    private BRouterSegmentSourceSettings() {
    }

    @Nullable
    static Uri getTreeUri(Context context) {
        String raw = prefs(context).getString(TREE_URI, null);
        return raw == null || raw.trim().isEmpty() ? null : Uri.parse(raw);
    }

    @Nullable
    static String getRevision(Context context) {
        return prefs(context).getString(REVISION, null);
    }

    static void save(Context context, Uri uri) {
        saveSource(context, TREE_URI, uri);
    }

    static void saveSource(Context context, String key, Uri uri) {
        prefs(context).edit()
                .putString(key, uri.toString())
                .putString(REVISION, UUID.randomUUID().toString())
                .apply();
    }

    @NonNull
    public static Runnable observe(Context context, Runnable onChanged) {
        SharedPreferences prefs = prefs(context);
        SharedPreferences.OnSharedPreferenceChangeListener listener = (preferences, key) -> {
            if (key == null || REVISION.equals(key)) {
                onChanged.run();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
        return () -> prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
