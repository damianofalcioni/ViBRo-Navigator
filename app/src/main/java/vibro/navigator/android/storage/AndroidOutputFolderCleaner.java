package vibro.navigator.android.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** Deletes only this output's generated files, retaining the folder and its other contents. */
public final class AndroidOutputFolderCleaner {
    private static final Pattern LOG_NAMES = Pattern.compile("vibro-navigator-log-\\d{14}(?:-\\d+)?\\.txt");
    private static final Pattern GPX_NAMES = Pattern.compile("vibro-navigator-route-\\d{14}(?:-\\d+)?\\.gpx");

    private AndroidOutputFolderCleaner() {
    }

    public static void clear(Context context, Kind kind, @Nullable String selected) throws IOException {
        if (selected != null) {
            clearDocuments(context, kind, AndroidOutputFolderAccess.ensureFolder(context, Uri.parse(selected)));
            return;
        }
        clearDefault(kind, AndroidAppStorageDirs.preferredExternalFilesDir(context));
        clearDefault(kind, AndroidAppStorageDirs.internalFilesDir(context));
    }

    private static void clearDefault(Kind kind, @Nullable File root) throws IOException {
        if (root == null) {
            return;
        }
        File folder = new File(root, kind == Kind.LOGS ? "logs" : "gpx");
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            throw new IOException("Could not list default output folder");
        }
        for (File file : files) {
            deleteDefaultFile(kind, file);
        }
    }

    private static void deleteDefaultFile(Kind kind, File file) throws IOException {
        if (file.isFile() && matches(kind, file.getName(), true) && !file.delete()) {
            throw new IOException("Could not delete output file");
        }
    }

    private static void clearDocuments(Context context, Kind kind, Uri folder) throws IOException {
        for (Uri document : outputDocuments(context, kind, folder)) {
            if (!DocumentsContract.deleteDocument(context.getContentResolver(), document)) {
                throw new IOException("Could not delete output document");
            }
        }
    }

    private static List<Uri> outputDocuments(Context context, Kind kind, Uri folder) throws IOException {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(folder,
                AndroidWritableDocumentTree.directoryId(folder));
        List<Uri> result = new ArrayList<>();
        try (Cursor cursor = AndroidWritableDocumentTree.query(context, children, new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE})) {
            if (cursor == null) {
                throw new IOException("Could not list output folder");
            }
            while (cursor.moveToNext()) {
                if (!DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))
                        && matches(kind, cursor.getString(1), false)) {
                    result.add(DocumentsContract.buildDocumentUriUsingTree(folder, cursor.getString(0)));
                }
            }
        }
        return result;
    }

    private static boolean matches(Kind kind, String name, boolean defaultFolder) {
        Pattern pattern = kind == Kind.LOGS ? LOG_NAMES : GPX_NAMES;
        return pattern.matcher(name).matches()
                || (defaultFolder && kind == Kind.LOGS && "app-behavior.log".equals(name));
    }
}
