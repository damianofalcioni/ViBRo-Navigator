package vibro.navigator.brouter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.util.AppLogger;

import java.util.List;

public class BRouterProfilesRepository {

    public static final String BROUTER_PACKAGE_NAME = "btools.routingapp";

    private static final String PREFS = "vibenavigator_brouter";
    private static final String KEY_CUSTOM_PROFILE_URI = "custom_profile_uri";
    private static final String KEY_CUSTOM_PROFILE_NAME = "custom_profile_name";
    private static final String KEY_PROFILES_TREE_URI = "profiles_tree_uri";
    private static final String KEY_SELECTED_PROFILE = "selected_profile";
    private static final String TAG = "BRouterProfiles";

    private final BRouterProfileDirectories profileDirectories = new BRouterProfileDirectories();
    private final BRouterProfileLister profileLister;

    public BRouterProfilesRepository() {
        this(new BRouterProfileLister());
    }

    BRouterProfilesRepository(@NonNull BRouterProfileLister profileLister) {
        this.profileLister = profileLister;
    }

    @Nullable
    public Uri getCustomProfileUri(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_CUSTOM_PROFILE_URI, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Uri.parse(raw);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to parse saved custom profile URI raw=" + raw, e);
            return null;
        }
    }

    @Nullable
    public String getCustomProfileName(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_CUSTOM_PROFILE_NAME, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return raw.trim();
    }

    public void saveCustomProfile(@NonNull Context context, @NonNull Uri documentUri, @NonNull String profileName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CUSTOM_PROFILE_URI, documentUri.toString())
                .putString(KEY_CUSTOM_PROFILE_NAME, profileName)
                .apply();
        AppLogger.i(TAG, "Saved custom profile uri=" + documentUri + " profile=" + profileName);
    }

    @Nullable
    public Uri getProfilesTreeUri(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_PROFILES_TREE_URI, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Uri.parse(raw);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to parse saved profiles tree URI raw=" + raw, e);
            return null;
        }
    }

    public void saveProfilesTreeUri(@NonNull Context context, @NonNull Uri treeUri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_PROFILES_TREE_URI, treeUri.toString())
                .apply();
        AppLogger.i(TAG, "Saved profiles tree uri=" + treeUri);
    }

    @Nullable
    public String getSelectedProfileKey(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SELECTED_PROFILE, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return raw.trim();
    }

    public void saveSelectedProfileKey(@NonNull Context context, @NonNull String selectionKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SELECTED_PROFILE, selectionKey).apply();
        AppLogger.i(TAG, "Saved selected profile key=" + selectionKey);
    }

    @Nullable
    public Uri getCustomProfilePickerInitialUri(@NonNull Context context) {
        Uri profilesTreeUri = getProfilesTreeUri(context);
        return profileDirectories.getCustomProfilePickerInitialUri(
                context,
                profilesTreeUri,
                hasPersistedReadPermission(context, profilesTreeUri),
                getCustomProfileUri(context)
        );
    }

    public boolean hasPersistedProfilesTreeAccess(@NonNull Context context) {
        return hasPersistedReadPermission(context, getProfilesTreeUri(context));
    }

    public boolean isBRouterInstalled(@NonNull Context context) {
        try {
            context.getPackageManager().getPackageInfo(BROUTER_PACKAGE_NAME, 0);
            AppLogger.d(TAG, "BRouter package detected");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            AppLogger.w(TAG, "BRouter package not installed");
            return false;
        }
    }

    @NonNull
    public List<String> listProfiles(@NonNull Context context) {
        return profileLister.listProfiles(context, resolveProfilesDiscoveryTreeUris(context));
    }

    @NonNull
    private List<Uri> resolveProfilesDiscoveryTreeUris(@NonNull Context context) {
        Uri savedTreeUri = getProfilesTreeUri(context);
        return profileDirectories.resolveProfilesDiscoveryTreeUris(
                context,
                savedTreeUri,
                hasPersistedReadPermission(context, savedTreeUri)
        );
    }

    @Nullable
    public String normalizeProfileName(@Nullable String rawName) {
        return profileLister.normalizeProfileName(rawName);
    }

    private boolean hasPersistedReadPermission(@NonNull Context context, @Nullable Uri uri) {
        if (uri == null) {
            return false;
        }
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (!permission.isReadPermission()) {
                continue;
            }
            if (uri.equals(permission.getUri())) {
                return true;
            }
        }
        return false;
    }
}
