package vibro.navigator.nav.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.android.startup.AndroidNavigationPreflight;
import vibro.navigator.android.startup.AndroidNavigationSettingsLauncher;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.startup.NavigationStartupCoordinator;
import vibro.navigator.logging.AppLogger;

final class NavigationActivityStartupHost implements NavigationStartupCoordinator.Host {
    private static final String TAG = "NavigationActivity";

    @NonNull
    private final NavigationActivity activity;

    private NavigationActivityStartupHost(@NonNull NavigationActivity activity) {
        this.activity = activity;
    }

    @NonNull
    static NavigationStartupCoordinator createCoordinator(@NonNull NavigationActivity activity) {
        return new NavigationStartupCoordinator(
                new NavigationActivityStartupHost(activity),
                () -> AndroidNavigationPreflight.inspect(activity)
        );
    }

    @NonNull
    @Override
    public NavigationRequest getNavigationRequest() {
        return AndroidNavigationRequestIntentContract.fromIntent(activity.getIntent());
    }

    @NonNull
    @Override
    public String getString(int messageResId) {
        return activity.getString(messageResId);
    }

    @Override
    public void requestPermissions(@NonNull String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    @Override
    public void showPermissionRationale(@NonNull String message, @NonNull Runnable onContinue) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.msg_permission_required)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (d, w) -> onContinue.run())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void showSettingsRedirectDialog(
            int messageResId,
            @NonNull NavigationStartupCoordinator.SettingsTarget settingsTarget,
            @NonNull Runnable onCancel
    ) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.msg_permission_required)
                .setMessage(messageResId)
                .setPositiveButton(R.string.action_open_settings, (d, w) -> openSettings(settingsTarget))
                .setNegativeButton(android.R.string.cancel, (d, w) -> onCancel.run())
                .setOnCancelListener(d -> onCancel.run())
                .show();
    }

    @Override
    public void showBatteryOptimizationDialog(@NonNull Runnable onContinue) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.msg_permission_required)
                .setMessage(R.string.msg_battery_opt_rationale)
                .setPositiveButton(
                        R.string.action_open_settings,
                        (d, w) -> openBatteryOptimizationSettings(onContinue)
                )
                .setNegativeButton(android.R.string.cancel, (d, w) -> onContinue.run())
                .setOnCancelListener(d -> onContinue.run())
                .show();
    }

    @Override
    public void startNavigationService(@NonNull NavigationRequest request) {
        Intent start = new Intent(activity, NavigationService.class);
        start.setAction(NavigationService.ACTION_START);
        AndroidNavigationRequestIntentContract.putInto(start, request);
        AppLogger.i(TAG, "Starting foreground navigation service " + request.describe());
        ContextCompat.startForegroundService(activity, start);
    }

    @Override
    public void cancelNavigationStartup() {
        AppLogger.i(TAG, "Closing navigation screen after startup cancellation");
        activity.finish();
    }

    private void openSettings(@NonNull NavigationStartupCoordinator.SettingsTarget settingsTarget) {
        switch (settingsTarget) {
            case LOCATION:
                launchSettingsIntent(AndroidNavigationPreflight.newLocationSettingsIntent());
                return;
            case NOTIFICATIONS:
                openNotificationSettings();
                return;
            default:
                throw new IllegalArgumentException("Unsupported settings target=" + settingsTarget);
        }
    }

    private void openNotificationSettings() {
        Intent settingsIntent = AndroidNavigationPreflight.newNotificationSettingsIntent(activity);
        launchSettingsIntent(settingsIntent);
    }

    private void openBatteryOptimizationSettings(@NonNull Runnable onContinue) {
        Intent settingsIntent = AndroidNavigationPreflight.newBatteryOptimizationRequestIntent(activity);
        if (AndroidNavigationSettingsLauncher.launch(activity, settingsIntent)) {
            activity.onStartupSettingsOpened();
            onContinue.run();
            return;
        }
        Toast.makeText(
                activity,
                R.string.msg_open_settings_failed,
                Toast.LENGTH_SHORT
        ).show();
        onContinue.run();
    }

    private void launchSettingsIntent(@NonNull Intent settingsIntent) {
        if (AndroidNavigationSettingsLauncher.launch(activity, settingsIntent)) {
            activity.onStartupSettingsOpened();
            return;
        }
        Toast.makeText(
                activity,
                R.string.msg_open_settings_failed,
                Toast.LENGTH_SHORT
        ).show();
    }
}
