package vibro.navigator.main;

import vibro.navigator.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.logging.AppLogger;

final class MainActivityProfilePicker {

    private static final int REQ_PICK_CUSTOM_PROFILE = 1001;
    private static final int REQ_PICK_CUSTOM_PROFILE_TREE = 1002;
    private static final String TAG = "MainProfilePicker";

    @NonNull
    private final Activity activity;
    @NonNull
    private final BRouterProfilesRepository profilesRepository;

    @Nullable
    private ProfileSpinnerController profileSpinnerController;

    MainActivityProfilePicker(
            @NonNull Activity activity,
            @NonNull BRouterProfilesRepository profilesRepository
    ) {
        this.activity = activity;
        this.profilesRepository = profilesRepository;
    }

    void attachProfileSpinnerController(@NonNull ProfileSpinnerController profileSpinnerController) {
        this.profileSpinnerController = profileSpinnerController;
    }

    void refreshProfiles() {
        ProfileSpinnerController controller = requireProfileSpinnerController();
        controller.refresh();
    }

    void startCustomProfilePicker() {
        if (!profilesRepository.hasPersistedProfilesTreeAccess(activity)) {
            startProfilesTreePicker();
            return;
        }
        startCustomProfileDocumentPicker();
    }

    private void startProfilesTreePicker() {
        Uri initialUri = profilesRepository.getCustomProfilePickerInitialUri(activity);
        Intent intent = AndroidDocumentAccess.openDocumentTreeIntent(initialUri);
        AppLogger.i(TAG, "Launching profiles tree picker initialUri=" + safe(initialUri));
        activity.startActivityForResult(intent, REQ_PICK_CUSTOM_PROFILE_TREE);
    }

    private void startCustomProfileDocumentPicker() {
        Uri initialUri = profilesRepository.getCustomProfilePickerInitialUri(activity);
        Intent intent = AndroidDocumentAccess.openDocumentIntent(initialUri);
        AppLogger.i(TAG, "Launching custom profile document picker initialUri=" + safe(initialUri));
        activity.startActivityForResult(intent, REQ_PICK_CUSTOM_PROFILE);
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_PICK_CUSTOM_PROFILE_TREE) {
            handleProfilesTreePickerResult(resultCode, data);
            return true;
        }
        if (requestCode == REQ_PICK_CUSTOM_PROFILE) {
            handleCustomProfilePickerResult(resultCode, data);
            return true;
        }
        return false;
    }

    private void handleProfilesTreePickerResult(int resultCode, @Nullable Intent data) {
        ProfileSpinnerController controller = requireProfileSpinnerController();
        if (resultCode != Activity.RESULT_OK || data == null) {
            controller.onCustomProfilePickerCancelled();
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            AppLogger.w(TAG, "Profiles tree picker returned without URI");
            controller.onCustomProfilePickerCancelled();
            return;
        }
        if (!AndroidDocumentAccess.persistReadPermission(activity, data, uri)) {
            controller.onCustomProfilePickerCancelled();
            return;
        }
        profilesRepository.saveProfilesTreeUri(activity, uri);
        controller.refresh();
        startCustomProfileDocumentPicker();
    }

    private void handleCustomProfilePickerResult(int resultCode, @Nullable Intent data) {
        ProfileSpinnerController controller = requireProfileSpinnerController();
        if (resultCode != Activity.RESULT_OK || data == null) {
            controller.onCustomProfilePickerCancelled();
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            AppLogger.w(TAG, "Custom profile picker returned without URI");
            controller.onCustomProfilePickerCancelled();
            return;
        }
        AndroidDocumentAccess.persistReadPermission(activity, data, uri);
        String displayName = AndroidDocumentAccess.displayName(activity, uri);
        String profileName = profilesRepository.normalizeProfileName(displayName);
        if (profileName == null) {
            AppLogger.w(TAG, "Custom profile picker returned a non-.brf file uri=" + uri
                    + " displayName=" + safe(displayName));
            Toast.makeText(activity, R.string.msg_invalid_custom_profile, Toast.LENGTH_SHORT).show();
            controller.onCustomProfilePickerCancelled();
            return;
        }
        profilesRepository.saveCustomProfile(activity, uri, profileName);
        controller.onCustomProfileSaved();
    }

    @NonNull
    private ProfileSpinnerController requireProfileSpinnerController() {
        if (profileSpinnerController == null) {
            throw new IllegalStateException("Profile spinner controller not attached");
        }
        return profileSpinnerController;
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }

    @NonNull
    private static String safe(@Nullable Uri value) {
        return value == null ? "null" : value.toString();
    }
}

