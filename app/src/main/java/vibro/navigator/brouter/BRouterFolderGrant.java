package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The read grant shared by profiles and routing segments. */
public final class BRouterFolderGrant {
    private static final String TREE_URI = "brouter_tree_uri";
    private static final String PROFILES_URI = "brouter_profiles_uri";
    private static final String SEGMENTS_URI = "brouter_segments_uri";
    private static final String VERIFIED = "brouter_directories_verified";

    private BRouterFolderGrant() {
    }

    @Nullable
    public static Uri get(@NonNull Context context) {
        return readUri(context, TREE_URI);
    }

    @Nullable
    public static Uri directory(Context context, boolean profiles) {
        return readUri(context, profiles ? PROFILES_URI : SEGMENTS_URI);
    }

    public static boolean directoriesVerified(Context context) {
        return BRouterSegmentSourceSettings.prefs(context).getBoolean(VERIFIED, false);
    }

    @Nullable
    private static Uri readUri(Context context, String key) {
        String value = BRouterSegmentSourceSettings.prefs(context).getString(key, null);
        return value == null ? null : Uri.parse(value);
    }

    public static void save(Context context, Uri parent, @Nullable Uri profiles, @Nullable Uri segments) {
        BRouterSegmentSourceSettings.applySourceChange(BRouterSegmentSourceSettings.prefs(context).edit()
                .putString(TREE_URI, parent.toString())
                .putString(PROFILES_URI, profiles == null ? null : profiles.toString())
                .putString(SEGMENTS_URI, segments == null ? null : segments.toString())
                .putBoolean(VERIFIED, true));
    }

    @Nullable
    static Uri resolve(Context context, @Nullable Uri legacy, GrantCheck check) {
        Uri shared = get(context);
        if (shared != null && check.isGranted(context, shared)) {
            return shared;
        }
        return check.isGranted(context, legacy) ? legacy : null;
    }

    static List<Uri> discoveryTrees(Context context, @Nullable Uri legacy, GrantCheck check) {
        List<Uri> trees = new ArrayList<>();
        Uri primary = resolve(context, legacy, check);
        if (primary != null) {
            trees.add(primary);
        }
        if (legacy != null && !legacy.equals(primary) && check.isGranted(context, legacy)) {
            trees.add(legacy);
        }
        return trees;
    }

    interface GrantCheck {
        boolean isGranted(Context context, @Nullable Uri uri);
    }
}
