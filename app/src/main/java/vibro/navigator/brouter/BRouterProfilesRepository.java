package vibro.navigator.brouter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.util.AppLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class BRouterProfilesRepository {

    public static final String BROUTER_PACKAGE_NAME = "btools.routingapp";

    private static final String PREFS = "vibenavigator_brouter";
    private static final String KEY_CUSTOM_PROFILE_URI = "custom_profile_uri";
    private static final String KEY_CUSTOM_PROFILE_NAME = "custom_profile_name";
    private static final String KEY_PROFILES_TREE_URI = "profiles_tree_uri";
    private static final String KEY_SELECTED_PROFILE = "selected_profile";
    private static final String BROUTER_PROFILES_ZIP = "assets/profiles2.zip";
    private static final String TAG = "BRouterProfiles";

    private final BRouterProfileDirectories profileDirectories = new BRouterProfileDirectories();

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
        Set<String> externalProfiles = new TreeSet<>();
        for (Uri treeUri : resolveProfilesDiscoveryTreeUris(context)) {
            externalProfiles.addAll(listProfilesFromTree(context, treeUri));
        }
        Set<String> out = new TreeSet<>(externalProfiles);
        List<String> bundled = listBundledProfiles(context);
        out.addAll(bundled);
        AppLogger.i(TAG, "Listed profiles total=" + out.size()
                + " external=" + externalProfiles.size()
                + " bundled=" + bundled.size());
        return new ArrayList<>(out);
    }

    @NonNull
    List<Uri> resolveProfilesDiscoveryTreeUris(@NonNull Context context) {
        Uri savedTreeUri = getProfilesTreeUri(context);
        return profileDirectories.resolveProfilesDiscoveryTreeUris(
                context,
                savedTreeUri,
                hasPersistedReadPermission(context, savedTreeUri)
        );
    }

    @NonNull
    List<String> listProfilesFromTree(@NonNull Context context, @NonNull Uri treeUri) {
        ContentResolver cr = context.getContentResolver();
        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getDocumentId(docUri));

        List<String> out = new ArrayList<>();
        try (Cursor c = cr.query(childrenUri,
                new String[]{
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null,
                null,
                null
        )) {
            if (c == null) {
                AppLogger.w(TAG, "Profiles tree query returned null cursor uri=" + treeUri);
                return Collections.emptyList();
            }
            int nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            while (c.moveToNext()) {
                addProfileName(out, c.getString(nameCol));
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to list profiles from tree uri=" + treeUri, e);
            return Collections.emptyList();
        }
        Collections.sort(out);
        AppLogger.i(TAG, "Listed external profiles uri=" + treeUri + " count=" + out.size());
        return out;
    }

    @NonNull
    List<String> listBundledProfiles(@NonNull Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(BROUTER_PACKAGE_NAME, 0);
            try (ZipFile apk = new ZipFile(appInfo.sourceDir)) {
                ZipEntry profilesZipEntry = apk.getEntry(BROUTER_PROFILES_ZIP);
                if (profilesZipEntry == null) {
                    AppLogger.w(TAG, "Bundled profiles zip missing inside BRouter APK");
                    return Collections.emptyList();
                }
                List<String> out = new ArrayList<>();
                try (InputStream raw = apk.getInputStream(profilesZipEntry);
                     ZipInputStream zip = new ZipInputStream(raw)) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            addProfileName(out, entry.getName());
                        }
                        zip.closeEntry();
                    }
                }
                Collections.sort(out);
                AppLogger.i(TAG, "Listed bundled BRouter profiles count=" + out.size());
                return out;
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read bundled BRouter profiles", e);
            return Collections.emptyList();
        }
    }

    private void addProfileName(@NonNull List<String> out, @Nullable String rawName) {
        String profileName = normalizeProfileName(rawName);
        if (profileName != null) {
            out.add(profileName);
        }
    }

    @Nullable
    public String normalizeProfileName(@Nullable String rawName) {
        if (rawName == null) {
            return null;
        }
        String name = rawName.trim();
        if (name.isEmpty()) {
            return null;
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < name.length()) {
            name = name.substring(slash + 1);
        }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".brf")) {
            return null;
        }
        String base = name.substring(0, name.length() - 4).trim();
        return base.isEmpty() ? null : base;
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
