package vibro.navigator.android.brouter;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import static vibro.navigator.android.brouter.AndroidBRouterStorageSetup.REQUEST_PROFILES;

final class AndroidBRouterFolderOffers {
    private static final String PROFILES = "brouter_profiles_offered_uris";
    private static final String SEGMENTS = "brouter_segments_offered_uris";
    private static final String CURRENT = "brouter_foreground_offer";
    private final ArrayList<String> profiles;
    private final ArrayList<String> segments;
    private String current;

    AndroidBRouterFolderOffers(@Nullable Bundle state) {
        profiles = read(state, PROFILES);
        segments = read(state, SEGMENTS);
        current = state == null ? null : state.getString(CURRENT);
    }

    void begin(@Nullable Uri uri) {
        current = uri == null ? null : uri.toString();
    }

    void record(int requestCode, @Nullable Uri uri) {
        if (uri != null && !entries(requestCode).contains(uri.toString())) {
            entries(requestCode).add(uri.toString());
        }
    }

    void forgetCurrent(int requestCode) {
        entries(requestCode).remove(current);
        current = null;
    }

    ArrayList<String> copy(int requestCode) {
        return new ArrayList<>(entries(requestCode));
    }

    void save(Bundle state) {
        state.putStringArrayList(PROFILES, new ArrayList<>(profiles));
        state.putStringArrayList(SEGMENTS, new ArrayList<>(segments));
        state.putString(CURRENT, current);
    }

    private ArrayList<String> entries(int requestCode) {
        return requestCode == REQUEST_PROFILES ? profiles : segments;
    }

    private static ArrayList<String> read(@Nullable Bundle state, String key) {
        ArrayList<String> saved = state == null ? null : state.getStringArrayList(key);
        return saved == null ? new ArrayList<>() : new ArrayList<>(saved);
    }
}
