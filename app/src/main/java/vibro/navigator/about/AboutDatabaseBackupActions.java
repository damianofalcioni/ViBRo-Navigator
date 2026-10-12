package vibro.navigator.about;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidDatabaseBackupStorage;
import vibro.navigator.android.storage.AndroidOutputStorageBrowser;
import vibro.navigator.logging.AppLogger;

final class AboutDatabaseBackupActions {

    private static final String TAG = "AboutDatabaseBackup";
    private static final int REQ_IMPORT_DATABASE = 4002;
    private static final Executor WORKER = Executors.newSingleThreadExecutor();

    private final Activity activity;
    private final Runnable beforeBackup;
    private final Runnable afterImport;
    private final Executor worker;
    private boolean busy;

    AboutDatabaseBackupActions(Activity activity, Runnable beforeBackup, Runnable afterImport) {
        this(activity, beforeBackup, afterImport, WORKER);
    }

    AboutDatabaseBackupActions(Activity activity, Runnable beforeBackup, Runnable afterImport, Executor worker) {
        this.activity = activity;
        this.beforeBackup = beforeBackup;
        this.afterImport = afterImport;
        this.worker = worker;
    }

    void configure(View exportButton, View importButton) {
        AboutDeferredDialogAction.configure(activity, exportButton, () -> startWork(this::exportDatabase));
        AboutDeferredDialogAction.configure(activity, importButton, () -> startWork(this::prepareImportPicker));
    }

    void handleLaunchIntent(Intent intent, boolean recreated) {
        // A restored activity's system launch intent may still contain an already delivered result.
        if (!recreated) {
            AndroidOutputStorageBrowser.dispatchResult(intent, this::handleActivityResult);
        }
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQ_IMPORT_DATABASE) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            startWork(() -> importDatabase(uri));
        }
        return true;
    }

    private void startWork(Runnable work) {
        if (busy) {
            return;
        }
        busy = true;
        beforeBackup.run();
        worker.execute(work);
    }

    private void prepareImportPicker() {
        Intent intent = AndroidDatabaseBackupStorage.importIntent(activity);
        complete(() -> {
            try {
                AndroidOutputStorageBrowser.openForResult(activity, intent, REQ_IMPORT_DATABASE);
            } catch (ActivityNotFoundException | SecurityException e) {
                AppLogger.w(TAG, "Could not open database import picker", e);
                Toast.makeText(activity, R.string.msg_database_import_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportDatabase() {
        try {
            String name = AndroidDatabaseBackupStorage.exportDatabase(activity);
            complete(() -> Toast.makeText(activity, activity.getString(R.string.msg_database_exported, name),
                    Toast.LENGTH_LONG).show());
        } catch (IOException | JSONException | RuntimeException e) {
            AppLogger.w(TAG, "Failed to export database", e);
            complete(() -> Toast.makeText(activity, R.string.msg_database_export_failed, Toast.LENGTH_SHORT).show());
        }
    }

    private void importDatabase(Uri uri) {
        try {
            AndroidDatabaseBackupStorage.importDatabase(activity, uri);
            complete(() -> {
                afterImport.run();
                Toast.makeText(activity, R.string.msg_database_imported, Toast.LENGTH_SHORT).show();
            });
        } catch (IOException | JSONException | RuntimeException e) {
            AppLogger.w(TAG, "Failed to import database", e);
            complete(() -> Toast.makeText(activity, R.string.msg_database_import_failed, Toast.LENGTH_SHORT).show());
        }
    }

    private void complete(Runnable uiAction) {
        activity.runOnUiThread(() -> {
            busy = false;
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                uiAction.run();
            }
        });
    }
}
