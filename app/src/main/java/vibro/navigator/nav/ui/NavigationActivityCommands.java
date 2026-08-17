package vibro.navigator.nav.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import vibro.navigator.R;
import vibro.navigator.android.export.AndroidRouteGpxViewIntent;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.service.NavigationServiceBinder;

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

    NavigationActivityCommands(@NonNull Activity activity, @NonNull BinderProvider binderProvider) {
        this.activity = activity;
        this.binderProvider = binderProvider;
    }

    void addBlockedWaypointFromUi() {
        NavigationServiceBinder binder = binderProvider.current();
        if (binder != null) {
            if (!binder.canAddBlockedWaypoint()) {
                AppLogger.w(TAG, "Blocked-road button tapped while blocked-road rerouting is unavailable");
                return;
            }
            AppLogger.i(TAG, "Blocked-road reroute requested from UI");
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
        AppLogger.dMultiline(TAG, "Generated route GPX XML", gpx);
        try {
            activity.startActivity(AndroidRouteGpxViewIntent.createChooser(activity, gpx));
            AppLogger.i(TAG, "Route GPX chooser launched");
        } catch (ActivityNotFoundException e) {
            AppLogger.w(TAG, "No app can open exported GPX route", e);
            showShortToast(R.string.msg_route_export_no_app);
        } catch (IOException | RuntimeException e) {
            AppLogger.w(TAG, "Failed to export current route as GPX", e);
            showShortToast(R.string.msg_route_export_failed);
        }
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
