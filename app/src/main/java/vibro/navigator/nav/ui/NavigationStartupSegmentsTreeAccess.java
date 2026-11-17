package vibro.navigator.nav.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;

import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterFolderAccess;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterPickerRequest;
import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPrompt;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.settings.AppCompassSettings;

final class NavigationStartupSegmentsTreeAccess {
    private static final String TAG = "NavigationActivity";
    private static final int REQUEST_SEGMENTS_TREE = 3005;
    private static final String STATE_STEP = "navigation_segments_access_step";
    private static final String STATE_OFFERED_FOLDER = "navigation_segments_access_folder";

    private enum Step {
        IDLE, CHECK_FOLDER, WAIT_FOR_PICKER, RETRY_FOLDER, CONTINUE;

        static Step restored(@Nullable Bundle state, @Nullable Completion completion) {
            if (completion != null) {
                return afterResult(completion);
            }
            return state == null ? IDLE : valueOf(state.getString(STATE_STEP, IDLE.name()));
        }

        static Step afterResult(Completion completion) {
            return completion == Completion.RETRY ? RETRY_FOLDER : CONTINUE;
        }

        @Nullable
        static Uri restoreOfferedFolder(@Nullable Bundle state) {
            return state == null ? null : BundleCompat.getParcelable(state, STATE_OFFERED_FOLDER, Uri.class);
        }
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final StorageNeeds storageNeeds = new StorageNeeds();
    @NonNull
    private final Runnable continueStartup;
    private final AndroidBRouterPickerRequest picker;
    private Step step = Step.IDLE;
    private boolean resumed;
    private boolean discovering;
    private int discoveryGeneration;
    @Nullable
    private Uri offeredFolder;
    private List<String> skippedFolders = Collections.emptyList();
    @Nullable
    private AlertDialog folderDialog;

    NavigationStartupSegmentsTreeAccess(@NonNull Activity activity, @NonNull Runnable continueStartup) {
        this.activity = activity;
        this.continueStartup = continueStartup;
        picker = new AndroidBRouterPickerRequest(activity, "navigation_segments_picker");
    }

    boolean restoreState(@Nullable Bundle state) {
        step = Step.restored(state, picker.restore(state));
        offeredFolder = Step.restoreOfferedFolder(state);
        picker.watch(this::applyCompletion);
        return step != Step.IDLE;
    }

    void saveState(@NonNull Bundle state) {
        state.putString(STATE_STEP, step.name());
        state.putParcelable(STATE_OFFERED_FOLDER, offeredFolder);
        picker.save(state);
    }

    void onResume() {
        resumed = true;
        advance();
    }

    void onPause() {
        resumed = false;
        cancelDiscovery();
    }

    boolean deferStartup(boolean autoStart, @Nullable List<String> skippedFolders) {
        if (!autoStart) {
            clear();
            return false;
        }
        this.skippedFolders = skippedFolders == null ? Collections.emptyList() : skippedFolders;
        if (step == Step.IDLE) {
            if (!storageNeeds.requiresGrant(activity)) {
                return false;
            }
            step = Step.CHECK_FOLDER;
        }
        advance();
        return true;
    }

    private void advance() {
        if (!canAdvance() || folderDialog != null || discovering) {
            return;
        }
        switch (step) {
            case CHECK_FOLDER -> checkFolder();
            case RETRY_FOLDER -> folderDialog = AndroidBRouterSegmentsTreeAccessPrompt.showRetry(
                    activity, offeredFolder, REQUEST_SEGMENTS_TREE, TAG, this::pickerOpened, this::finishCheck);
            case CONTINUE -> {
                step = Step.IDLE;
                continueStartup.run();
            }
            default -> { }
        }
    }

    private boolean canAdvance() {
        return resumed && !activity.isFinishing();
    }

    private void checkFolder() {
        discovering = true;
        int generation = ++discoveryGeneration;
        AndroidBRouterSegmentsTreeAccessPrompt.findAvailableFolder(activity, skippedFolders,
                uri -> onFolderFound(generation, uri));
    }

    private void onFolderFound(int generation, @Nullable Uri uri) {
        if (generation != discoveryGeneration || !canAdvance()) {
            return;
        }
        cancelDiscovery();
        if (!storageNeeds.canOffer(activity, uri)) {
            finishCheck();
            return;
        }
        offeredFolder = uri;
        folderDialog = AndroidBRouterSegmentsTreeAccessPrompt.showNow(
                activity,
                uri,
                REQUEST_SEGMENTS_TREE,
                TAG,
                this::pickerOpened,
                () -> {
                    denySurroundingStreetStorage();
                    finishCheck();
                },
                this::finishCheck
        );
    }

    private void pickerOpened() {
        picker.begin(REQUEST_SEGMENTS_TREE);
        step = Step.WAIT_FOR_PICKER;
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_SEGMENTS_TREE) {
            return false;
        }
        if (step != Step.WAIT_FOR_PICKER) {
            return true;
        }
        if (resultCode == Activity.RESULT_OK) {
            picker.beginValidation();
            AndroidBRouterFolderAccess.saveSelection(activity, data, this::onFolderSelection);
        } else {
            denySurroundingStreetStorage();
            picker.complete(Completion.ADVANCE);
            finishCheck();
        }
        return true;
    }

    private void onFolderSelection(boolean saved) {
        boolean canContinue = saved && !storageNeeds.requiresGrant(activity);
        picker.complete(canContinue ? Completion.ADVANCE : Completion.RETRY);
        if (step == Step.WAIT_FOR_PICKER) {
            applyCompletion(canContinue ? Completion.ADVANCE : Completion.RETRY);
        }
    }

    private void applyCompletion(Completion completion) {
        folderDialog = null;
        step = Step.afterResult(completion);
        advance();
    }

    private void finishCheck() {
        step = Step.CONTINUE;
        folderDialog = null;
        advance();
    }

    void clear() {
        cancelDiscovery();
        picker.stopWatching();
        step = Step.IDLE;
        if (folderDialog != null) {
            folderDialog.dismiss();
            folderDialog = null;
        }
    }

    private void denySurroundingStreetStorage() {
        AppCompassSettings.setSurroundingStreetsEnabled(activity, false);
        explainMissingStorage();
    }

    private void cancelDiscovery() {
        discoveryGeneration++;
        discovering = false;
    }

    private void explainMissingStorage() {
        Toast.makeText(
                activity,
                R.string.msg_compass_surrounding_streets_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }

    private static final class StorageNeeds {
        private final BRouterProfilesRepository profiles = AndroidBRouterProfilesRepositoryFactory.create();
        private final BRouterSegmentsRepository segments = AndroidBRouterSegmentsRepositoryFactory.create();

        boolean requiresGrant(Activity activity) {
            return profiles.isBRouterInstalled(activity)
                    && AppCompassSettings.isSurroundingStreetsEnabled(activity)
                    && !AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()
                    && !segments.hasPersistedSegmentsTreeAccess(activity);
        }

        boolean canOffer(Activity activity, @Nullable Uri uri) {
            return uri != null && requiresGrant(activity);
        }
    }
}
