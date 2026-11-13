package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** The read grant shared by profiles and routing segments. */
public final class BRouterFolderGrant {
    private static final String TREE_URI = "brouter_tree_uri";

    private BRouterFolderGrant() {
    }

    @Nullable
    public static Uri get(@NonNull Context context) {
        String value = BRouterSegmentSourceSettings.prefs(context).getString(TREE_URI, null);
        return value == null ? null : Uri.parse(value);
    }

    public static void save(@NonNull Context context, @NonNull Uri uri) {
        BRouterSegmentSourceSettings.saveSource(context, TREE_URI, uri);
    }

    @Nullable
    static Uri resolve(Context context, @Nullable Uri legacy, GrantCheck check) {
        Uri shared = get(context);
        if (shared != null && check.isGranted(context, shared)) {
            return shared;
        }
        return check.isGranted(context, legacy) ? legacy : null;
    }

    interface GrantCheck {
        boolean isGranted(Context context, @Nullable Uri uri);
    }
}
