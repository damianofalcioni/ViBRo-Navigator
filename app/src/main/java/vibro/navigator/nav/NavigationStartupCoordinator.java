package vibro.navigator.nav;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.util.AppLogger;

public final class NavigationStartupCoordinator {

    public static final int REQUEST_PERMISSIONS = 2001;

    interface PreflightInspector {
        @NonNull
        NavigationPreflight.Status inspect(@NonNull Activity activity);
    }

    public interface Host {
        @NonNull
        Activity getActivity();

        @NonNull
        NavigationRequest getNavigationRequest();

        void requestPermissions(@NonNull String[] permissions, int requestCode);

        void showPermissionRationale(@NonNull String message, @NonNull Runnable onContinue);

        void showSettingsRedirectDialog(int messageResId, @NonNull Intent settingsIntent);

        void showBatteryOptimizationDialog(@NonNull Intent settingsIntent);

        void startNavigationService(@NonNull NavigationRequest request);
    }

    private static final String TAG = "NavStartup";

    @NonNull
    private final Host host;
    @NonNull
    private final PreflightInspector preflightInspector;

    private boolean autoStartNavigation;
    private boolean settingsLaunchInProgress;
    private boolean awaitingSettingsReturn;

    public NavigationStartupCoordinator(@NonNull Host host) {
        this(host, NavigationPreflight::inspect);
    }

    NavigationStartupCoordinator(@NonNull Host host, @NonNull PreflightInspector preflightInspector) {
        this.host = host;
        this.preflightInspector = preflightInspector;
    }

    public void setAutoStartNavigation(boolean autoStartNavigation) {
        this.autoStartNavigation = autoStartNavigation;
    }

    public boolean isAutoStartNavigation() {
        return autoStartNavigation;
    }

    public void ensureReadyThenStart() {
        if (!autoStartNavigation) {
            AppLogger.i(TAG, "Startup skipped because auto-start is disabled");
            return;
        }

        Activity activity = host.getActivity();
        NavigationPreflight.Status status = preflightInspector.inspect(activity);
        if (status.hasMissingPermissions()) {
            AppLogger.i(TAG, "Missing permissions=" + status.missingPermissions);
            requestMissingPermissions(activity, status);
            return;
        }

        if (!status.locationEnabled) {
            AppLogger.w(TAG, "Location services are disabled");
            host.showSettingsRedirectDialog(
                    R.string.msg_location_disabled,
                    NavigationPreflight.newLocationSettingsIntent()
            );
            return;
        }

        if (!status.notificationsEnabled) {
            AppLogger.w(TAG, "Notifications are disabled for the app");
            host.showSettingsRedirectDialog(
                    R.string.msg_enable_notifications,
                    NavigationPreflight.newNotificationSettingsIntent(activity)
            );
            return;
        }

        if (status.needsBatteryOptimizationExemption) {
            AppLogger.i(TAG, "Prompting for battery optimization exemption");
            host.showBatteryOptimizationDialog(NavigationPreflight.newBatteryOptimizationIntent(activity));
            return;
        } else {
            AppLogger.i(TAG, "Battery optimization exemption already granted");
        }

        NavigationRequest request = host.getNavigationRequest();
        AppLogger.i(TAG, "Environment checks passed, starting navigation service " + request.describe());
        host.startNavigationService(request);
        autoStartNavigation = false;
    }

    public boolean onRequestPermissionsResult(int requestCode) {
        if (requestCode != REQUEST_PERMISSIONS) {
            return false;
        }
        ensureReadyThenStart();
        return true;
    }

    public void onSettingsOpened() {
        settingsLaunchInProgress = true;
    }

    public void onPause() {
        if (!settingsLaunchInProgress) {
            return;
        }
        awaitingSettingsReturn = true;
        AppLogger.i(TAG, "Activity paused after opening external settings");
    }

    public void onResume() {
        if (!awaitingSettingsReturn) {
            return;
        }
        awaitingSettingsReturn = false;
        settingsLaunchInProgress = false;
        AppLogger.i(TAG, "Returned from external settings, rechecking startup preflight");
        ensureReadyThenStart();
    }

    private void requestMissingPermissions(
            @NonNull Activity activity,
            @NonNull NavigationPreflight.Status status
    ) {
        String[] permissions = status.missingPermissions.toArray(new String[0]);
        if (status.showPermissionRationale) {
            AppLogger.i(TAG, "Showing permission rationale for permissions=" + status.missingPermissions);
            host.showPermissionRationale(buildPermissionRationaleMessage(activity, status), () ->
                    host.requestPermissions(permissions, REQUEST_PERMISSIONS));
            return;
        }
        AppLogger.i(TAG, "Requesting permissions directly permissions=" + status.missingPermissions);
        host.requestPermissions(permissions, REQUEST_PERMISSIONS);
    }

    @NonNull
    private static String buildPermissionRationaleMessage(
            @NonNull Activity activity,
            @NonNull NavigationPreflight.Status status
    ) {
        String message = activity.getString(R.string.msg_permission_location_rationale);
        if (status.missingPermissions.contains(Manifest.permission.POST_NOTIFICATIONS)) {
            message = message + "\n\n" + activity.getString(R.string.msg_permission_notifications_rationale);
        }
        return message;
    }
}
