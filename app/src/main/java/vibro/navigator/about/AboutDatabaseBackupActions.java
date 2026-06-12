package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppDataBackup;

final class AboutDatabaseBackupActions {

    private static final String TAG = "AboutDatabaseBackup";
    private static final int REQ_EXPORT_DATABASE = 4001;
    private static final int REQ_IMPORT_DATABASE = 4002;

    @NonNull
    private final Activity activity;
    @NonNull
    private final Runnable beforePicker;
    @NonNull
    private final Runnable afterImport;

    AboutDatabaseBackupActions(
            @NonNull Activity activity,
            @NonNull Runnable beforePicker,
            @NonNull Runnable afterImport
    ) {
        this.activity = activity;
        this.beforePicker = beforePicker;
        this.afterImport = afterImport;
    }

    void configure(@NonNull View exportButton, @NonNull View importButton) {
        exportButton.setOnClickListener(v -> openExportDatabasePicker());
        importButton.setOnClickListener(v -> openImportDatabasePicker());
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_EXPORT_DATABASE) {
            handleExportResult(resultCode, data);
            return true;
        }
        if (requestCode == REQ_IMPORT_DATABASE) {
            handleImportResult(resultCode, data);
            return true;
        }
        return false;
    }

    private void openExportDatabasePicker() {
        beforePicker.run();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(AppDataBackup.MIME_TYPE)
                .putExtra(Intent.EXTRA_TITLE, AppDataBackup.DEFAULT_FILE_NAME);
        activity.startActivityForResult(intent, REQ_EXPORT_DATABASE);
    }

    private void openImportDatabasePicker() {
        beforePicker.run();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(AppDataBackup.MIME_TYPE);
        activity.startActivityForResult(intent, REQ_IMPORT_DATABASE);
    }

    private void handleExportResult(int resultCode, @Nullable Intent data) {
        Uri uri = resultUri(resultCode, data);
        if (uri != null) {
            exportDatabase(uri);
        }
    }

    private void handleImportResult(int resultCode, @Nullable Intent data) {
        Uri uri = resultUri(resultCode, data);
        if (uri != null) {
            importDatabase(uri);
        }
    }

    @Nullable
    private static Uri resultUri(int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return null;
        }
        return data.getData();
    }

    private void exportDatabase(@NonNull Uri uri) {
        try (OutputStream out = activity.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IOException("No output stream for backup document");
            }
            out.write(AppDataBackup.exportJson(activity).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(activity, R.string.msg_database_exported, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to export database", e);
            Toast.makeText(activity, R.string.msg_database_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(@NonNull Uri uri) {
        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("No input stream for backup document");
            }
            AppDataBackup.importJson(activity, readUtf8(in));
            afterImport.run();
            Toast.makeText(activity, R.string.msg_database_imported, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to import database", e);
            Toast.makeText(activity, R.string.msg_database_import_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private static String readUtf8(@NonNull InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
