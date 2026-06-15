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
        boolean enabled = selectedProfileName() != null;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.38f);
    }

    private void showSelectedProfileSettings() {
        String profileName = selectedProfileName();
        if (profileName == null) {
            return;
        }
        List<BRouterProfileParameter> parameters = profilesRepository.getProfileParameters(activity, profileName);
        if (parameters.isEmpty()) {
            showNoParametersDialog(profileName);
            return;
        }
        ProfileParameterForm form = ProfileParameterForm.create(
                activity,
                parameters,
                profilesRepository.getProfileParameterValueOverrides(activity, profileName)
        );
        showParameterDialog(profileName, parameters, form);
    }

    private void showParameterDialog(
            @NonNull String profileName,
            @NonNull List<BRouterProfileParameter> parameters,
            @NonNull ProfileParameterForm form
    ) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.title_profile_parameters, profileName))
                .setView(form.view())
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(R.string.action_reset_defaults, null)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnShowListener(shown -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
                    saveAndClose(dialog, profileName, parameters, form));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                    confirmReset(profileName, form));
        });
        dialog.show();
    }

    private void saveAndClose(
            @NonNull AlertDialog dialog,
            @NonNull String profileName,
            @NonNull List<BRouterProfileParameter> parameters,
            @NonNull ProfileParameterForm form
    ) {
        Map<String, String> values = form.collectValues(activity);
        if (values == null) {
            return;
        }
        profilesRepository.saveProfileParameterValues(activity, profileName, parameters, values);
        Toast.makeText(activity, R.string.msg_profile_parameters_saved, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }

    private void confirmReset(@NonNull String profileName, @NonNull ProfileParameterForm form) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.title_profile_parameters_reset_confirm)
                .setMessage(R.string.msg_profile_parameters_reset_confirm)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> reset(profileName, form))
                .show();
    }

    private void reset(@NonNull String profileName, @NonNull ProfileParameterForm form) {
        profilesRepository.resetProfileParameterValues(activity, profileName);
        form.resetToDefaults();
        Toast.makeText(activity, R.string.msg_profile_parameters_reset, Toast.LENGTH_SHORT).show();
        AppLogger.i(TAG, "Reset profile parameters profile=" + profileName);
    }

    private void showNoParametersDialog(@NonNull String profileName) {
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.title_profile_parameters, profileName))
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
