package vibro.navigator.android.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Semaphore;

import vibro.navigator.android.storage.AndroidDocumentAccess.DirectoryPresence;
import vibro.navigator.logging.AppLogger;

final class AndroidExternalDirectoryPresence {
    // A provider can ignore cancellation. Limit outstanding probes to that provider.
    private static final Semaphore DISCOVERY_QUERY = new Semaphore(1);
    private AndroidExternalDirectoryPresence() {
    }

    @NonNull
    static DirectoryPresence check(@NonNull Context context, @NonNull String documentId,
                                   @NonNull CancellationSignal signal) {
        signal.throwIfCanceled();
        DirectoryPresence directPresence = directDirectoryPresence(context, documentId);
        if (directPresence == DirectoryPresence.PRESENT) {
            return directPresence;
        }
        // A denied provider query leaves open the possibility that the picker can see the folder.
        DirectoryPresence providerPresence = discoveryProviderPresence(context, documentId, signal);
        return providerPresence == DirectoryPresence.MISSING ? directPresence : providerPresence;
    }

    private static DirectoryPresence discoveryProviderPresence(Context context, String documentId,
                                                               CancellationSignal signal) {
        if (!DISCOVERY_QUERY.tryAcquire()) {
            return DirectoryPresence.UNKNOWN;
        }
        try {
            return documentProviderDirectoryPresence(context, documentId, signal);
        } finally {
            DISCOVERY_QUERY.release();
        }
    }

    @NonNull
    static DirectoryPresence documentProviderDirectoryPresence(@NonNull Context context, @NonNull String documentId) {
        return documentProviderDirectoryPresence(context, documentId, null);
    }

    @NonNull
    private static DirectoryPresence documentProviderDirectoryPresence(Context context, String documentId,
                                                                       CancellationSignal signal) {
        Uri treeUri = AndroidDocumentAccess.buildExternalStorageTreeUri(documentId);
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        String mimeType = DocumentsContract.Document.COLUMN_MIME_TYPE;
        try (Cursor cursor = context.getContentResolver().query(documentUri, new String[]{mimeType},
                null, null, null, signal)) {
            return cursor != null && cursor.moveToFirst() && DocumentsContract.Document.MIME_TYPE_DIR.equals(
                    cursor.getString(cursor.getColumnIndexOrThrow(mimeType)))
                    ? DirectoryPresence.PRESENT : DirectoryPresence.MISSING;
        } catch (OperationCanceledException e) {
            if (signal != null) {
                signal.throwIfCanceled();
            }
            return DirectoryPresence.UNKNOWN;
        } catch (Exception e) {
            AppLogger.d("AndroidDocumentAccess", "Document not accessible documentId=" + documentId
                    + " error=" + e.getClass().getSimpleName());
            return DirectoryPresence.UNKNOWN;
        }
    }

    @NonNull
    static DirectoryPresence directDirectoryPresence(@NonNull Context context, @NonNull String documentId) {
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
