package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.android.export.AndroidRouteGpxActions;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.android.storage.AndroidOutputDestination;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.android.storage.AndroidOutputStorageBrowser;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;

final class AboutOutputFolderSettings {
    static final int REQUEST_PHONE_STORAGE = 4011;
    private final Activity activity;
    private final ExecutorService worker;
    private final AboutOutputStorageSwitch storageSwitch;

    AboutOutputFolderSettings(Activity activity) {
        this(activity, Executors.newSingleThreadExecutor());
    }

    AboutOutputFolderSettings(Activity activity, ExecutorService worker) {
        this.activity = activity;
        this.worker = worker;
        storageSwitch = new AboutOutputStorageSwitch(activity, worker);
    }

    void configure() {
        storageSwitch.configure();
        AboutDeferredDialogAction.configure(activity,
                activity.findViewById(R.id.aboutLogFolderSettingsButton), () -> show(Kind.LOGS));
        AboutDeferredDialogAction.configure(activity,
                activity.findViewById(R.id.aboutGpxFolderSettingsButton), () -> show(Kind.GPX));
    }

    void render() {
        storageSwitch.render();
    }

    private void show(Kind kind) {
        AndroidOutputDestination destination = AndroidOutputStorage.current(activity, kind);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(kind == Kind.LOGS ? R.string.title_log_output_folder : R.string.title_gpx_output_folder)
                .setMessage(activity.getString(R.string.format_output_folder, destination.label) + "\n\n"
                        + activity.getString(kind == Kind.LOGS ? R.string.hint_log_output_folder
                        : R.string.hint_gpx_output_folder))
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.action_open_output_folder, null)
                .setNegativeButton(R.string.action_clear_output_folder, null).show();
        alignButtons(dialog);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(activity, R.color.danger));
        AboutDeferredDialogAction.configure(activity, dialog.getButton(AlertDialog.BUTTON_NEGATIVE),
                () -> confirmClear(kind, destination, dialog));
        AboutDeferredDialogAction.configure(activity, dialog.getButton(AlertDialog.BUTTON_NEUTRAL),
                () -> worker.execute(() -> prepareOpen(destination)));
    }

    private static void alignButtons(AlertDialog dialog) {
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE).getParent() instanceof LinearLayout panel) {
            for (int i = 0; i < panel.getChildCount(); i++) {
                View child = panel.getChildAt(i);
                if (!(child instanceof Button)) {
                    child.setVisibility(View.GONE);
                }
            }
            panel.setGravity(Gravity.END);
        }
    }

    private void prepareOpen(AndroidOutputDestination destination) {
        try {
            AndroidOutputStorageBrowser.prepare(activity, destination);
        } catch (IOException | RuntimeException e) {
            AppLogger.w("AboutOutputFolderSettings", "Could not prepare folder for Files: " + destination.label, e);
        }
        // Browsing is still useful when writes fail or storage was removed; Files can show its root.
        activity.runOnUiThread(() -> launchFolder(destination));
    }

    private void launchFolder(AndroidOutputDestination destination) {
        if (activity.isDestroyed() || activity.isFinishing()) {
            return;
        }
        try {
            AndroidOutputStorageBrowser.open(activity, destination);
        } catch (ActivityNotFoundException | SecurityException e) {
            toast(R.string.msg_output_viewer_unavailable);
        }
    }

    private void confirmClear(Kind kind, AndroidOutputDestination destination, AlertDialog folderDialog) {
        AlertDialog confirm = new AlertDialog.Builder(activity).setTitle(R.string.action_clear_output_folder)
                .setMessage(activity.getString(kind == Kind.LOGS ? R.string.hint_clear_log_output_folder
                        : R.string.hint_clear_gpx_output_folder, destination.label))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_clear_output_folder, (dialog, which) -> {
                    folderDialog.dismiss();
                    clear(kind, destination.token());
                }).show();
        confirm.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(activity, R.color.danger));
    }

    private void clear(Kind kind, String destination) {
        if (kind == Kind.GPX) {
            AndroidRouteGpxActions.clearFolder(activity.getApplicationContext(), destination, this::onCleared);
        } else {
            worker.execute(() -> clearLogs(destination));
        }
    }

    private void clearLogs(String destination) {
        boolean cleared;
        try {
            AppLogger.clearOutputFolder(activity.getApplicationContext(), destination);
            cleared = true;
        } catch (IOException | RuntimeException e) {
            cleared = false;
        }
        onCleared(cleared);
    }

    private void onCleared(boolean cleared) {
        activity.runOnUiThread(() -> {
            if (!activity.isDestroyed()) {
                toast(cleared ? R.string.msg_output_folder_cleared : R.string.msg_output_folder_clear_failed);
            }
        });
    }

    static void requestAccess(Activity activity) {
        if (AppOutputStorageSettings.useSdCard(activity)) {
            if (activity instanceof AboutActivity about) {
                about.requestOutputStorageAccess();
            }
        } else {
            requestPhoneAccess(activity);
        }
    }

    static void requestPhoneAccess(Activity activity) {
        if (!AppOutputStorageSettings.useSdCard(activity)
                && AndroidLegacyExternalStorageAccess.needsOutputPermission(activity)) {
            AndroidLegacyExternalStorageAccess.requestOutputPermission(activity, REQUEST_PHONE_STORAGE);
        }
    }

    void requestStorageAccess() {
        storageSwitch.requestAccess();
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return storageSwitch.handleActivityResult(requestCode, resultCode, data);
    }

    boolean handlePermissionResult(int requestCode) {
        if (requestCode != REQUEST_PHONE_STORAGE) {
            return false;
        }
        worker.execute(() -> {
            AndroidOutputStorage.resetFailures();
            AppLogger.refreshOutputFolder(activity.getApplicationContext());
        });
        return true;
    }

    private void toast(int message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }

    void shutdown() {
        flush();
        worker.shutdown();
    }

    void flush() {
        storageSwitch.flush();
    }
}
