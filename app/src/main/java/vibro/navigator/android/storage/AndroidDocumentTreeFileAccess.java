package vibro.navigator.android.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import vibro.navigator.logging.AppLogger;

final class AndroidDocumentTreeFileAccess {
    private static final String TAG = "AndroidDocumentAccess";

    private AndroidDocumentTreeFileAccess() {
    }

    @Nullable
    static Uri childDocumentUri(
            @NonNull Context context,
            @NonNull Uri treeUri,
            @NonNull String displayName
    ) {
        String treeDocumentId = AndroidDocumentAccess.treeDocumentId(treeUri);
        if (treeDocumentId == null) {
            return null;
        }
        return findChildDocumentUri(context, treeUri, treeDocumentId, displayName);
    }

    @Nullable
    static String readText(@NonNull Context context, @NonNull Uri documentUri) {
        try (InputStream input = context.getContentResolver().openInputStream(documentUri)) {
            if (input == null) {
                AppLogger.w(TAG, "Document input stream unavailable uri=" + documentUri);
                return null;
            }
            return readUtf8(input);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read document text uri=" + documentUri, e);
            return null;
        }
    }

    @Nullable
    private static Uri findChildDocumentUri(
            @NonNull Context context,
            @NonNull Uri treeUri,
            @NonNull String treeDocumentId,
            @NonNull String displayName
    ) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId);
        try (Cursor cursor = queryChildren(context, childrenUri)) {
            return cursor == null ? null : findChildDocumentUri(treeUri, cursor, displayName);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to resolve child document uri treeUri=" + treeUri
                    + " displayName=" + displayName, e);
            return null;
        }
    }

    @Nullable
    private static Cursor queryChildren(@NonNull Context context, @NonNull Uri childrenUri) {
        return context.getContentResolver().query(
                childrenUri,
                new String[]{
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID
                },
                null,
                null,
                null
        );
    }

    @Nullable
    private static Uri findChildDocumentUri(
            @NonNull Uri treeUri,
            @NonNull Cursor cursor,
            @NonNull String displayName
    ) {
        int nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
        int idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
        if (nameCol < 0 || idCol < 0) {
            return null;
        }
        while (cursor.moveToNext()) {
            if (displayName.equals(cursor.getString(nameCol))) {
                return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idCol));
            }
        }
        return null;
    }

    @NonNull
    private static String readUtf8(@NonNull InputStream input) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
