package vibro.navigator.main;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.brouter.BRouterProfilesRepository;

final class MainActivityProfileCoordinator {
    @NonNull
    private final MainActivity activity;
    @NonNull
    private final BRouterProfilesRepository profilesRepository;
    @NonNull
    private final MainActivityProfilePicker profilePicker;
    @NonNull
    private final ProfileSpinnerController profileSpinnerController;

    private MainActivityProfileCoordinator(
            @NonNull MainActivity activity,
            @NonNull BRouterProfilesRepository profilesRepository,
            @NonNull MainActivityProfilePicker profilePicker,
            @NonNull ProfileSpinnerController profileSpinnerController
    ) {
        this.activity = activity;
        this.profilesRepository = profilesRepository;
        this.profilePicker = profilePicker;
        this.profileSpinnerController = profileSpinnerController;
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
                profileSpinnerController
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

    @Nullable
    ProfileSelection resolveSelectedProfileSelection() {
        return profileSpinnerController.resolveSelectedProfileSelection();
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        return profilePicker.handleActivityResult(requestCode, resultCode, data);
    }
}
