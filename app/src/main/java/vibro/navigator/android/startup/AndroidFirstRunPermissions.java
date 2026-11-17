package vibro.navigator.android.startup;

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
    private static final String STATE_WAITING = "first_run_permission_waiting";

    private final Activity activity;
    private final Runnable onComplete;
    private final AndroidBRouterStorageSetup storage;
    private final Step[] steps;
    private int step;
    private boolean waitingForResult;
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
        storage.watchFirstRun(this::next, this::resumeSetup);
    }

    public void start() {
        if (activity.isFinishing() || activity.isDestroyed() || waitingForResult) {
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
        return storage.requestForFirstRun(this::next);
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
        return storage.handleActivityResult(requestCode, resultCode, data, this::next, this::resumeSetup);
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
        state.putBoolean(STATE_WAITING, waitingForResult);
        storage.saveState(state);
    }

    public void setBRouterForegroundChecksEnabled(boolean enabled) {
        storage.setForegroundChecksEnabled(enabled);
    }

    public void setResumed(boolean resumed) {
        storage.setResumed(resumed);
    }

    public void bindBRouterAction(View action, Runnable onPickerFinished) {
        storage.bindManualAction(action, onPickerFinished);
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

    private record SavedStep(int index, boolean waiting) {
        static SavedStep restore(Bundle state, boolean completedFolderStep) {
            if (state == null) {
                return new SavedStep(0, false);
            }
            int index = state.getInt(STATE_STEP);
            boolean waiting = state.getBoolean(STATE_WAITING);
            if (AndroidBRouterStorageSetup.hasSharedPickerState(state)) {
                return new SavedStep(index + (completedFolderStep ? 1 : 0), waiting);
            }
            // Old snapshots used two folder pickers. Recheck grants in the shared step.
            return new SavedStep(Math.max(0, index - 1), waiting && index >= 2);
        }
    }
}
