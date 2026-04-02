package com.vibenavigator.brouter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class BRouterProfilesRepository {

    private static final String PREFS = "vibenavigator_brouter";
    private static final String KEY_PROFILES_TREE_URI = "profiles_tree_uri";
    private static final String BROUTER_PACKAGE = "btools.routingapp";
    private static final String BROUTER_PROFILES_ZIP = "assets/profiles2.zip";

    @Nullable
    public Uri getProfilesTreeUri(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_PROFILES_TREE_URI, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Uri.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void saveProfilesTreeUri(@NonNull Context context, @NonNull Uri treeUri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PROFILES_TREE_URI, treeUri.toString()).apply();
    }

    public boolean isBRouterInstalled(@NonNull Context context) {
        try {
            context.getPackageManager().getPackageInfo(BROUTER_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @NonNull
    public List<String> listProfiles(@NonNull Context context) {
        Uri treeUri = getProfilesTreeUri(context);
        Set<String> out = new TreeSet<>();
        if (treeUri != null) {
            out.addAll(listProfilesFromTree(context, treeUri));
        }
        out.addAll(listBundledProfiles(context));
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
                return Collections.emptyList();
            }
            int nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            while (c.moveToNext()) {
                addProfileName(out, c.getString(nameCol));
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        Collections.sort(out);
        return out;
    }

    @NonNull
    private List<String> listBundledProfiles(@NonNull Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(BROUTER_PACKAGE, 0);
            try (ZipFile apk = new ZipFile(appInfo.sourceDir)) {
                ZipEntry profilesZipEntry = apk.getEntry(BROUTER_PROFILES_ZIP);
                if (profilesZipEntry == null) {
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
                return out;
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private void addProfileName(@NonNull List<String> out, @Nullable String rawName) {
        if (rawName == null) {
            return;
        }
        String name = rawName.trim();
        if (name.isEmpty()) {
            return;
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < name.length()) {
            name = name.substring(slash + 1);
        }
        if (!name.toLowerCase().endsWith(".brf")) {
            return;
        }
        String base = name.substring(0, name.length() - 4).trim();
        if (!base.isEmpty()) {
            out.add(base);
        }
    }
}
