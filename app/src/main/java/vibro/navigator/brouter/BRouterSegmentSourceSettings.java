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
        applySourceChange(prefs(context).edit().putString(TREE_URI, uri.toString()));
    }

    static void applySourceChange(SharedPreferences.Editor editor) {
        editor.putString(REVISION, UUID.randomUUID().toString()).apply();
    }

    @NonNull
    public static Runnable observe(Context context, Runnable onChanged) {
        return observeKey(context, REVISION, onChanged);
    }

    static Runnable observeKey(Context context, String revision, Runnable onChanged) {
        SharedPreferences prefs = prefs(context);
        SharedPreferences.OnSharedPreferenceChangeListener listener = (preferences, key) -> {
            if (key == null || revision.equals(key)) {
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
