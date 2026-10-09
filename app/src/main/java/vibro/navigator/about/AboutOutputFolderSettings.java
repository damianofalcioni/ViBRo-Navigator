package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.android.export.AndroidRouteGpxActions;
import vibro.navigator.android.storage.AndroidOutputFolderAccess;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

final class AboutOutputFolderSettings {
    private static final int REQUEST_LOGS = 4011;
    private static final int REQUEST_GPX = 4012;
    private final Activity activity;
    private final ExecutorService worker;

    AboutOutputFolderSettings(Activity activity) {
        this(activity, Executors.newSingleThreadExecutor());
    }

    AboutOutputFolderSettings(Activity activity, ExecutorService worker) {
        this.activity = activity;
        this.worker = worker;
    }

    void configure() {
        AboutDeferredDialogAction.configure(activity,
                activity.findViewById(R.id.aboutLogFolderSettingsButton), () -> show(Kind.LOGS));
        AboutDeferredDialogAction.configure(activity,
                activity.findViewById(R.id.aboutGpxFolderSettingsButton), () -> show(Kind.GPX));
    }

    private void show(Kind kind) {
        String hint = activity.getString(kind == Kind.LOGS
                ? R.string.hint_log_output_folder : R.string.hint_gpx_output_folder);
        AlertDialog folderDialog = new AlertDialog.Builder(activity)
                .setTitle(kind == Kind.LOGS ? R.string.title_log_output_folder : R.string.title_gpx_output_folder)
                .setMessage(activity.getString(R.string.format_output_folder, location(kind)) + "\n\n" + hint)
                .setView(activity.getLayoutInflater().inflate(R.layout.dialog_output_folder_actions,
                        new FrameLayout(activity), false))
                .setPositiveButton(R.string.action_choose_output_folder,
                        (dialog, which) -> requestAccess(activity, kind))
                .setNeutralButton(R.string.action_use_default_folder, (dialog, which) -> reset(kind))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        AboutDeferredDialogAction.configure(activity, folderDialog.findViewById(R.id.outputFolderClearButton),
                () -> confirmClear(kind, folderDialog));
        AboutDeferredDialogAction.configure(activity, folderDialog.findViewById(R.id.outputFolderOpenButton),
                () -> new AboutOutputFolderBrowser(activity, worker).open(kind));
    }

    private String location(Kind kind) {
        String label = AndroidOutputFolderAccess.label(activity, kind);
        return label.isEmpty() ? activity.getString(R.string.label_output_folder_default) : label;
    }

    private void confirmClear(Kind kind, AlertDialog folderDialog) {
        String selected = AppOutputFolderSettings.get(activity, kind);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.action_clear_output_folder)
                .setMessage(activity.getString(kind == Kind.LOGS ? R.string.hint_clear_log_output_folder
                        : R.string.hint_clear_gpx_output_folder, location(kind)))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_clear_output_folder, (dialog, which) -> {
                    folderDialog.dismiss();
                    clear(kind, selected);
                })
                .show();
    }

    private void clear(Kind kind, String selected) {
        if (kind == Kind.GPX) {
            AndroidRouteGpxActions.clearFolder(activity.getApplicationContext(), selected, this::onCleared);
        } else {
            worker.execute(() -> clearLogs(selected));
        }
    }

    private void clearLogs(String selected) {
        boolean cleared;
        try {
            AppLogger.clearOutputFolder(activity.getApplicationContext(), selected);
            cleared = true;
        } catch (IOException | RuntimeException e) {
            AppLogger.w("OutputFolders", "Could not clear log output folder", e);
            cleared = false;
        }
        onCleared(cleared);
    }

    private void onCleared(boolean cleared) {
        activity.runOnUiThread(() -> toast(cleared
                ? R.string.msg_output_folder_cleared : R.string.msg_output_folder_clear_failed));
    }

    static void requestAccess(Activity activity, Kind kind) {
        try {
            activity.startActivityForResult(AndroidOutputFolderAccess.picker(activity, kind),
                    kind == Kind.LOGS ? REQUEST_LOGS : REQUEST_GPX);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.msg_output_folder_picker_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void reset(Kind kind) {
        worker.execute(() -> {
            AppOutputFolderSettings.set(activity, kind, null);
            AndroidOutputFolderAccess.markAvailable(kind);
            refreshLogs(kind);
            activity.runOnUiThread(() -> toast(R.string.msg_output_folder_reset));
        });
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_LOGS && requestCode != REQUEST_GPX) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK && data != null) {
            Kind kind = requestCode == REQUEST_LOGS ? Kind.LOGS : Kind.GPX;
            worker.execute(() -> accept(kind, data));
        }
        return true;
    }

    private void accept(Kind kind, Intent data) {
        int message;
        try {
            AndroidOutputFolderAccess.accept(activity.getApplicationContext(), kind, data);
            refreshLogs(kind);
            message = R.string.msg_output_folder_saved;
        } catch (IOException | RuntimeException e) {
            AppLogger.w("OutputFolders", "Could not select output folder", e);
            message = R.string.msg_output_folder_failed;
        }
        int resultMessage = message;
        activity.runOnUiThread(() -> toast(resultMessage));
    }

    private void refreshLogs(Kind kind) {
        if (kind == Kind.LOGS) {
            AppLogger.refreshOutputFolder(activity.getApplicationContext());
        }
    }

    private void toast(int message) {
        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    void shutdown() {
        worker.shutdown();
    }
}
