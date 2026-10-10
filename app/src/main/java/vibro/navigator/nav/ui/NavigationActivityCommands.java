package vibro.navigator.nav.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.export.AndroidRouteGpxActions;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.android.storage.AndroidOutputPermissionRequest;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

final class NavigationActivityCommands {
    private static final String TAG = "NavigationActivity";

    interface BinderProvider {
        @Nullable
        NavigationServiceBinder current();
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final BinderProvider binderProvider;
    private final AndroidOutputPermissionRequest outputAccess;
    private boolean exportReady;

    NavigationActivityCommands(@NonNull Activity activity, @NonNull BinderProvider binderProvider) {
        this.activity = activity;
        this.binderProvider = binderProvider;
        outputAccess = new AndroidOutputPermissionRequest(activity, (kind, allowed) -> {
            exportReady = allowed;
            exportWhenBound();
        });
    }

    void addBlockedWaypointFromUi() {
        NavigationServiceBinder binder = binderProvider.current();
        if (binder != null) {
            if (!binder.canAddBlockedWaypoint()) {
                AppLogger.w(TAG, "Blocked-road button tapped while its current action is unavailable");
                return;
            }
            AppLogger.i(TAG, "Blocked-road or beeline-skip action requested from UI");
            binder.addBlockedWaypoint();
        } else {
            AppLogger.w(TAG, "Blocked-road button tapped before service binding completed");
        }
    }

    void togglePausedFromUi() {
        NavigationServiceBinder binder = binderProvider.current();
        if (binder == null) {
            AppLogger.w(TAG, "Pause/resume tapped before service binding completed");
            return;
        }
        if (binder.isPaused()) {
            AppLogger.i(TAG, "Resume navigation requested from UI");
            binder.resume();
        } else {
            AppLogger.i(TAG, "Pause navigation requested from UI");
            binder.pause();
        }
    }

    void exportCurrentRouteFromUi() {
        if (binderProvider.current() == null) {
            showShortToast(R.string.msg_route_export_unavailable);
            return;
        }
        outputAccess.request(Kind.GPX);
    }

    void exportWhenBound() {
        if (exportReady && binderProvider.current() != null) {
            exportReady = false;
            exportGrantedRoute();
        }
    }

    private void exportGrantedRoute() {
        NavigationServiceBinder binder = binderProvider.current();
        if (binder == null) {
            AppLogger.w(TAG, "Route export tapped before service binding completed");
            showShortToast(R.string.msg_route_export_unavailable);
            return;
        }
        String gpx = binder.buildCurrentRouteGpx();
        if (gpx == null) {
            AppLogger.w(TAG, "Route export requested without an active route");
            showShortToast(R.string.msg_route_export_unavailable);
            return;
        }
        AndroidRouteGpxActions.export(activity, gpx);
    }

    boolean handlePermissionResult(int code) {
        return outputAccess.handlePermissionResult(code);
    }

    boolean handleActivityResult(int code, int resultCode, Intent data) {
        return outputAccess.handleActivityResult(code, resultCode, data);
    }

    void saveState(Bundle state) {
        outputAccess.saveState(state);
        state.putBoolean("route_export_ready", exportReady);
    }

    void restoreState(Bundle state) {
        outputAccess.restoreState(state);
        exportReady = state != null && state.getBoolean("route_export_ready");
    }

    void showStopNavigationConfirmation() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.title_stop_navigation_confirm)
                .setMessage(R.string.msg_stop_navigation_confirm)
                .setPositiveButton(R.string.action_stop_navigation, (dialog, which) -> {
                    NavigationServiceBinder binder = binderProvider.current();
                    if (binder != null) {
                        AppLogger.i(TAG, "Stop navigation requested from UI");
                        NavigationStopGpxAutoSave.saveIfEnabled(activity, binder::buildCurrentRouteGpx);
                        binder.stop();
                    } else {
                        AppLogger.w(TAG, "Stop navigation confirmed before service binding completed");
                    }
                    activity.finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showShortToast(int messageResId) {
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show();
    }
}
