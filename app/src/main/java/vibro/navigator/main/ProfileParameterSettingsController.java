package vibro.navigator.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterProfileLoader;
import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.logging.AppLogger;

final class ProfileParameterSettingsController {

    private static final String TAG = "ProfileParameterSettings";

    @NonNull
    private final Activity activity;
    @NonNull
    private final ImageButton button;
    @NonNull
    private final BRouterProfilesRepository profilesRepository;
    @NonNull
    private final ProfileSpinnerController profileSpinnerController;
    private boolean profileSelectionApplicable = true;
    private boolean loading;

    ProfileParameterSettingsController(
            @NonNull Activity activity,
            @NonNull ImageButton button,
            @NonNull BRouterProfilesRepository profilesRepository,
            @NonNull ProfileSpinnerController profileSpinnerController
    ) {
        this.activity = activity;
        this.button = button;
        this.profilesRepository = profilesRepository;
        this.profileSpinnerController = profileSpinnerController;
        button.setOnClickListener(v -> showSelectedProfileSettings());
        updateButtonState();
    }

    void updateButtonState() {
        boolean enabled = profileSelectionApplicable && !loading && selectedProfileName() != null;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.38f);
    }

    void setProfileSelectionApplicable(boolean applicable) {
        profileSelectionApplicable = applicable;
        updateButtonState();
    }

    private void showSelectedProfileSettings() {
        String profileName = selectedProfileName();
        ProfileSpinnerOption option = profileSpinnerController.selectedOption();
        if (profileName == null || option == null || loading) {
            return;
        }
        loading = true;
        updateButtonState();
        AndroidBRouterProfileLoader.loadParameters(activity, profilesRepository, profileName, option.isCustom(),
                parameters -> showLoadedParameters(profileName, option, parameters));
    }

    private void showLoadedParameters(String profileName, ProfileSpinnerOption option,
                                      @Nullable List<BRouterProfileParameter> parameters) {
        loading = false;
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        updateButtonState();
        if (!isStillSelected(option)) {
            return;
        }
        if (parameters == null) {
            Toast.makeText(activity, R.string.msg_brouter_profile_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        if (parameters.isEmpty()) {
            showNoParametersDialog();
            return;
        }
        ProfileParameterForm form = ProfileParameterForm.create(
                activity,
                parameters,
                profilesRepository.getProfileParameterValueOverrides(activity, profileName)
        );
        showParameterDialog(profileName, parameters, form);
    }

    private boolean isStillSelected(ProfileSpinnerOption option) {
        ProfileSpinnerOption selected = profileSpinnerController.selectedOption();
        return profileSelectionApplicable && selected != null && selected.selectionKey().equals(option.selectionKey());
    }

    private void showParameterDialog(
            @NonNull String profileName,
            @NonNull List<BRouterProfileParameter> parameters,
            @NonNull ProfileParameterForm form
    ) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.title_profile_parameters,
                        profileSpinnerController.selectedOption()))
                .setView(form.view())
                .create();
        form.view().findViewById(R.id.profileParameterCancelButton).setOnClickListener(v -> dialog.dismiss());
        form.view().findViewById(R.id.profileParameterSaveButton).setOnClickListener(v ->
                saveAndClose(dialog, profileName, parameters, form));
        form.view().findViewById(R.id.profileParameterResetButton).setOnClickListener(v ->
                confirmReset(profileName, parameters, form));
        dialog.show();
    }

    private void saveAndClose(
            @NonNull AlertDialog dialog,
            @NonNull String profileName,
            @NonNull List<BRouterProfileParameter> parameters,
            @NonNull ProfileParameterForm form
    ) {
        Map<String, String> values = form.collectChanges(activity);
        if (values == null) {
            return;
        }
        profilesRepository.saveProfileParameterValues(activity, profileName, parameters, values);
        Toast.makeText(activity, R.string.msg_profile_parameters_saved, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }

    private void confirmReset(String profileName, List<BRouterProfileParameter> parameters, ProfileParameterForm form) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.title_profile_parameters_reset_confirm)
                .setMessage(R.string.msg_profile_parameters_reset_confirm)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> reset(profileName, parameters, form))
                .show();
    }

    private void reset(String profileName, List<BRouterProfileParameter> parameters, ProfileParameterForm form) {
        profilesRepository.saveProfileParameterValues(activity, profileName, parameters, form.resetToDefaults());
        Toast.makeText(activity, R.string.msg_profile_parameters_reset, Toast.LENGTH_SHORT).show();
        AppLogger.i(TAG, "Reset profile parameters profile=" + profileName);
    }

    private void showNoParametersDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.title_profile_parameters,
                        profileSpinnerController.selectedOption()))
                .setMessage(R.string.msg_profile_parameters_empty)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Nullable
    private String selectedProfileName() {
        String profileName = profileSpinnerController.selectedBRouterProfileName();
        return profileName == null || profileName.trim().isEmpty() ? null : profileName;
    }
}
