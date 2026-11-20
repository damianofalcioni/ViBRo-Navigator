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

import androidx.core.util.Consumer;

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
    private static final String STATE_RETRY_PICKER = "navigation_segments_retry_picker";
    private static final String STATE_OFFERED_FOLDER = "navigation_segments_access_folder";

    private enum Step {
        IDLE, CHECK_FOLDER, OFFER_FOLDER, WAIT_FOR_PICKER, RETRY_FOLDER, CONTINUE;

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
    private final PendingPicker pendingPicker;
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
        pendingPicker = new PendingPicker(activity);
    }

    boolean restoreState(@Nullable Bundle state) {
        step = Step.restored(state, pendingPicker.restore(state, this::applyCompletion));
        offeredFolder = Step.restoreOfferedFolder(state);
        return step != Step.IDLE;
    }

    void saveState(@NonNull Bundle state) {
        state.putString(STATE_STEP, step.name());
        state.putParcelable(STATE_OFFERED_FOLDER, offeredFolder);
        pendingPicker.save(state);
    }

    void onResume() {
        resumed = true;
        advance();
    }

    void onPause() {
        resumed = false;
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
        if (!canAdvance() || discovering) {
            return;
        }
        switch (step) {
            case CHECK_FOLDER -> checkFolder();
            case OFFER_FOLDER -> showFoundFolder();
            case RETRY_FOLDER -> retryIfStillNeeded();
            case CONTINUE -> {
                step = Step.IDLE;
                continueStartup.run();
            }
            default -> { }
        }
    }

    private void retryIfStillNeeded() {
        if (!storageNeeds.requiresGrant(activity)) {
            finishCheck();
            return;
        }
        folderDialog = AndroidBRouterSegmentsTreeAccessPrompt.showRetry(
                activity, offeredFolder, REQUEST_SEGMENTS_TREE, TAG, this::pickerOpened, this::finishCheck);
    }

    private boolean canAdvance() {
        return resumed && folderDialog == null && !activity.isFinishing();
    }

    private void checkFolder() {
        discovering = true;
        int generation = ++discoveryGeneration;
        AndroidBRouterSegmentsTreeAccessPrompt.findAvailableFolder(activity, skippedFolders,
                uri -> onFolderFound(generation, uri));
    }

    private void onFolderFound(int generation, @Nullable Uri uri) {
        if (generation != discoveryGeneration) {
            return;
        }
        cancelDiscovery();
        offeredFolder = uri;
        step = Step.OFFER_FOLDER;
        advance();
    }

    private void showFoundFolder() {
        if (!storageNeeds.canOffer(activity, offeredFolder, skippedFolders)) {
            finishCheck();
            return;
        }
        folderDialog = AndroidBRouterSegmentsTreeAccessPrompt.showNow(
                activity,
                offeredFolder,
                REQUEST_SEGMENTS_TREE,
                TAG,
                this::pickerOpened,
                this::declineFolder,
                this::finishCheck
        );
    }

    private void declineFolder() {
        storageNeeds.onDecline(activity, this::denySurroundingStreetStorage);
        finishCheck();
    }

    private void pickerOpened() {
        pendingPicker.begin(step == Step.RETRY_FOLDER);
        step = Step.WAIT_FOR_PICKER;
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_SEGMENTS_TREE) {
            return false;
        }
        if (step == Step.WAIT_FOR_PICKER) {
            pendingPicker.accept(resultCode, data, storageNeeds, this::denySurroundingStreetStorage, this::onFolderResult);
        }
        return true;
    }

    private void onFolderResult(Completion completion) {
        if (step == Step.WAIT_FOR_PICKER) {
            applyCompletion(completion);
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
        pendingPicker.stopWatching();
        step = Step.IDLE;
        if (folderDialog != null) {
            folderDialog.dismiss();
            folderDialog = null;
        }
    }

    private void denySurroundingStreetStorage() {
        AppCompassSettings.setSurroundingStreetsEnabled(activity, false);
        Toast.makeText(
                activity,
                R.string.msg_compass_surrounding_streets_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void cancelDiscovery() {
        discoveryGeneration++;
        discovering = false;
    }

    private static final class PendingPicker {
        private final Activity activity;
        private final AndroidBRouterPickerRequest picker;
        private boolean retry;

        PendingPicker(Activity activity) {
            this.activity = activity;
            picker = new AndroidBRouterPickerRequest(activity, "navigation_segments_picker");
        }

        @Nullable
        Completion restore(@Nullable Bundle state, Consumer<Completion> onCompletion) {
            retry = state != null && state.getBoolean(STATE_RETRY_PICKER);
            Completion outcome = picker.restore(state);
            picker.watch(onCompletion);
            return outcome;
        }

        void save(Bundle state) {
            state.putBoolean(STATE_RETRY_PICKER, retry);
            picker.save(state);
        }

        void begin(boolean retry) {
            this.retry = retry;
            picker.begin(REQUEST_SEGMENTS_TREE);
        }

        void stopWatching() {
            picker.stopWatching();
        }

        void accept(int resultCode, @Nullable Intent data, StorageNeeds needs, Runnable onDeny,
                    Consumer<Completion> onCompletion) {
            if (resultCode == Activity.RESULT_OK) {
                picker.beginValidation();
                AndroidBRouterFolderAccess.saveStreetSelection(activity, data, saved ->
                        finish(needs.requiresGrant(activity) ? Completion.RETRY : Completion.ADVANCE, onCompletion));
                return;
            }
            if (!retry && needs.requiresGrant(activity)) {
                onDeny.run();
            }
            finish(Completion.ADVANCE, onCompletion);
        }

        private void finish(Completion outcome, Consumer<Completion> onCompletion) {
            picker.complete(outcome);
            onCompletion.accept(outcome);
        }
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

        boolean canOffer(Activity activity, @Nullable Uri uri, List<String> skippedFolders) {
            return uri != null && !skippedFolders.contains(uri.toString()) && requiresGrant(activity);
        }

        void onDecline(Activity activity, Runnable onDeny) {
            if (requiresGrant(activity)) {
                onDeny.run();
            }
        }
    }
}
