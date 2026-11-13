package vibro.navigator.android.brouter;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;

final class AndroidBRouterFolderOffers {
    private static final String OFFERED = "brouter_offered_uris";
    private static final String CURRENT = "brouter_foreground_offer";
    private final ArrayList<String> folders;
    private String current;

    AndroidBRouterFolderOffers(@Nullable Bundle state) {
        ArrayList<String> saved = state == null ? null : state.getStringArrayList(OFFERED);
        folders = saved == null ? new ArrayList<>() : new ArrayList<>(saved);
        current = state == null ? null : state.getString(CURRENT);
    }

    void begin(@Nullable Uri uri) {
        current = uri == null ? null : uri.toString();
    }

    void record(@Nullable Uri uri) {
        if (uri != null && !folders.contains(uri.toString())) {
            folders.add(uri.toString());
        }
    }

    void forgetCurrent() {
        folders.remove(current);
        current = null;
    }

    ArrayList<String> copy() {
        return new ArrayList<>(folders);
    }

    void save(Bundle state) {
        state.putStringArrayList(OFFERED, new ArrayList<>(folders));
        state.putString(CURRENT, current);
    }
}
