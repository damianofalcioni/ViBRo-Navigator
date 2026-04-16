package vibro.navigator.brouter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
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

public final class BRouterProfilesRepository {

    public static final String BROUTER_PACKAGE_NAME = "btools.routingapp";

    private static final String PREFS = "vibenavigator_brouter";
    private static final String KEY_PROFILES_TREE_URI = "profiles_tree_uri";
    private static final String KEY_CUSTOM_PROFILE_URI = "custom_profile_uri";
    private static final String KEY_CUSTOM_PROFILE_NAME = "custom_profile_name";
    private static final String KEY_SELECTED_PROFILE = "selected_profile";
    private static final String BROUTER_PROFILES_ZIP = "assets/profiles2.zip";
    private static final String EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents";
    private static final String MEDIA_PROFILES_RELATIVE_DOCUMENT_PATH =
            "Android/media/btools.routingapp/brouter/profiles2";
    private static final String LEGACY_DATA_PROFILES_RELATIVE_DOCUMENT_PATH =
            "Android/data/btools.routingapp/files/brouter/profiles2";
    private static final String TAG = "BRouterProfiles";

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
            AppLogger.w(TAG, "Failed to parse saved profiles URI raw=" + raw, e);
            return null;
        }
    }

    public void saveProfilesTreeUri(@NonNull Context context, @NonNull Uri treeUri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PROFILES_TREE_URI, treeUri.toString()).apply();
        AppLogger.i(TAG, "Saved profiles tree URI=" + treeUri);
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

    @NonNull
    public Uri getProfilesFolderPickerInitialUri(@NonNull Context context) {
        Uri treeUri = getProfilesTreeUri(context);
        if (treeUri != null) {
            return treeUri;
        }
        Uri customUri = getCustomProfileUri(context);
        if (customUri != null) {
            return customUri;
        }
        String documentId = resolveExistingProfilesDocumentId(context);
        return DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
                documentId
        );
    }

    @NonNull
    public Uri getCustomProfilePickerInitialUri(@NonNull Context context) {
        Uri customUri = getCustomProfileUri(context);
        if (customUri != null) {
            Uri parentUri = toParentDocumentUri(customUri);
            if (parentUri != null) {
                return parentUri;
            }
            return customUri;
        }
        Uri treeUri = getProfilesTreeUri(context);
        if (treeUri != null) {
            return DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
            );
        }
        String documentId = resolveExistingProfilesDocumentId(context);
        Uri defaultTreeUri = DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
                documentId
        );
        return DocumentsContract.buildDocumentUriUsingTree(
                defaultTreeUri,
                documentId
        );
    }

    @Nullable
    private Uri toParentDocumentUri(@NonNull Uri documentUri) {
        try {
            if (!EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY.equals(documentUri.getAuthority())) {
                return null;
            }
            String documentId = DocumentsContract.getDocumentId(documentUri);
            int slash = documentId.lastIndexOf('/');
            if (slash <= 0) {
                return null;
            }
            String parentDocumentId = documentId.substring(0, slash);
            return DocumentsContract.buildDocumentUri(
                    EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
                    parentDocumentId
            );
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to derive parent document URI from custom profile uri=" + documentUri, e);
            return null;
        }
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
        Uri treeUri = getProfilesTreeUri(context);
        Set<String> out = new TreeSet<>();
        int externalCount = 0;
        if (treeUri != null) {
            List<String> external = listProfilesFromTree(context, treeUri);
            externalCount = external.size();
            out.addAll(external);
        }
        List<String> bundled = listBundledProfiles(context);
        out.addAll(bundled);
        AppLogger.i(TAG, "Listed profiles total=" + out.size()
                + " external=" + externalCount
                + " bundled=" + bundled.size());
        return new ArrayList<>(out);
    }

    @NonNull
    private List<String> listProfilesFromTree(@NonNull Context context, @NonNull Uri treeUri) {
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
    private List<String> listBundledProfiles(@NonNull Context context) {
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

    @NonNull
    private String resolveExistingProfilesDocumentId(@NonNull Context context) {
        for (String documentId : getProfilesDocumentIdCandidates(context)) {
            if (documentExists(context, documentId)) {
                AppLogger.d(TAG, "Using detected profiles path documentId=" + documentId);
                return documentId;
            }
        }
        String fallback = getFallbackProfilesDocumentId();
        AppLogger.d(TAG, "Using fallback profiles path documentId=" + fallback);
        return fallback;
    }

    @NonNull
    private List<String> getProfilesDocumentIdCandidates(@NonNull Context context) {
        List<String> candidates = new ArrayList<>();
        addDocumentIdCandidate(candidates, "primary", MEDIA_PROFILES_RELATIVE_DOCUMENT_PATH);
        addDocumentIdCandidate(candidates, "primary", LEGACY_DATA_PROFILES_RELATIVE_DOCUMENT_PATH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StorageManager storageManager = context.getSystemService(StorageManager.class);
            if (storageManager != null) {
                for (StorageVolume volume : storageManager.getStorageVolumes()) {
                    String uuid = volume.getUuid();
                    if (uuid == null || uuid.trim().isEmpty()) {
                        continue;
                    }
                    addDocumentIdCandidate(candidates, uuid, MEDIA_PROFILES_RELATIVE_DOCUMENT_PATH);
                    addDocumentIdCandidate(candidates, uuid, LEGACY_DATA_PROFILES_RELATIVE_DOCUMENT_PATH);
                }
            }
        }
        return candidates;
    }

    private void addDocumentIdCandidate(
            @NonNull List<String> candidates,
            @NonNull String rootId,
            @NonNull String relativePath
    ) {
        String documentId = rootId + ":" + relativePath;
        if (!candidates.contains(documentId)) {
            candidates.add(documentId);
        }
    }

    private boolean documentExists(@NonNull Context context, @NonNull String documentId) {
        Uri treeUri = DocumentsContract.buildTreeDocumentUri(EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY, documentId);
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        try (Cursor cursor = context.getContentResolver().query(
                documentUri,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                null,
                null,
                null
        )) {
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            AppLogger.d(TAG, "Profiles path not accessible documentId=" + documentId + " error=" + e.getClass().getSimpleName());
            return false;
        }
    }

    @NonNull
    private String getFallbackProfilesDocumentId() {
        String relativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? MEDIA_PROFILES_RELATIVE_DOCUMENT_PATH
                : LEGACY_DATA_PROFILES_RELATIVE_DOCUMENT_PATH;
        return "primary:" + relativePath;
    }
}
