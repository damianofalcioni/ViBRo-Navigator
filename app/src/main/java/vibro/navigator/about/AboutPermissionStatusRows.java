package vibro.navigator.about;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.nav.startup.NavigationPreflight;
import vibro.navigator.nav.startup.NavigationSettingsLauncher;

final class AboutPermissionStatusRows {

    @NonNull
    private final Activity activity;
    @NonNull
    private final PermissionRow locationPermissionRow;
    @NonNull
    private final PermissionRow locationServicesRow;
    @NonNull
    private final PermissionRow notificationsRow;
    @NonNull
    private final PermissionRow batteryOptimizationRow;

    AboutPermissionStatusRows(@NonNull Activity activity) {
        this.activity = activity;
        locationPermissionRow = new PermissionRow(
                activity,
                R.id.aboutPermissionLocationRow,
                R.id.aboutPermissionLocationMark,
                R.id.aboutPermissionLocationLabel,
                R.id.aboutPermissionLocationStatus,
                NavigationPreflight::newAppDetailsSettingsIntent
        );
        locationServicesRow = new PermissionRow(
                activity,
                R.id.aboutPermissionLocationServicesRow,
                R.id.aboutPermissionLocationServicesMark,
                R.id.aboutPermissionLocationServicesLabel,
                R.id.aboutPermissionLocationServicesStatus,
                ignored -> NavigationPreflight.newLocationSettingsIntent()
        );
        notificationsRow = new PermissionRow(
                activity,
                R.id.aboutPermissionNotificationsRow,
                R.id.aboutPermissionNotificationsMark,
                R.id.aboutPermissionNotificationsLabel,
                R.id.aboutPermissionNotificationsStatus,
                NavigationPreflight::newNotificationSettingsIntent
        );
        batteryOptimizationRow = new PermissionRow(
                activity,
                R.id.aboutPermissionBatteryRow,
                R.id.aboutPermissionBatteryMark,
                R.id.aboutPermissionBatteryLabel,
                R.id.aboutPermissionBatteryStatus,
                NavigationPreflight::newBatteryOptimizationIntent
        );
    }

    void render() {
        NavigationPreflight.Status status = NavigationPreflight.inspect(activity);
        locationPermissionRow.render(hasLocationPermission(status));
        locationServicesRow.render(status.locationEnabled);
        notificationsRow.render(hasNotificationAccess(status));
        batteryOptimizationRow.render(!status.needsBatteryOptimizationExemption);
    }

    private static boolean hasLocationPermission(@NonNull NavigationPreflight.Status status) {
        return !status.missingPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)
                && !status.missingPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private static boolean hasNotificationAccess(@NonNull NavigationPreflight.Status status) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return status.notificationsEnabled;
        }
        return !status.missingPermissions.contains(Manifest.permission.POST_NOTIFICATIONS)
                && status.notificationsEnabled;
    }

    private interface SettingsIntentFactory {
        @NonNull
        Intent create(@NonNull Activity activity);
    }

    private static final class PermissionRow {

        @NonNull
        private final Activity activity;
        @NonNull
        private final View rowView;
        @NonNull
        private final View markerView;
        @NonNull
        private final TextView labelView;
        @NonNull
        private final TextView statusView;

        PermissionRow(
                @NonNull Activity activity,
                int rowId,
                int markerId,
                int labelId,
                int statusId,
                @NonNull SettingsIntentFactory intentFactory
        ) {
            this.activity = activity;
            rowView = activity.findViewById(rowId);
            markerView = activity.findViewById(markerId);
            labelView = activity.findViewById(labelId);
            statusView = activity.findViewById(statusId);
            rowView.setOnClickListener(v -> openSettings(intentFactory.create(activity)));
        }

        void render(boolean allowed) {
            int statusResId = allowed
                    ? R.string.permission_status_ok
                    : R.string.permission_status_needs_attention;
            int colorResId = allowed ? R.color.success : R.color.danger;
            int markerResId = allowed
                    ? R.drawable.bg_permission_status_ok
                    : R.drawable.bg_permission_status_error;

            markerView.setBackgroundResource(markerResId);
            statusView.setText(statusResId);
            statusView.setTextColor(ContextCompat.getColor(activity, colorResId));
            rowView.setContentDescription(activity.getString(
                    R.string.format_about_permission_status_content_description,
                    labelView.getText(),
                    statusView.getText()
            ));
        }

        private void openSettings(@NonNull Intent intent) {
            if (!NavigationSettingsLauncher.launch(activity, intent)) {
                Toast.makeText(activity, R.string.msg_open_settings_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
