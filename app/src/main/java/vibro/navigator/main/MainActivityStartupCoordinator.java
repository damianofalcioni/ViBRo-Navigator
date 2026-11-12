package vibro.navigator.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import vibro.navigator.android.startup.AndroidFirstRunPermissions;
import vibro.navigator.settings.AppMainUiSettings;

/** Keeps welcome, optional routing installation, and access requests in order. */
final class MainActivityStartupCoordinator {
    private static final int REQUEST_INSTALL = 1104;
    private static final String STATE_INSTALL_DONE = "first_run_install_done";
    private static final String STATE_INSTALL_WAITING = "first_run_install_waiting";
    private static final String STATE_INSTALL_LEFT = "first_run_install_left";

    private final MainActivity activity;
    private final MainActivityProfileCoordinator profiles;
    private Runnable onReady = () -> { };
    private AndroidFirstRunPermissions permissions;
    private MainActivityWelcomeScreen welcome;
    private AlertDialog installDialog;
    private boolean installDone;
    private boolean waitingForInstall;
    private boolean leftForInstall;
    private boolean ready;
    private boolean resumed;

    MainActivityStartupCoordinator(MainActivity activity, MainActivityProfileCoordinator profiles) {
        this.activity = activity;
        this.profiles = profiles;
        permissions = new AndroidFirstRunPermissions(activity, null, this::complete);
    }

    void start(Bundle state, Runnable onReady) {
        this.onReady = onReady;
        permissions = new AndroidFirstRunPermissions(activity, state, this::complete);
        if (state != null) {
            installDone = state.getBoolean(STATE_INSTALL_DONE);
            waitingForInstall = state.getBoolean(STATE_INSTALL_WAITING);
            leftForInstall = state.getBoolean(STATE_INSTALL_LEFT);
        }
        welcome = MainActivityWelcomeScreen.showIfNeeded(activity, state, this::firstRun);
        if (welcome != null) {
            return;
        }
        if (AppMainUiSettings.isFirstRunSetupPending(activity)) {
            firstRun();
        } else {
            profiles.showStartupPrompt(state == null);
            ready = true;
            onReady.run();
        }
    }

    private void firstRun() {
        if (waitingForInstall) {
            return;
        }
        if (installDone || profiles.isBRouterInstalled()) {
            afterInstall();
            return;
        }
        installDialog = MainActivityBRouterInstallPrompt.show(activity, REQUEST_INSTALL,
                () -> waitingForInstall = true, this::afterInstall);
    }

    private void afterInstall() {
        waitingForInstall = false;
        leftForInstall = false;
        installDone = true;
        permissions.start();
    }

    private void complete() {
        AppMainUiSettings.completeFirstRunSetup(activity);
        profiles.refresh();
        ready = true;
        onReady.run();
        permissions.setBRouterForegroundChecksEnabled(resumed);
    }

    boolean isPending() {
        return !ready;
    }

    boolean wasSegmentsFolderOffered() {
        return permissions.wasSegmentsFolderOffered();
    }

    boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_INSTALL && waitingForInstall) {
            // Stores can return RESULT_CANCELED immediately when opening another task.
            // Continue only after MainActivity has left and resumed in the foreground.
            return true;
        }
        return permissions.onActivityResult(requestCode, resultCode, data);
    }

    boolean onRequestPermissionsResult(int requestCode) {
        return permissions.onRequestPermissionsResult(requestCode);
    }

    void onPause() {
        resumed = false;
        permissions.setBRouterForegroundChecksEnabled(false);
        if (waitingForInstall) {
            leftForInstall = true;
        }
    }

    void onResume() {
        resumed = true;
        if (waitingForInstall && leftForInstall) {
            afterInstall();
        } else if (ready) {
            permissions.setBRouterForegroundChecksEnabled(true);
        }
    }

    void saveState(Bundle state) {
        if (welcome != null) {
            welcome.saveState(state);
        }
        state.putBoolean(STATE_INSTALL_DONE, installDone);
        state.putBoolean(STATE_INSTALL_WAITING, waitingForInstall);
        state.putBoolean(STATE_INSTALL_LEFT, leftForInstall);
        permissions.saveState(state);
    }

    void dispose() {
        if (welcome != null) {
            welcome.dispose();
        }
        if (installDialog != null) {
            installDialog.dismiss();
        }
        permissions.dispose();
    }
}
