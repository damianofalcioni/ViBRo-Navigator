package vibro.navigator.android.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.logging.AppLogger;

public final class AndroidDocumentTreeFileAccess {
    private static final String TAG = "AndroidDocumentAccess";

    private AndroidDocumentTreeFileAccess() {
    }

    @NonNull
    public static List<String> childDisplayNames(Context context, Uri treeUri, @Nullable CancellationSignal signal) {
        String directoryId = AndroidDocumentAccess.directoryDocumentId(treeUri);
        if (directoryId == null) {
            return Collections.emptyList();
        }
        if (AndroidDocumentAccess.isExternalStorageDocument(treeUri)) {
            List<String> directNames = AndroidExternalStorageDocumentFiles.childDisplayNames(context, directoryId);
            if (directNames != null) {
                return directNames;
            }
        }
        return queryDisplayNames(context, treeUri, directoryId, signal);
    }

    private static List<String> queryDisplayNames(Context context, Uri treeUri, String directoryId,
                                                  @Nullable CancellationSignal signal) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directoryId);
        try (Cursor cursor = queryChildren(context, childrenUri, signal)) {
            return cursor == null ? Collections.emptyList() : readDisplayNames(cursor);
        } catch (Exception e) {
            checkCanceled(signal);
            AppLogger.w(TAG, "Failed to list document tree children uri=" + treeUri, e);
            return Collections.emptyList();
        }
    }

    private static List<String> readDisplayNames(Cursor cursor) {
        int nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
        if (nameColumn < 0) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        while (cursor.moveToNext()) {
            names.add(cursor.getString(nameColumn));
        }
        return names;
    }

    @Nullable
    public static Uri childDocumentUri(
            @NonNull Context context,
            @NonNull Uri treeUri,
            @NonNull String displayName,
            @Nullable CancellationSignal signal
    ) {
        String directoryId = AndroidDocumentAccess.directoryDocumentId(treeUri);
        if (directoryId == null) {
            return null;
        }
        if (AndroidDocumentAccess.isExternalStorageDocument(treeUri)) {
            Uri directUri = AndroidExternalStorageDocumentFiles.childFileUri(context, directoryId, displayName);
            if (directUri != null) {
                return directUri;
            }
            String grantId = AndroidDocumentAccess.treeDocumentId(treeUri);
            if (grantId == null || !AndroidPersistedUriPermissions.hasReadPermission(context,
                    AndroidDocumentAccess.treeGrantUri(treeUri, grantId))) {
                AppLogger.d(TAG, "Skipping ungranted external storage tree child lookup treeUri=" + treeUri
                        + " displayName=" + displayName);
                return null;
            }
        }
        return findChildDocumentUri(context, treeUri, directoryId, displayName, signal);
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
            @NonNull String displayName,
            @Nullable CancellationSignal signal
    ) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId);
        try (Cursor cursor = queryChildren(context, childrenUri, signal)) {
            return cursor == null ? null : findChildDocumentUri(treeUri, cursor, displayName);
        } catch (Exception e) {
            checkCanceled(signal);
            AppLogger.w(TAG, "Failed to resolve child document uri treeUri=" + treeUri
                    + " displayName=" + displayName, e);
            return null;
        }
    }

    private static void checkCanceled(@Nullable CancellationSignal signal) {
        if (signal != null) {
            signal.throwIfCanceled();
        }
    }

    @Nullable
    private static Cursor queryChildren(@NonNull Context context, @NonNull Uri childrenUri,
                                        @Nullable CancellationSignal signal) {
        return context.getContentResolver().query(
                childrenUri,
                new String[]{
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID
                },
                null,
                null,
                null,
                signal
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
