package vibro.navigator.android.startup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import vibro.navigator.R;
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
    private final AndroidFirstRunStorageAccess storage;
    private final Step[] steps;
    private int step;
    private boolean waitingForResult;
    private AlertDialog settingsDialog;

    public AndroidFirstRunPermissions(Activity activity, Bundle state, Runnable onComplete) {
        this.activity = activity;
        this.onComplete = onComplete;
        storage = new AndroidFirstRunStorageAccess(activity);
        steps = new Step[]{this::profiles, this::segments, this::runtime, this::location,
                this::notifications, this::battery};
        if (state != null) {
            step = state.getInt(STATE_STEP);
            waitingForResult = state.getBoolean(STATE_WAITING);
        }
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
        waitingForResult = false;
        step++;
        start();
    }

    private boolean profiles() {
        return storage.requestProfiles(() -> waitingForResult = true, this::next);
    }

    private boolean segments() {
        return storage.requestSegments(() -> waitingForResult = true, this::next);
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
        if (!waitingForResult) {
            return false;
        }
        if (requestCode == REQUEST_SETTINGS || storage.handleResult(requestCode, resultCode, data)) {
            next();
            return true;
        }
        return false;
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
}
