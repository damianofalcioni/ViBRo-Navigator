package com.vibenavigator.brouter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BRouterProfilesRepository {

    private static final String PREFS = "vibenavigator_brouter";
    private static final String KEY_PROFILES_TREE_URI = "profiles_tree_uri";

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
            context.getPackageManager().getPackageInfo("btools.routingapp", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @NonNull
    public List<String> listProfiles(@NonNull Context context) {
        Uri treeUri = getProfilesTreeUri(context);
        if (treeUri == null) {
            return Collections.emptyList();
        }
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
                String name = c.getString(nameCol);
                if (name == null) {
                    continue;
                }
                if (!name.toLowerCase().endsWith(".brf")) {
                    continue;
                }
                String base = name.substring(0, name.length() - 4);
                if (!base.trim().isEmpty()) {
                    out.add(base);
                }
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        Collections.sort(out);
        return out;
    }
}
