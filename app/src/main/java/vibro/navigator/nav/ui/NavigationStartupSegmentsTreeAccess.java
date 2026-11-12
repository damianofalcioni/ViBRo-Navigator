package vibro.navigator.nav.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterFolderAccess;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterPickerRequest;
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

    private enum Step {
        IDLE, CHECK_FOLDER, WAIT_FOR_PICKER, CONTINUE
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final BRouterProfilesRepository profilesRepository =
            AndroidBRouterProfilesRepositoryFactory.create();
    @NonNull
    private final BRouterSegmentsRepository segmentsRepository =
            AndroidBRouterSegmentsRepositoryFactory.create();
    @NonNull
    private final Runnable continueStartup;
    private final AndroidBRouterPickerRequest picker;
    private Step step = Step.IDLE;
    private boolean resumed;
    private boolean discovering;
    private int discoveryGeneration;
    @Nullable
    private String skippedFolder;
    @Nullable
    private AlertDialog folderDialog;

    NavigationStartupSegmentsTreeAccess(@NonNull Activity activity, @NonNull Runnable continueStartup) {
        this.activity = activity;
        this.continueStartup = continueStartup;
        picker = new AndroidBRouterPickerRequest(activity, "navigation_segments_picker");
    }

    boolean restoreState(@Nullable Bundle state) {
        if (state != null) {
            step = Step.valueOf(state.getString(STATE_STEP, Step.IDLE.name()));
        }
        if (picker.restore(state)) {
            step = Step.CONTINUE;
        }
        return step != Step.IDLE;
    }

    void saveState(@NonNull Bundle state) {
        state.putString(STATE_STEP, step.name());
        picker.save(state);
    }

    void onResume() {
        resumed = true;
        if (step == Step.WAIT_FOR_PICKER && !needsFolderGrant()) {
            // A result can change the stored grant or setting after Android saves instance state.
            step = Step.CONTINUE;
        }
        advance();
    }

    void onPause() {
        resumed = false;
        cancelDiscovery();
    }

    boolean deferStartup(boolean autoStart, @Nullable String skippedFolder) {
        if (!autoStart) {
            clear();
            return false;
        }
        this.skippedFolder = skippedFolder;
        if (step == Step.IDLE) {
            if (!needsFolderGrant()) {
                return false;
            }
            step = Step.CHECK_FOLDER;
        }
        advance();
        return true;
    }

    private boolean needsFolderGrant() {
        return profilesRepository.isBRouterInstalled(activity)
                && AppCompassSettings.isSurroundingStreetsEnabled(activity)
                && !AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()
                && !segmentsRepository.hasPersistedSegmentsTreeAccess(activity);
    }

    private void advance() {
        if (!canAdvance()) {
            return;
        }
        if (step == Step.CHECK_FOLDER && folderDialog == null && !discovering) {
            checkFolder();
        }
        if (step == Step.CONTINUE) {
            step = Step.IDLE;
            continueStartup.run();
        }
    }

    private boolean canAdvance() {
        return resumed && !activity.isFinishing() && !activity.isDestroyed();
    }

    private void checkFolder() {
        discovering = true;
        int generation = ++discoveryGeneration;
        AndroidBRouterSegmentsTreeAccessPrompt.findAvailableFolder(activity, segmentsRepository,
                uri -> onFolderFound(generation, uri));
    }

    private void onFolderFound(int generation, @Nullable Uri uri) {
        if (generation != discoveryGeneration || !canAdvance()) {
            return;
        }
        cancelDiscovery();
        if (uri == null || uri.toString().equals(skippedFolder) || !needsFolderGrant()) {
            finishCheck();
            return;
        }
        folderDialog = AndroidBRouterSegmentsTreeAccessPrompt.showNow(
                activity,
                uri,
                REQUEST_SEGMENTS_TREE,
                TAG,
                () -> {
                    picker.begin(REQUEST_SEGMENTS_TREE);
                    step = Step.WAIT_FOR_PICKER;
                },
                () -> {
                    denySurroundingStreetStorage();
                    finishCheck();
                },
                this::finishCheck
        );
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_SEGMENTS_TREE) {
            return false;
        }
        if (step != Step.WAIT_FOR_PICKER) {
            return true;
        }
        if (resultCode == Activity.RESULT_OK) {
            Uri granted = AndroidBRouterFolderAccess.persistSelection(activity, data, AndroidBRouterFolderAccess.Kind.SEGMENTS);
            if (granted != null) {
                segmentsRepository.saveSegmentsTreeUri(activity, granted);
            } else {
                explainMissingStorage();
            }
        } else {
            denySurroundingStreetStorage();
        }
        picker.complete();
        finishCheck();
        return true;
    }

    private void finishCheck() {
        step = Step.CONTINUE;
        folderDialog = null;
        advance();
    }

    void clear() {
        cancelDiscovery();
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
}
