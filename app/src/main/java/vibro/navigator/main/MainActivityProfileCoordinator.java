package vibro.navigator.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.R;
import vibro.navigator.nav.model.NavigationRoutingMode;

final class MainActivityProfileCoordinator {
    private static final int REQUEST_STARTUP_PROFILES_STORAGE = 1003;

    @NonNull
    private final MainActivity activity;
    @NonNull
    private final BRouterProfilesRepository profilesRepository;
    @NonNull
    private final MainActivityProfilePicker profilePicker;
    @NonNull
    private final ProfileSpinnerController profileSpinnerController;
    @NonNull
    private final ProfileParameterSettingsController profileParameterSettingsController;
    private final MainActivityStartupCoordinator startup;

    private MainActivityProfileCoordinator(
            @NonNull MainActivity activity,
            @NonNull BRouterProfilesRepository profilesRepository,
            @NonNull MainActivityProfilePicker profilePicker,
            @NonNull ProfileSpinnerController profileSpinnerController,
            @NonNull ProfileParameterSettingsController profileParameterSettingsController
    ) {
        this.activity = activity;
        this.profilesRepository = profilesRepository;
        this.profilePicker = profilePicker;
        this.profileSpinnerController = profileSpinnerController;
        this.profileParameterSettingsController = profileParameterSettingsController;
        startup = new MainActivityStartupCoordinator(activity, this);
    }

    @NonNull
    static MainActivityProfileCoordinator configure(
            @NonNull MainActivity activity,
            @NonNull MainActivityControls controls
    ) {
        BRouterProfilesRepository profilesRepository = AndroidBRouterProfilesRepositoryFactory.create();
        MainActivityProfilePicker profilePicker = new MainActivityProfilePicker(activity, profilesRepository);
        ProfileSpinnerController profileSpinnerController = new ProfileSpinnerController(
                activity,
                controls.profileSpinner,
                profilesRepository,
                profilePicker::startCustomProfilePicker
        );
        ProfileParameterSettingsController profileParameterSettingsController =
                new ProfileParameterSettingsController(
                        activity,
                        controls.profileSettingsButton,
                        profilesRepository,
                        profileSpinnerController
                );
        profileSpinnerController.setSelectionChangeListener(
                profileParameterSettingsController::updateButtonState
        );
        profilePicker.attachProfileSpinnerController(profileSpinnerController);
        return new MainActivityProfileCoordinator(
                activity,
                profilesRepository,
                profilePicker,
                profileSpinnerController,
                profileParameterSettingsController
        );
    }

    boolean isBRouterInstalled() {
        return profilesRepository.isBRouterInstalled(activity);
    }

    ArrayList<String> skippedSegmentsFolders() {
        return startup.skippedSegmentsFolders();
    }

    void refresh() {
        profileSpinnerController.refresh();
    }

    static void refresh(@Nullable MainActivityProfileCoordinator coordinator) {
        if (coordinator != null) {
            coordinator.refresh();
            coordinator.startup.onResume();
        }
    }

    static void pauseStartup(@Nullable MainActivityProfileCoordinator coordinator) {
        if (coordinator != null) {
            coordinator.startup.onPause();
        }
    }

    static void startup(@Nullable MainActivityProfileCoordinator coordinator, @Nullable Bundle state,
                        @NonNull Runnable onReady) {
        if (coordinator != null) {
            coordinator.startup.start(state, onReady);
        }
    }

    static boolean isStartupPending(@Nullable MainActivityProfileCoordinator coordinator) {
        return coordinator != null && coordinator.startup.isPending();
    }

    static void saveStartupState(@Nullable MainActivityProfileCoordinator coordinator, @NonNull Bundle state) {
        if (coordinator != null) {
            coordinator.startup.saveState(state);
        }
    }

    static void disposeStartup(@Nullable MainActivityProfileCoordinator coordinator) {
        if (coordinator != null) {
            coordinator.profileSpinnerController.dispose();
            coordinator.startup.dispose();
        }
    }

    void showStartupPrompt(boolean enabled) {
        if (!enabled) {
            return;
        }
        if (!isBRouterInstalled()) {
            MainActivityBRouterInstallPrompt.show(activity);
        } else if (AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()
                && !AndroidLegacyExternalStorageAccess.hasReadPermission(activity)) {
            AndroidLegacyExternalStorageAccess.requestReadPermission(activity, REQUEST_STARTUP_PROFILES_STORAGE);
        }
    }

    @Nullable
    ProfileSelection resolveSelectedProfileSelection() {
        return profileSpinnerController.resolveSelectedProfileSelection();
    }

    void onRouteModeChanged(@NonNull NavigationRoutingMode mode) {
        profileParameterSettingsController.setProfileSelectionApplicable(mode != NavigationRoutingMode.STRAIGHT_LINE);
    }

    static boolean handleLaunchIntent(@Nullable MainActivityProfileCoordinator coordinator,
                                      @NonNull Intent intent, boolean recreated) {
        return coordinator != null && coordinator.profilePicker.handleLaunchIntent(intent, recreated);
    }

    static boolean handleActivityResult(@Nullable MainActivityProfileCoordinator coordinator,
                                        int requestCode, int resultCode, @Nullable Intent data) {
        return coordinator != null && coordinator.onActivityResult(requestCode, resultCode, data);
    }

    private boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (startup.onActivityResult(requestCode, resultCode, data)) {
            return true;
        }
        return profilePicker.handleActivityResult(requestCode, resultCode, data);
    }

    static boolean handlePermissionResult(
            @Nullable MainActivityProfileCoordinator coordinator,
            int requestCode,
            @NonNull int[] grantResults
    ) {
        if (coordinator == null) {
            return false;
        }
        return coordinator.startup.onRequestPermissionsResult(requestCode)
                || coordinator.handleLegacyStorageResult(requestCode, grantResults);
    }

    private boolean handleLegacyStorageResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_STARTUP_PROFILES_STORAGE) {
            return false;
        }
        if (AndroidLegacyExternalStorageAccess.isReadPermissionGranted(grantResults)) {
            profilePicker.refreshProfiles();
        } else {
            showStorageRequiredToast();
        }
        return true;
    }

    private void showStorageRequiredToast() {
        Toast.makeText(
                activity,
                R.string.msg_brouter_profiles_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }
}
