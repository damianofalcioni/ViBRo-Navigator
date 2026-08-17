package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPrompt;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterProfilesTreeAccessPrompt;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.android.startup.AndroidNavigationPreflight;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.nav.startup.NavigationPreflight;
import vibro.navigator.settings.AppCompassSettings;

final class AboutPermissionStatusRows {
    static final int REQUEST_SURROUNDING_STREETS_STORAGE = 3006;
    static final int REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE = 3007;
    static final int REQUEST_PROFILES_TREE = 3008;

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
    private final AboutPermissionRow surroundingStreetStorageRow;
    @NonNull
    private final AboutPermissionRow profileStorageRow;
    @NonNull
    private final BRouterProfilesRepository profilesRepository =
            AndroidBRouterProfilesRepositoryFactory.create();
    @NonNull
    private final BRouterSegmentsRepository segmentsRepository =
            AndroidBRouterSegmentsRepositoryFactory.create();
    private boolean waitingForSurroundingStreetSegmentsTree;
    private boolean waitingForProfilesTree;

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
        surroundingStreetStorageRow = new AboutPermissionRow(
                activity,
                R.id.aboutPermissionSurroundingStreetStorageRow,
                R.id.aboutPermissionSurroundingStreetStorageMark,
                R.id.aboutPermissionSurroundingStreetStorageLabel,
                R.id.aboutPermissionSurroundingStreetStorageStatus,
                this::openSurroundingStreetStorageAccess
        );
        profileStorageRow = new AboutPermissionRow(
                activity,
                R.id.aboutPermissionProfileStorageRow,
                R.id.aboutPermissionProfileStorageMark,
                R.id.aboutPermissionProfileStorageLabel,
                R.id.aboutPermissionProfileStorageStatus,
                this::openProfileStorageAccess
        );
    }

    void render() {
        NavigationPreflight.Status status = AndroidNavigationPreflight.inspect(activity);
        locationPermissionRow.render(status.hasLocationPermission());
        locationServicesRow.render(status.locationEnabled);
        notificationsRow.render(status.hasNotificationAccess());
        renderBatteryOptimization(status);
        renderSurroundingStreetStorage();
        renderProfileStorage();
    }

    boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE) {
            return onSurroundingStreetSegmentsTreeActivityResult(resultCode, data);
        }
        if (requestCode == REQUEST_PROFILES_TREE) {
            return onProfilesTreeActivityResult(resultCode, data);
        }
        return false;
    }

    private boolean onSurroundingStreetSegmentsTreeActivityResult(int resultCode, @Nullable Intent data) {
        if (!waitingForSurroundingStreetSegmentsTree) {
            return true;
        }
        waitingForSurroundingStreetSegmentsTree = false;
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null
                && AndroidDocumentAccess.persistReadPermission(activity, data, data.getData())) {
            segmentsRepository.saveSegmentsTreeUri(activity, data.getData());
        } else {
            showSurroundingStreetStorageRequiredToast();
        }
        render();
        return true;
    }

    private boolean onProfilesTreeActivityResult(int resultCode, @Nullable Intent data) {
        if (!waitingForProfilesTree) {
            return true;
        }
        waitingForProfilesTree = false;
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null
                && AndroidDocumentAccess.persistReadPermission(activity, data, data.getData())) {
            profilesRepository.saveProfilesTreeUri(activity, data.getData());
        } else {
            showProfileStorageRequiredToast();
        }
        render();
        return true;
    }

    boolean onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_SURROUNDING_STREETS_STORAGE) {
            return false;
        }
        if (!AndroidLegacyExternalStorageAccess.isReadPermissionGranted(grantResults)) {
            showSurroundingStreetStorageRequiredToast();
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

    private void renderSurroundingStreetStorage() {
        if (!isBRouterInstalled()) {
            surroundingStreetStorageRow.setVisible(false);
            return;
        }
        boolean hasAccess = hasSurroundingStreetStorageAccess();
        boolean visible = shouldShowSurroundingStreetStorageStatus();
        surroundingStreetStorageRow.setVisible(visible);
        if (visible) {
            surroundingStreetStorageRow.render(hasAccess);
        }
    }

    private void renderProfileStorage() {
        if (!isBRouterInstalled()) {
            profileStorageRow.setVisible(false);
            return;
        }
        profileStorageRow.setVisible(true);
        profileStorageRow.render(profilesRepository.hasPersistedProfilesTreeAccess(activity));
    }

    private boolean shouldShowSurroundingStreetStorageStatus() {
        if (AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()) {
            return AppCompassSettings.isSurroundingStreetsEnabled(activity);
        }
        return true;
    }

    private boolean hasSurroundingStreetStorageAccess() {
        if (AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()) {
            return AndroidLegacyExternalStorageAccess.hasReadPermission(activity);
        }
        return segmentsRepository.hasPersistedSegmentsTreeAccess(activity);
    }

    private void openSurroundingStreetStorageAccess() {
        if (AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()) {
            AndroidLegacyExternalStorageAccess.requestReadPermission(activity, REQUEST_SURROUNDING_STREETS_STORAGE);
            return;
        }
        AndroidBRouterSegmentsTreeAccessPrompt.show(
                activity,
                segmentsRepository,
                REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE,
                TAG,
                () -> waitingForSurroundingStreetSegmentsTree = true,
                () -> {
                }
        );
    }

    private void openProfileStorageAccess() {
        AndroidBRouterProfilesTreeAccessPrompt.show(
                activity,
                profilesRepository,
                REQUEST_PROFILES_TREE,
                TAG,
                () -> waitingForProfilesTree = true,
                () -> {
                }
        );
    }

    private void showSurroundingStreetStorageRequiredToast() {
        Toast.makeText(
                activity,
                R.string.msg_compass_surrounding_streets_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }

    private boolean isBRouterInstalled() {
        return profilesRepository.isBRouterInstalled(activity);
    }

    private void showProfileStorageRequiredToast() {
        Toast.makeText(
                activity,
                R.string.msg_brouter_profiles_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }

}
