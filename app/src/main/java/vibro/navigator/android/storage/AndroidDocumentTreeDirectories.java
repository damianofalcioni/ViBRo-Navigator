package vibro.navigator.android.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.DocumentsContract;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import vibro.navigator.logging.AppLogger;

/** Directory enumeration used to verify a document-tree grant. */
public final class AndroidDocumentTreeDirectories {
    private static final String TAG = "AndroidDocumentAccess";

    private AndroidDocumentTreeDirectories() { }

    public static Map<String, Uri> childDirectories(Context context, Uri tree, CancellationSignal signal) {
        String id = AndroidDocumentAccess.treeDocumentId(tree);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id);
        try (Cursor cursor = context.getContentResolver().query(children, new String[]{
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        }, null, null, null, signal)) {
            return cursor == null ? Collections.emptyMap() : readDirectories(tree, cursor);
        } catch (RuntimeException e) {
            AppLogger.d(TAG, "Unable to list BRouter directories: " + e.getClass().getSimpleName());
            return Collections.emptyMap();
        }
    }

    private static Map<String, Uri> readDirectories(Uri tree, Cursor cursor) {
        int name = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
        int id = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
        int type = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
        Map<String, Uri> directories = new HashMap<>();
        while (cursor.moveToNext()) {
            String displayName = cursor.getString(name);
            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(type))
                    && !directories.containsKey(displayName)) {
                directories.put(displayName, DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(id)));
            }
        }
        return directories;
    }

    public static boolean canList(Context context, Uri directory, CancellationSignal signal) {
        String id = AndroidDocumentAccess.directoryDocumentId(directory);
        if (id == null) {
            return false;
        }
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(directory, id);
        try (Cursor cursor = context.getContentResolver().query(children,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID}, null, null, null, signal)) {
            return cursor != null && cursor.getCount() >= 0;
        } catch (RuntimeException e) {
            AppLogger.d(TAG, "Unable to read BRouter directory: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
