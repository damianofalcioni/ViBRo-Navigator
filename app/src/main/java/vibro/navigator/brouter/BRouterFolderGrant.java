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
    private static final String PROFILES_TREE = "brouter_profiles_tree_uri";
    private static final String SEGMENTS_TREE = "brouter_segments_tree_uri";

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

    @Nullable
    public static Uri parentFor(Context context, boolean profiles) {
        if (directory(context, profiles) == null) {
            return null;
        }
        Uri parent = readUri(context, profiles ? PROFILES_TREE : SEGMENTS_TREE);
        return parent == null ? get(context) : parent;
    }

    @Nullable
    private static Uri readUri(Context context, String key) {
        String value = BRouterSegmentSourceSettings.prefs(context).getString(key, null);
        return value == null ? null : Uri.parse(value);
    }

    public static void save(Context context, Uri parent, @Nullable Uri profiles, @Nullable Uri segments) {
        Uri profileSource = profiles == null ? parentFor(context, true) : parent;
        Uri segmentSource = segments == null ? parentFor(context, false) : parent;
        var editor = BRouterSegmentSourceSettings.prefs(context).edit().putString(TREE_URI, parent.toString());
        editor.putString(PROFILES_TREE, profileSource == null ? null : profileSource.toString());
        editor.putString(SEGMENTS_TREE, segmentSource == null ? null : segmentSource.toString());
        if (profiles != null) {
            editor.putString(PROFILES_URI, profiles.toString());
        }
        if (segments != null) {
            editor.putString(SEGMENTS_URI, segments.toString());
        }
        BRouterSegmentSourceSettings.applySourceChange(editor);
    }

    @Nullable
    static Uri resolve(Context context, @Nullable Uri legacy, boolean profiles, GrantCheck check) {
        Uri shared = get(context);
        if (shared != null && check.isGranted(context, shared)) {
            return shared;
        }
        Uri previous = parentFor(context, profiles);
        if (previous != null && !previous.equals(shared) && check.isGranted(context, previous)) {
            return previous;
        }
        return check.isGranted(context, legacy) ? legacy : null;
    }

    static List<Uri> discoveryTrees(Context context, @Nullable Uri legacy, boolean profiles, GrantCheck check) {
        List<Uri> trees = new ArrayList<>();
        Uri primary = resolve(context, legacy, profiles, check);
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
