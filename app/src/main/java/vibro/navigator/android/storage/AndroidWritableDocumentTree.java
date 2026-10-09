package vibro.navigator.android.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/** Writes documents using tree URIs, without assuming a filesystem path or overwriting old exports. */
public final class AndroidWritableDocumentTree {
    private AndroidWritableDocumentTree() {
    }

    @NonNull
    public static Uri createFile(@NonNull Context context, @NonNull Uri tree,
                                 @NonNull String mimeType, @NonNull String name) throws IOException {
        String parentId = directoryId(tree);
        Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, parentId);
        String uniqueName = uniqueName(context, tree, parentId, name);
        Uri document = DocumentsContract.createDocument(context.getContentResolver(), parent, mimeType, uniqueName);
        if (document == null) {
            throw new IOException("Could not create output document");
        }
        return document;
    }

    @NonNull
    private static String uniqueName(@NonNull Context context, @NonNull Uri tree,
                                     @NonNull String parentId, @NonNull String name) throws IOException {
        Set<String> names = new HashSet<>();
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        try (Cursor cursor = queryNames(context, children)) {
            if (cursor == null) {
                throw new IOException("Could not list output folder");
            }
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0));
            }
        }
        int index = 1;
        String candidate = name;
        int dot = name.lastIndexOf('.');
        while (names.contains(candidate)) {
            index++;
            candidate = dot > 0 ? name.substring(0, dot) + "-" + index + name.substring(dot)
                    : name + "-" + index;
        }
        return candidate;
    }

    private static Cursor queryNames(@NonNull Context context, @NonNull Uri children) {
        return query(context, children, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME});
    }

    static Cursor query(Context context, Uri uri, String[] columns) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getContentResolver().query(uri, columns, Bundle.EMPTY, null);
        }
        return context.getContentResolver().query(uri, columns, null, null, null);
    }

    static String directoryId(Uri folder) {
        try {
            return DocumentsContract.getDocumentId(folder);
        } catch (IllegalArgumentException e) {
            return DocumentsContract.getTreeDocumentId(folder);
        }
    }

    @NonNull
    public static OutputStream open(@NonNull Context context, @NonNull Uri document) throws IOException {
        OutputStream out = context.getContentResolver().openOutputStream(document, "wt");
        if (out == null) {
            throw new IOException("Could not open output document");
        }
        return out;
    }

    static void verifyWrite(Context context, Uri folder) throws IOException {
        Uri probe = createFile(context, folder, "text/plain", ".vibro-write-check.txt");
        try (OutputStream out = open(context, probe)) {
            out.write(0);
        } finally {
            removeQuietly(context, probe);
        }
    }

    public static void removeQuietly(Context context, @Nullable Uri document) {
        if (document == null) {
            return;
        }
        try {
            DocumentsContract.deleteDocument(context.getContentResolver(), document);
        } catch (IOException | RuntimeException ignored) {
            // A disappearing provider can also prevent cleanup of its partial document.
        }
    }
}
