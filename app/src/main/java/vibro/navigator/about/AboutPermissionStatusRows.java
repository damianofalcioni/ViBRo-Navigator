package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterFolderAccess;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterProfilesStorageAccess;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.android.startup.AndroidNavigationPreflight;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.nav.startup.NavigationPreflight;

final class AboutPermissionStatusRows {
    static final int REQUEST_BROUTER_STORAGE = 3008;

    private static final String TAG = "AboutDiagnostics";

    @NonNull
    private final Activity activity;
    @NonNull
    private final AboutPermissionRow locationPermissionRow;
    @NonNull
    private final AboutPermissionRow locationServicesRow;
    @NonNull
    private final AboutPermissionRow notificationsRow;
    @NonNull
    private final AboutPermissionRow batteryOptimizationRow;
    @NonNull
    private final AboutPermissionRow brouterStorageRow;
    @NonNull
    private final BRouterProfilesRepository profilesRepository =
            AndroidBRouterProfilesRepositoryFactory.create();
    @NonNull
    private final BRouterSegmentsRepository segmentsRepository =
            AndroidBRouterSegmentsRepositoryFactory.create();

    AboutPermissionStatusRows(@NonNull Activity activity) {
        this.activity = activity;
        locationPermissionRow = new AboutPermissionRow(
                activity,
                R.id.aboutPermissionLocationRow,
                R.id.aboutPermissionLocationMark,
                R.id.aboutPermissionLocationLabel,
                R.id.aboutPermissionLocationStatus,
                AndroidNavigationPreflight::newAppDetailsSettingsIntent
        );
        locationServicesRow = new AboutPermissionRow(
                activity,
                R.id.aboutPermissionLocationServicesRow,
                R.id.aboutPermissionLocationServicesMark,
                R.id.aboutPermissionLocationServicesLabel,
                R.id.aboutPermissionLocationServicesStatus,
                ignored -> AndroidNavigationPreflight.newLocationSettingsIntent()
        );
        notificationsRow = new AboutPermissionRow(
                activity,
                R.id.aboutPermissionNotificationsRow,
                R.id.aboutPermissionNotificationsMark,
                R.id.aboutPermissionNotificationsLabel,
                R.id.aboutPermissionNotificationsStatus,
                AndroidNavigationPreflight::newNotificationSettingsIntent
        );
        batteryOptimizationRow = new AboutPermissionRow(
                activity,
                R.id.aboutPermissionBatteryRow,
                R.id.aboutPermissionBatteryMark,
                R.id.aboutPermissionBatteryLabel,
                R.id.aboutPermissionBatteryStatus,
                AboutPermissionStatusRows::newBatteryOptimizationIntent
        );
        brouterStorageRow = new AboutPermissionRow(
                activity,
                R.id.aboutPermissionBRouterStorageRow,
                R.id.aboutPermissionBRouterStorageMark,
                R.id.aboutPermissionBRouterStorageLabel,
                R.id.aboutPermissionBRouterStorageStatus,
                this::openBRouterStorageAccess
        );
    }

    void render() {
        NavigationPreflight.Status status = AndroidNavigationPreflight.inspect(activity);
        locationPermissionRow.render(status.hasLocationPermission());
        locationServicesRow.render(status.locationEnabled);
        notificationsRow.render(status.hasNotificationAccess());
        renderBatteryOptimization(status);
        renderBRouterStorage();
    }

    static boolean handlesTreeResult(int requestCode) {
        return requestCode == REQUEST_BROUTER_STORAGE;
    }

    boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (!handlesTreeResult(requestCode)) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK) {
            AndroidBRouterFolderAccess.saveSelection(activity, data, saved -> {
                if (!saved) {
                    showStorageRequiredToast(R.string.msg_brouter_folder_required);
                }
                render();
            });
        }
        render();
        return true;
    }

    boolean onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_BROUTER_STORAGE) {
            return false;
        }
        if (!AndroidLegacyExternalStorageAccess.isReadPermissionGranted(grantResults)) {
            showStorageRequiredToast(R.string.msg_brouter_profiles_storage_permission_required);
        }
        render();
        return true;
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

    private void renderBRouterStorage() {
        boolean installed = profilesRepository.isBRouterInstalled(activity);
        brouterStorageRow.setVisible(installed);
        if (installed) {
            brouterStorageRow.render(hasBRouterStorageAccess());
        }
    }

    private boolean hasBRouterStorageAccess() {
        if (AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()) {
            return AndroidLegacyExternalStorageAccess.hasReadPermission(activity);
        }
        return AndroidBRouterFolderAccess.hasUsableParent(activity)
                || (profilesRepository.hasPersistedProfilesTreeAccess(activity)
                        && segmentsRepository.hasPersistedSegmentsTreeAccess(activity));
    }

    private void openBRouterStorageAccess() {
        AndroidBRouterProfilesStorageAccess.request(
                activity,
                profilesRepository,
                REQUEST_BROUTER_STORAGE,
                TAG,
                () -> { },
                () -> {
                }
        );
    }

    private void showStorageRequiredToast(int messageId) {
        Toast.makeText(
                activity,
                messageId,
                Toast.LENGTH_SHORT
        ).show();
    }

}
