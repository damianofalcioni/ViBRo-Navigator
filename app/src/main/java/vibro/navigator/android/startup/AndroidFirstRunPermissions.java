package vibro.navigator.android.startup;

import androidx.core.util.Supplier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterStorageSetup;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.startup.NavigationPreflight;

/** Serializes first-run requests and keeps external requests intact across recreation. */
public final class AndroidFirstRunPermissions {
    public static final int REQUEST_RUNTIME = 1100;
    public static final int REQUEST_SETTINGS = 1101;
    private static final String STATE_STEP = "first_run_permission_step";
    private static final String STATE_LEGACY_PICKER = "first_run_legacy_folder_picker";
    private static final int LEGACY_PROFILES_PICKER = 1102;
    private static final String STATE_WAITING = "first_run_permission_waiting";
    private static final String STATE_DEFER_FOLDER_RESULT = "first_run_defer_folder_result";

    private final Activity activity;
    private final Runnable onComplete;
    private final AndroidBRouterStorageSetup storage;
    private final Step[] steps;
    private int step;
    private boolean waitingForResult;
    private final FolderContinuation folderContinuation;
    private AlertDialog settingsDialog;

    public AndroidFirstRunPermissions(Activity activity, Bundle state, Runnable onComplete) {
        this.activity = activity;
        this.onComplete = onComplete;
        storage = new AndroidBRouterStorageSetup(activity, state);
        steps = new Step[]{this::brouterStorage, this::runtime, this::location,
                this::notifications, this::battery};
        SavedStep saved = SavedStep.restore(state, storage.completedFirstRunStep());
        step = saved.index;
        waitingForResult = saved.waiting;
        folderContinuation = new FolderContinuation(state, saved, storage.completedFirstRunStep(), this::resumeSetup);
        storage.watchFirstRun(this::advanceAfterFolder, this::resumeAfterFolder);
    }

    public void start() {
        if (activity.isFinishing() || activity.isDestroyed() || waitingForResult || folderContinuation.deferred) {
            return;
        }
        while (step < steps.length) {
            if (steps[step].request()) {
                return;
            }
            step++;
        }
        onComplete.run();
    }

    private void next() {
        step++;
        resumeSetup();
    }

    private void resumeSetup() {
        waitingForResult = false;
        start();
    }

    private boolean brouterStorage() {
        return storage.requestForFirstRun(this::advanceAfterFolder);
    }

    private void advanceAfterFolder() {
        step++;
        resumeAfterFolder();
    }

    private void resumeAfterFolder() {
        folderContinuation.continueWhenResumed();
    }

    private boolean runtime() {
        String[] missing = AndroidFirstRunRuntimePermissions.missing(activity);
        if (missing.length == 0) {
            return false;
        }
        waitingForResult = true;
        ActivityCompat.requestPermissions(activity, missing, REQUEST_RUNTIME);
        return true;
    }

    private boolean location() {
        if (status().locationEnabled) {
            return false;
        }
        return explainSettings(R.string.msg_location_disabled, AndroidNavigationPreflight.newLocationSettingsIntent());
    }

    private boolean notifications() {
        // Android 13+ has just presented its notification permission prompt.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || status().notificationsEnabled) {
            return false;
        }
        return explainSettings(R.string.msg_enable_notifications,
                AndroidNavigationPreflight.newNotificationSettingsIntent(activity));
    }

    private boolean battery() {
        if (!status().needsBatteryOptimizationExemption) {
            return false;
        }
        return explainSettings(R.string.msg_battery_opt_rationale,
                AndroidNavigationPreflight.newBatteryOptimizationRequestIntent(activity));
    }

    private NavigationPreflight.Status status() {
        return AndroidNavigationPreflight.inspect(activity);
    }

    private boolean explainSettings(int message, Intent intent) {
        settingsDialog = new AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(R.string.action_continue, (dialog, which) -> launchSettings(intent))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> next())
                .setOnCancelListener(dialog -> next())
                .show();
        return true;
    }

    private void launchSettings(Intent intent) {
        try {
            waitingForResult = true;
            activity.startActivityForResult(intent, REQUEST_SETTINGS);
        } catch (ActivityNotFoundException | SecurityException error) {
            AppLogger.w("FirstRunPermissions", "Unable to open setup settings", error);
            next();
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (waitingForResult && requestCode == REQUEST_SETTINGS) {
            next();
            return true;
        }
        return folderContinuation.acceptLegacyResult(storage, requestCode, resultCode, data)
                || storage.handleActivityResult(requestCode, resultCode, data,
                this::advanceAfterFolder, this::resumeAfterFolder);
    }

    public boolean onRequestPermissionsResult(int requestCode) {
        if (requestCode != REQUEST_RUNTIME || !waitingForResult) {
            return false;
        }
        next();
        return true;
    }

    public void saveState(Bundle state) {
        state.putInt(STATE_STEP, step);
        state.putInt(STATE_LEGACY_PICKER, folderContinuation.legacyPicker);
        state.putBoolean(STATE_WAITING, waitingForResult);
        state.putBoolean(STATE_DEFER_FOLDER_RESULT, folderContinuation.deferred);
        storage.saveState(state);
    }

    public void setBRouterForegroundChecksEnabled(boolean enabled) {
        storage.setForegroundChecksEnabled(enabled);
    }

    public void setResumed(boolean resumed) {
        folderContinuation.setResumed(resumed);
        storage.setResumed(resumed);
    }

    public Supplier<Boolean> bindBRouterAction(View action, Runnable onPickerFinished,
                                               Supplier<Boolean> customPickerPending, Runnable onFolderPromptClosed) {
        storage.coordinatePicker(customPickerPending, onFolderPromptClosed);
        storage.bindManualAction(action, onPickerFinished);
        return storage::isPickerPending;
    }

    public ArrayList<String> skippedSegmentsFolders() {
        return storage.skippedSegmentsFolders();
    }

    public void dispose() {
        storage.dispose();
        if (settingsDialog != null) {
            settingsDialog.dismiss();
        }
    }

    private interface Step {
        boolean request();
    }

    private static final class FolderContinuation {
        private final Runnable resumeSetup;
        private boolean resumed;
        private boolean deferred;
        private int legacyPicker;

        FolderContinuation(Bundle state, SavedStep saved, boolean completedFolder, Runnable resumeSetup) {
            legacyPicker = saved.legacyPicker;
            this.resumeSetup = resumeSetup;
            deferred = completedFolder || (state != null && state.getBoolean(STATE_DEFER_FOLDER_RESULT));
        }

        boolean acceptLegacyResult(AndroidBRouterStorageSetup storage, int requestCode, int resultCode, Intent data) {
            if (legacyPicker == 0 || requestCode != legacyPicker) {
                return false;
            }
            legacyPicker = 0;
            storage.acceptLegacyPickerResult(requestCode == LEGACY_PROFILES_PICKER, resultCode, data);
            return true;
        }

        void continueWhenResumed() {
            if (resumed) {
                resumeSetup.run();
            } else {
                deferred = true;
            }
        }

        void setResumed(boolean resumed) {
            this.resumed = resumed;
            if (resumed && deferred) {
                deferred = false;
                resumeSetup.run();
            }
        }
    }

    private record SavedStep(int index, boolean waiting, int legacyPicker) {
        static SavedStep restore(Bundle state, boolean completedFolderStep) {
            if (state == null) {
                return new SavedStep(0, false, 0);
            }
            int index = state.getInt(STATE_STEP);
            boolean waiting = state.getBoolean(STATE_WAITING);
            if (AndroidBRouterStorageSetup.hasSharedPickerState(state)) {
                return new SavedStep(index + (completedFolderStep ? 1 : 0), waiting, state.getInt(STATE_LEGACY_PICKER));
            }
            // Old snapshots used two folder pickers. Recheck grants in the shared step.
            int legacy = waiting && index < 2 ? LEGACY_PROFILES_PICKER + index : 0;
            return new SavedStep(Math.max(0, index - 1), waiting && index >= 2, legacy);
        }
    }
}
