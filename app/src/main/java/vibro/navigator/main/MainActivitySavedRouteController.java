package vibro.navigator.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.ui.PoiInputController;

final class MainActivitySavedRouteController {
    private static final String TAG = "MainSavedRoutes";

    @NonNull
    private final Activity activity;
    @NonNull
    private final PoiInputControllerProvider inputProvider;
    @NonNull
    private final MainActivityStopController stopController;
    @NonNull
    private final SavedRouteStore routeStore;

    interface PoiInputControllerProvider {
        @NonNull
        PoiInputController destinationController();
    }

    MainActivitySavedRouteController(
            @NonNull Activity activity,
            @NonNull PoiInputControllerProvider inputProvider,
            @NonNull MainActivityStopController stopController
    ) {
        this.activity = activity;
        this.inputProvider = inputProvider;
        this.stopController = stopController;
        routeStore = new SavedRouteStore(activity);
    }

    void configure(@NonNull View saveButton, @NonNull View restoreButton) {
        saveButton.setOnClickListener(v -> showSaveDialog());
        restoreButton.setOnClickListener(v -> showRestoreDialog());
    }

    private void showSaveDialog() {
        SavedRoutePoints points = SavedRouteFormReader.capture(
                activity,
                inputProvider.destinationController(),
                stopController.getStopControllers()
        );
        if (points == null) {
            return;
        }
        EditText input = SavedRouteDialogViews.routeNameEditText(activity, defaultRouteName());
        LinearLayout content = SavedRouteDialogViews.paddedContent(activity);
        content.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.title_save_route)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save_route, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> saveRoute(dialog, input, points)));
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        input.requestFocus();
    }

    private void saveRoute(
            @NonNull AlertDialog dialog,
            @NonNull EditText input,
            @NonNull SavedRoutePoints points
    ) {
        String routeName = input.getText().toString().trim();
        if (routeName.isEmpty()) {
            input.setError(activity.getString(R.string.msg_invalid_route_name));
            return;
        }
        routeStore.save(routeName, points.destination, points.stops);
        Toast.makeText(activity, R.string.msg_saved_route_saved, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }

    private void showRestoreDialog() {
        List<SavedRoute> routes = routeStore.list();
        if (routes.isEmpty()) {
            Toast.makeText(activity, R.string.msg_saved_routes_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        SavedRouteRestoreList restoreList = new SavedRouteRestoreList(activity, routeStore, routes);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.title_restore_route)
                .setView(restoreList.view())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> confirmSelectedRouteRestore(dialog, restoreList)));
        dialog.show();
    }

    private void confirmSelectedRouteRestore(
            @NonNull AlertDialog restoreDialog,
            @NonNull SavedRouteRestoreList restoreList
    ) {
        SavedRoute selectedRoute = restoreList.selectedRoute();
        if (selectedRoute == null) {
            Toast.makeText(activity, R.string.msg_saved_routes_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        confirmRestore(restoreDialog, selectedRoute);
    }

    private void confirmRestore(@NonNull AlertDialog restoreDialog, @NonNull SavedRoute route) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.title_restore_route_confirm)
                .setMessage(activity.getString(R.string.msg_restore_route_confirm, route.name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    restoreRoute(route);
                    restoreDialog.dismiss();
                })
                .show();
    }

    private void restoreRoute(@NonNull SavedRoute route) {
        AppLogger.i(TAG, "Restoring route id=" + route.id + " stopCount=" + route.stops.size());
        inputProvider.destinationController().restorePoi(route.destination);
        stopController.replaceStops(route.stops);
        Toast.makeText(activity, R.string.msg_saved_route_restored, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private String defaultRouteName() {
        return SavedRouteNameFormatter.defaultName(activity, System.currentTimeMillis());
    }
}
