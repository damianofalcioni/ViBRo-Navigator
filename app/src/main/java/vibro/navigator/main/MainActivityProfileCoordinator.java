package vibro.navigator.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterProfilesTreeAccessPrompt;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.R;
import vibro.navigator.nav.model.NavigationRoutingMode;

final class MainActivityProfileCoordinator {
    private static final int REQUEST_STARTUP_PROFILES_TREE = 1003;
    private static final String TAG = "MainProfileCoordinator";

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
    private boolean waitingForStartupProfilesTree;
    private MainActivityWelcomeScreen welcomeScreen;

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

    void refresh(boolean brouterInstalled) {
        if (brouterInstalled) {
            profilePicker.refreshProfiles();
        } else {
            profileSpinnerController.refresh();
        }
    }

    // Welcome and BRouter setup share one startup gate so prompts cannot cover the introduction.
    static void startup(
            @Nullable MainActivityProfileCoordinator coordinator,
            @Nullable Bundle savedInstanceState,
            @NonNull Runnable onReady
    ) {
        if (coordinator == null) {
            return;
        }
        coordinator.welcomeScreen = MainActivityWelcomeScreen.showIfNeeded(
                coordinator.activity, savedInstanceState, () -> {
                    coordinator.showStartupPrompt(true);
                    onReady.run();
                });
        if (coordinator.welcomeScreen == null) {
            coordinator.showStartupPrompt(savedInstanceState == null);
            onReady.run();
        }
    }

    private void showStartupPrompt(boolean enabled) {
        if (!enabled) {
            return;
        }
        if (isBRouterInstalled()) {
            requestProfilesTreeAccessAtStartupIfNeeded(true);
        } else {
            MainActivityBRouterInstallPrompt.show(activity);
        }
    }

    static boolean isWelcomeShowing(@Nullable MainActivityProfileCoordinator coordinator) {
        return coordinator != null && coordinator.welcomeScreen != null && coordinator.welcomeScreen.isShowing();
    }

    static void saveWelcomeState(@Nullable MainActivityProfileCoordinator coordinator, @NonNull Bundle outState) {
        if (coordinator != null && coordinator.welcomeScreen != null) {
            coordinator.welcomeScreen.saveState(outState);
        }
    }

    static void disposeWelcome(@Nullable MainActivityProfileCoordinator coordinator) {
        if (coordinator != null && coordinator.welcomeScreen != null) {
            coordinator.welcomeScreen.dispose();
        }
    }

    void requestProfilesTreeAccessAtStartupIfNeeded(boolean startupPromptEnabled) {
        if (!startupPromptEnabled
                || !profilesRepository.isBRouterInstalled(activity)
                || profilesRepository.hasPersistedProfilesTreeAccess(activity)) {
            return;
        }
        AndroidBRouterProfilesTreeAccessPrompt.show(
                activity,
                profilesRepository,
                REQUEST_STARTUP_PROFILES_TREE,
                TAG,
                () -> waitingForStartupProfilesTree = true,
                () -> {
                }
        );
    }

    @Nullable
    ProfileSelection resolveSelectedProfileSelection() {
        return profileSpinnerController.resolveSelectedProfileSelection();
    }

    void onRouteModeChanged(@NonNull NavigationRoutingMode mode) {
        profileParameterSettingsController.setProfileSelectionApplicable(mode != NavigationRoutingMode.STRAIGHT_LINE);
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_STARTUP_PROFILES_TREE) {
            handleStartupProfilesTreeResult(resultCode, data);
            return true;
        }
        return profilePicker.handleActivityResult(requestCode, resultCode, data);
    }

    private void handleStartupProfilesTreeResult(int resultCode, @Nullable Intent data) {
        if (!waitingForStartupProfilesTree) {
            return;
        }
        waitingForStartupProfilesTree = false;
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null
                && AndroidDocumentAccess.persistReadPermission(activity, data, data.getData())) {
            profilesRepository.saveProfilesTreeUri(activity, data.getData());
            profilePicker.refreshProfiles();
            return;
        }
        Toast.makeText(
                activity,
                R.string.msg_brouter_profiles_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }
}
