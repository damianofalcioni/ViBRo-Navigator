package vibro.navigator.main;

import vibro.navigator.R;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.util.Supplier;

import vibro.navigator.android.brouter.AndroidBRouterProfilesTreeAccessPrompt;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidOutputStorageBrowser;
import vibro.navigator.logging.AppLogger;

final class MainActivityProfilePicker {

    private static final int REQ_PICK_CUSTOM_PROFILE = 1001;
    private static final String TAG = "MainProfilePicker";
    private static final String STATE_OPEN_PENDING = "custom_profile_picker_open_pending";
    private static final String STATE_PICKER_OPEN = "custom_profile_picker_open";

    @NonNull
    private final Activity activity;
    @NonNull
    private final BRouterProfilesRepository profilesRepository;

    @Nullable
    private ProfileSpinnerController profileSpinnerController;
    private boolean findingPickerLocation;
    private boolean pickerRequested;
    private boolean pickerOpen;
    private boolean resumed = true;
    private Runnable deferredPicker;
    private Supplier<Boolean> storagePickerPending = () -> false;

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
        controller.refreshAfterAccessChange();
    }

    void startCustomProfilePicker() {
        if (pickerOpen) {
            return;
        }
        pickerRequested = true;
        tryOpenPicker();
    }

    void setResumed(boolean resumed) {
        this.resumed = resumed;
        if (resumed) {
            tryOpenPicker();
        }
    }

    void restore(@Nullable Bundle state, Supplier<Boolean> storagePickerPending) {
        this.storagePickerPending = storagePickerPending;
        pickerRequested = state != null && state.getBoolean(STATE_OPEN_PENDING);
        pickerOpen = state != null && state.getBoolean(STATE_PICKER_OPEN);
    }

    void save(Bundle state) {
        state.putBoolean(STATE_OPEN_PENDING, pickerRequested);
        state.putBoolean(STATE_PICKER_OPEN, pickerOpen);
    }

    boolean isPickerRequested() {
        return pickerRequested || pickerOpen;
    }

    void tryOpenPicker() {
        if (!canOpenPicker()) {
            return;
        }
        if (deferredPicker != null) {
            Runnable show = deferredPicker;
            deferredPicker = null;
            pickerRequested = false;
            show.run();
        } else if (!findingPickerLocation) {
            findingPickerLocation = true;
            AndroidBRouterProfilesTreeAccessPrompt.findCustomProfileLocation(activity, profilesRepository, initialUri -> {
                findingPickerLocation = false;
                deferredPicker = () -> openPicker(initialUri);
                tryOpenPicker();
            });
        }
    }

    private boolean canOpenPicker() {
        return pickerRequested && resumed && !storagePickerPending.get()
                && !activity.isFinishing() && !activity.isDestroyed();
    }

    private void openPicker(@Nullable Uri initialUri) {
        Intent intent = AndroidDocumentAccess.openDocumentIntent(initialUri);
        AppLogger.i(TAG, "Launching custom profile document picker initialUri=" + safe(initialUri));
        try {
            pickerOpen = true;
            AndroidOutputStorageBrowser.openForResult(activity, intent, REQ_PICK_CUSTOM_PROFILE);
        } catch (ActivityNotFoundException | SecurityException e) {
            pickerOpen = false;
            AppLogger.w(TAG, "Could not open custom profile picker", e);
            Toast.makeText(activity, R.string.msg_output_viewer_unavailable, Toast.LENGTH_SHORT).show();
            requireProfileSpinnerController().onCustomProfilePickerCancelled();
        }
    }

    boolean handleLaunchIntent(@NonNull Intent intent, boolean recreated) {
        if (!AndroidOutputStorageBrowser.hasResult(intent)) {
            return false;
        }
        if (!recreated) {
            AndroidOutputStorageBrowser.dispatchResult(intent, this::handleActivityResult);
        }
        return true;
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_PICK_CUSTOM_PROFILE) {
            handleCustomProfilePickerResult(resultCode, data);
            return true;
        }
        return false;
    }

    private void handleCustomProfilePickerResult(int resultCode, @Nullable Intent data) {
        pickerOpen = false;
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

