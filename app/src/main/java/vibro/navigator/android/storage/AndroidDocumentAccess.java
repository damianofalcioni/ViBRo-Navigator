package vibro.navigator.android.storage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.logging.AppLogger;

public final class AndroidDocumentAccess {
    private static final String TAG = "AndroidDocumentAccess";
    private static final String EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents";

    private AndroidDocumentAccess() {
    }

    @NonNull
    public static Intent openDocumentTreeIntent(@Nullable Uri initialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        addInitialUriIfSupported(intent, initialUri);
        return intent;
    }

    @NonNull
    public static Intent openDocumentIntent(@Nullable Uri initialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        addInitialUriIfSupported(intent, initialUri);
        return intent;
    }

    public static boolean persistReadPermission(
            @NonNull Activity activity,
            @NonNull Intent data,
            @NonNull Uri uri
    ) {
        if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
            AppLogger.w(TAG, "Document picker returned without persistable read grant uri=" + uri);
            return false;
        }
        try {
            activity.getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
            AppLogger.i(TAG, "Persisted document read permission uri=" + uri);
            return true;
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to persist document read permission uri=" + uri, e);
            return false;
        }
    }

    @Nullable
    public static String displayName(@NonNull Context context, @NonNull Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameCol >= 0) {
                    return cursor.getString(nameCol);
                }
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to query document display name uri=" + uri, e);
        }
        return uri.getLastPathSegment();
    }

    @Nullable
    public static String documentId(@NonNull Uri documentUri) {
        try {
            return DocumentsContract.getDocumentId(documentUri);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read document id uri=" + documentUri, e);
            return null;
        }
    }

    @Nullable
    public static String treeDocumentId(@NonNull Uri treeUri) {
        try {
            return DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read tree document id uri=" + treeUri, e);
            return null;
        }
    }

    public static boolean isExternalStorageDocument(@NonNull Uri uri) {
        return EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY.equals(uri.getAuthority());
    }

    @NonNull
    public static Uri buildExternalStorageDocumentUri(@NonNull String documentId) {
        return DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY, documentId);
    }

    @NonNull
    public static Uri buildExternalStorageTreeUri(@NonNull String documentId) {
        return DocumentsContract.buildTreeDocumentUri(EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY, documentId);
    }

    @Nullable
    public static Uri buildTreeDocumentUri(@NonNull Uri treeUri) {
        String treeDocumentId = treeDocumentId(treeUri);
        if (treeDocumentId == null) {
            return null;
        }
        try {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to build tree document URI treeUri=" + treeUri, e);
            return null;
        }
    }

    @NonNull
    public static List<String> childDisplayNames(@NonNull Context context, @NonNull Uri treeUri) {
        String treeDocumentId = treeDocumentId(treeUri);
        if (treeDocumentId == null) {
            return Collections.emptyList();
        }
        List<String> directNames = AndroidExternalStorageDocumentFiles.childDisplayNames(context, treeDocumentId);
        if (directNames != null) {
            return directNames;
        }
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId);
        List<String> out = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(
                childrenUri,
                new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor == null) {
                AppLogger.w(TAG, "Document tree query returned null cursor uri=" + treeUri);
                return Collections.emptyList();
            }
            int nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            if (nameCol < 0) {
                AppLogger.w(TAG, "Document tree query missing display-name column uri=" + treeUri);
                return Collections.emptyList();
            }
            while (cursor.moveToNext()) {
                out.add(cursor.getString(nameCol));
            }
            return out;
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to list document tree children uri=" + treeUri, e);
            return Collections.emptyList();
        }
    }

    @Nullable
    public static Uri childDocumentUri(
            @NonNull Context context,
            @NonNull Uri treeUri,
            @NonNull String displayName
    ) {
        return AndroidDocumentTreeFileAccess.childDocumentUri(context, treeUri, displayName);
    }

    @Nullable
    public static String readText(@NonNull Context context, @NonNull Uri documentUri) {
        if (AndroidExternalStorageDocumentFiles.isFileUri(documentUri)) {
            return AndroidExternalStorageDocumentFiles.readText(documentUri);
        }
        return AndroidDocumentTreeFileAccess.readText(context, documentUri);
    }

    public static boolean externalStorageDocumentExists(@NonNull Context context, @NonNull String documentId) {
        if (AndroidExternalStorageDocumentFiles.directoryExists(context, documentId)) {
            AppLogger.d(TAG, "Direct external storage document accessible documentId=" + documentId);
            return true;
        }
        Uri treeUri = buildExternalStorageTreeUri(documentId);
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
            AppLogger.d(TAG, "Document not accessible documentId=" + documentId
                    + " error=" + e.getClass().getSimpleName());
            return false;
        }
    }

    private static void addInitialUriIfSupported(@NonNull Intent intent, @Nullable Uri initialUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }
    }
}
