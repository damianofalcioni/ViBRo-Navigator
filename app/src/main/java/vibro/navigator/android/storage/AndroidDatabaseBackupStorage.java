package vibro.navigator.android.storage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppDataBackup;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** JSON backups share the selected Downloads volume and its existing storage access. */
public final class AndroidDatabaseBackupStorage {
    private static final String TAG = "DatabaseBackupStorage";

    private AndroidDatabaseBackupStorage() { }

    public static String exportDatabase(Context context) throws IOException, JSONException {
        return save(context, AppDataBackup.exportJson(context), AppDataBackup.defaultFileName());
    }

    static String save(Context context, String json, String name) throws IOException {
        AndroidOutputFile file = AndroidOutputStorage.create(context, Kind.BACKUP, AppDataBackup.MIME_TYPE, name);
        try {
            try (OutputStream out = file.open()) {
                out.write(json.getBytes(StandardCharsets.UTF_8));
            }
            String savedName = file.displayName();
            file.publish();
            return savedName;
        } catch (IOException | RuntimeException e) {
            file.removeQuietly();
            AndroidOutputStorage.failed(Kind.BACKUP, file.destination);
            throw new IOException("Could not save database backup", e);
        }
    }

    public static Intent importIntent(Context context) {
        AndroidOutputDestination destination = AndroidOutputStorage.current(context, Kind.BACKUP);
        try {
            AndroidOutputStorageBrowser.prepare(context, AndroidOutputStorage.requested(context, Kind.BACKUP));
        } catch (IOException | RuntimeException e) {
            AppLogger.w(TAG, "Could not prepare backup folder for import", e);
        }
        // Providers may report .json as text/plain or application/octet-stream. Validate the
        // backup's JSON/schema after selection instead of disabling files by provider MIME type.
        return AndroidDocumentAccess.openDocumentIntent(AndroidOutputStorageBrowser.folderUri(context, destination));
    }

    public static void importDatabase(Context context, Uri uri) throws IOException, JSONException {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("No input stream for backup document");
            }
            AppDataBackup.importJson(context, readUtf8(in));
        }
    }

    private static String readUtf8(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
