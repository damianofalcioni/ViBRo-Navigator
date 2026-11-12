package vibro.navigator.android.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.attribute.BasicFileAttributes;

import vibro.navigator.android.storage.AndroidDocumentAccess.DirectoryPresence;
import vibro.navigator.logging.AppLogger;

final class AndroidExternalDirectoryPresence {
    private AndroidExternalDirectoryPresence() {
    }

    @NonNull
    static DirectoryPresence check(@NonNull Context context, @NonNull String documentId) {
        DirectoryPresence presence = directDirectoryPresence(context, documentId);
        // The document provider can see directories hidden from the app's filesystem view.
        if (presence != DirectoryPresence.PRESENT && documentProviderDirectoryExists(context, documentId)) {
            return DirectoryPresence.PRESENT;
        }
        return presence;
    }

    static boolean documentProviderDirectoryExists(@NonNull Context context, @NonNull String documentId) {
        Uri treeUri = AndroidDocumentAccess.buildExternalStorageTreeUri(documentId);
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        String mimeType = DocumentsContract.Document.COLUMN_MIME_TYPE;
        try (Cursor cursor = context.getContentResolver().query(documentUri, new String[]{mimeType}, null, null, null)) {
            return cursor != null && cursor.moveToFirst() && DocumentsContract.Document.MIME_TYPE_DIR.equals(
                    cursor.getString(cursor.getColumnIndexOrThrow(mimeType)));
        } catch (Exception e) {
            AppLogger.d("AndroidDocumentAccess", "Document not accessible documentId=" + documentId
                    + " error=" + e.getClass().getSimpleName());
            return false;
        }
    }

    @NonNull
    private static DirectoryPresence directDirectoryPresence(@NonNull Context context, @NonNull String documentId) {
        File directory = AndroidExternalStorageDocumentFiles.fileForDocumentId(context, documentId);
        if (directory == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return DirectoryPresence.UNKNOWN;
        }
        return statDirectory(directory);
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.O)
    private static DirectoryPresence statDirectory(@NonNull File directory) {
        try {
            return Files.readAttributes(directory.toPath(), BasicFileAttributes.class).isDirectory()
                    ? DirectoryPresence.PRESENT : DirectoryPresence.MISSING;
        } catch (NoSuchFileException | NotDirectoryException e) {
            return DirectoryPresence.MISSING;
        } catch (IOException | SecurityException e) {
            return DirectoryPresence.UNKNOWN;
        }
    }
}
