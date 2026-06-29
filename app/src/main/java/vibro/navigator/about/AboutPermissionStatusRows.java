package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.android.startup.AndroidNavigationSettingsLauncher;
import vibro.navigator.android.startup.AndroidNavigationPreflight;
import vibro.navigator.nav.startup.NavigationPreflight;
import vibro.navigator.settings.AppCompassSettings;

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
    @NonNull
    private final PermissionRow surroundingStreetStorageRow;

    AboutPermissionStatusRows(@NonNull Activity activity) {
        this.activity = activity;
        locationPermissionRow = new PermissionRow(
                activity,
                R.id.aboutPermissionLocationRow,
                R.id.aboutPermissionLocationMark,
                R.id.aboutPermissionLocationLabel,
                R.id.aboutPermissionLocationStatus,
                AndroidNavigationPreflight::newAppDetailsSettingsIntent
        );
        locationServicesRow = new PermissionRow(
                activity,
                R.id.aboutPermissionLocationServicesRow,
                R.id.aboutPermissionLocationServicesMark,
                R.id.aboutPermissionLocationServicesLabel,
                R.id.aboutPermissionLocationServicesStatus,
                ignored -> AndroidNavigationPreflight.newLocationSettingsIntent()
        );
        notificationsRow = new PermissionRow(
                activity,
                R.id.aboutPermissionNotificationsRow,
                R.id.aboutPermissionNotificationsMark,
                R.id.aboutPermissionNotificationsLabel,
                R.id.aboutPermissionNotificationsStatus,
                AndroidNavigationPreflight::newNotificationSettingsIntent
        );
        batteryOptimizationRow = new PermissionRow(
                activity,
                R.id.aboutPermissionBatteryRow,
                R.id.aboutPermissionBatteryMark,
                R.id.aboutPermissionBatteryLabel,
                R.id.aboutPermissionBatteryStatus,
                AboutPermissionStatusRows::newBatteryOptimizationIntent
        );
        surroundingStreetStorageRow = new PermissionRow(
                activity,
                R.id.aboutPermissionSurroundingStreetStorageRow,
                R.id.aboutPermissionSurroundingStreetStorageMark,
                R.id.aboutPermissionSurroundingStreetStorageLabel,
                R.id.aboutPermissionSurroundingStreetStorageStatus,
                AndroidNavigationPreflight::newAppDetailsSettingsIntent
        );
    }

    void render() {
        NavigationPreflight.Status status = AndroidNavigationPreflight.inspect(activity);
        locationPermissionRow.render(status.hasLocationPermission());
        locationServicesRow.render(status.locationEnabled);
        notificationsRow.render(status.hasNotificationAccess());
        renderBatteryOptimization(status);
        renderSurroundingStreetStorage();
    }

    private void renderBatteryOptimization(@NonNull NavigationPreflight.Status status) {
        if (status.needsBatteryOptimizationExemption) {
            batteryOptimizationRow.renderWarningKo();
            return;
        }
        batteryOptimizationRow.render(true);
    }

    @NonNull
    private static Intent newBatteryOptimizationIntent(@NonNull Activity activity) {
        NavigationPreflight.Status status = AndroidNavigationPreflight.inspect(activity);
        if (status.needsBatteryOptimizationExemption) {
            return AndroidNavigationPreflight.newBatteryOptimizationRequestIntent(activity);
        }
        return AndroidNavigationPreflight.newBatteryOptimizationSettingsIntent(activity);
    }

    private void renderSurroundingStreetStorage() {
        boolean visible = AppCompassSettings.isSurroundingStreetsEnabled(activity)
                && AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant();
        surroundingStreetStorageRow.setVisible(visible);
        if (visible) {
            surroundingStreetStorageRow.render(AndroidLegacyExternalStorageAccess.hasReadPermission(activity));
        }
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

            renderStatus(statusResId, colorResId, markerResId);
        }

        void renderWarningKo() {
            renderStatus(
                    R.string.permission_status_needs_attention,
                    R.color.warning,
                    R.drawable.bg_permission_status_warning
            );
        }

        private void renderStatus(int statusResId, int colorResId, int markerResId) {
            markerView.setBackgroundResource(markerResId);
            statusView.setText(statusResId);
            statusView.setTextColor(ContextCompat.getColor(activity, colorResId));
            rowView.setContentDescription(activity.getString(
                    R.string.format_about_permission_status_content_description,
                    labelView.getText(),
                    statusView.getText()
            ));
        }

        void setVisible(boolean visible) {
            rowView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        private void openSettings(@NonNull Intent intent) {
            if (!AndroidNavigationSettingsLauncher.launch(activity, intent)) {
                Toast.makeText(activity, R.string.msg_open_settings_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
