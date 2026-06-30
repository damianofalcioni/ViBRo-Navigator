package vibro.navigator.nav.startup;


import vibro.navigator.nav.model.NavigationRequest;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;

public final class NavigationStartupCoordinator {

    public static final int REQUEST_PERMISSIONS = 2001;

    public interface PreflightInspector {
        @NonNull
        NavigationPreflight.Status inspect();
    }

    public interface Host {
        @NonNull
        NavigationRequest getNavigationRequest();

        @NonNull
        String getString(int messageResId);

        void requestPermissions(@NonNull String[] permissions, int requestCode);

        void showPermissionRationale(@NonNull String message, @NonNull Runnable onContinue);

        void showSettingsRedirectDialog(
                int messageResId,
                @NonNull SettingsTarget settingsTarget,
                @NonNull Runnable onCancel
        );

        void showBatteryOptimizationDialog(@NonNull Runnable onContinue);

        void startNavigationService(@NonNull NavigationRequest request);

        void cancelNavigationStartup();
    }

    public enum SettingsTarget {
        LOCATION,
        NOTIFICATIONS
    }

    private static final String TAG = "NavStartup";

    @NonNull
    private final Host host;
    @NonNull
    private final PreflightInspector preflightInspector;

    private boolean autoStartNavigation;
    private boolean settingsLaunchInProgress;
    private boolean awaitingSettingsReturn;
    private boolean batteryOptimizationPromptShown;

    private enum StartupBlocker {
        LOCATION,
        NOTIFICATIONS
    }

    public NavigationStartupCoordinator(@NonNull Host host, @NonNull PreflightInspector preflightInspector) {
        this.host = host;
        this.preflightInspector = preflightInspector;
    }

    public void setAutoStartNavigation(boolean autoStartNavigation) {
        if (autoStartNavigation && !this.autoStartNavigation) {
            batteryOptimizationPromptShown = false;
        }
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

        NavigationPreflight.Status status = preflightInspector.inspect();
        if (status.hasMissingPermissions()) {
            AppLogger.i(TAG, "Missing permissions=" + status.missingPermissions);
            requestMissingPermissions(status);
            return;
        }

        if (!status.locationEnabled) {
            AppLogger.w(TAG, "Location services are disabled");
            host.showSettingsRedirectDialog(
                    R.string.msg_location_disabled,
                    SettingsTarget.LOCATION,
                    () -> onStartupBlockerCancelled(StartupBlocker.LOCATION)
            );
            return;
        }

        if (!status.notificationsEnabled) {
            AppLogger.w(TAG, "Notifications are disabled for the app");
            host.showSettingsRedirectDialog(
                    R.string.msg_enable_notifications,
                    SettingsTarget.NOTIFICATIONS,
                    () -> onStartupBlockerCancelled(StartupBlocker.NOTIFICATIONS)
            );
            return;
        }

        if (status.needsBatteryOptimizationExemption) {
            if (!batteryOptimizationPromptShown) {
                batteryOptimizationPromptShown = true;
                AppLogger.i(TAG, "Prompting for battery optimization exemption");
                host.showBatteryOptimizationDialog(this::continueAfterBatteryOptimizationPrompt);
                return;
            }
            AppLogger.i(TAG, "Battery optimization exemption still missing; continuing navigation startup");
        } else {
            AppLogger.i(TAG, "Battery optimization exemption already granted");
        }

        startNavigation();
    }

    private void continueAfterBatteryOptimizationPrompt() {
        if (!autoStartNavigation) {
            return;
        }
        AppLogger.i(TAG, "Continuing startup after battery optimization prompt");
        startNavigation();
    }

    private void startNavigation() {
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

    private void onStartupBlockerCancelled(@NonNull StartupBlocker blocker) {
        if (!autoStartNavigation) {
            return;
        }
        NavigationPreflight.Status status = preflightInspector.inspect();
        if (isStillBlockedBy(status, blocker)) {
            autoStartNavigation = false;
            settingsLaunchInProgress = false;
            awaitingSettingsReturn = false;
            AppLogger.i(TAG, "Navigation startup cancelled while blocker remains=" + blocker);
            host.cancelNavigationStartup();
            return;
        }
        AppLogger.i(TAG, "Startup blocker resolved while dialog was open blocker=" + blocker);
        ensureReadyThenStart();
    }

    private static boolean isStillBlockedBy(
            @NonNull NavigationPreflight.Status status,
            @NonNull StartupBlocker blocker
    ) {
        switch (blocker) {
            case LOCATION:
                return !status.locationEnabled;
            case NOTIFICATIONS:
                return !status.notificationsEnabled;
            default:
                return false;
        }
    }

    private void requestMissingPermissions(@NonNull NavigationPreflight.Status status) {
        String[] permissions = status.missingPermissions.toArray(new String[0]);
        if (status.showPermissionRationale) {
            AppLogger.i(TAG, "Showing permission rationale for permissions=" + status.missingPermissions);
            host.showPermissionRationale(buildPermissionRationaleMessage(host, status), () ->
                    host.requestPermissions(permissions, REQUEST_PERMISSIONS));
            return;
        }
        AppLogger.i(TAG, "Requesting permissions directly permissions=" + status.missingPermissions);
        host.requestPermissions(permissions, REQUEST_PERMISSIONS);
    }

    @NonNull
    private static String buildPermissionRationaleMessage(
            @NonNull Host host,
            @NonNull NavigationPreflight.Status status
    ) {
        List<String> messages = new ArrayList<>();
        if (isMissingLocationPermission(status)) {
            messages.add(host.getString(R.string.msg_permission_location_rationale));
        }
        if (status.missingPermissions.contains(NavigationPreflight.PERMISSION_POST_NOTIFICATIONS)) {
            messages.add(host.getString(R.string.msg_permission_notifications_rationale));
        }
        if (status.missingPermissions.contains(NavigationPreflight.PERMISSION_READ_EXTERNAL_STORAGE)) {
            messages.add(host.getString(R.string.msg_compass_surrounding_streets_storage_permission_required));
        }
        if (messages.isEmpty()) {
            return host.getString(R.string.msg_permission_location_rationale);
        }
        return joinMessages(messages);
    }

    private static boolean isMissingLocationPermission(@NonNull NavigationPreflight.Status status) {
        return status.missingPermissions.contains(NavigationPreflight.PERMISSION_FINE_LOCATION)
                || status.missingPermissions.contains(NavigationPreflight.PERMISSION_COARSE_LOCATION);
    }

    @NonNull
    private static String joinMessages(@NonNull List<String> messages) {
        StringBuilder out = new StringBuilder();
        for (String message : messages) {
            if (out.length() > 0) {
                out.append("\n\n");
            }
            out.append(message);
        }
        return out.toString();
    }
}
