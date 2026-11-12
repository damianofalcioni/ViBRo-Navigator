package vibro.navigator.nav.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterFolderAccess;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
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
    private Step step = Step.IDLE;
    private boolean resumed;
    @Nullable
    private AlertDialog folderDialog;

    NavigationStartupSegmentsTreeAccess(@NonNull Activity activity, @NonNull Runnable continueStartup) {
        this.activity = activity;
        this.continueStartup = continueStartup;
    }

    boolean restoreState(@Nullable Bundle state) {
        if (state != null) {
            step = Step.valueOf(state.getString(STATE_STEP, Step.IDLE.name()));
        }
        return step != Step.IDLE;
    }

    void saveState(@NonNull Bundle state) {
        state.putString(STATE_STEP, step.name());
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
    }

    boolean deferStartup(boolean autoStart, boolean skipPrompt) {
        if (!autoStart) {
            clear();
            return false;
        }
        if (step == Step.IDLE) {
            if (!needsFolderGrant()) {
                return false;
            }
            step = skipPrompt ? Step.CONTINUE : Step.CHECK_FOLDER;
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
        if (!resumed || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (step == Step.CHECK_FOLDER && folderDialog == null) {
            checkFolder();
        }
        if (step == Step.CONTINUE) {
            step = Step.IDLE;
            continueStartup.run();
        }
    }

    private void checkFolder() {
        if (!needsFolderGrant()) {
            step = Step.CONTINUE;
            return;
        }
        folderDialog = AndroidBRouterSegmentsTreeAccessPrompt.showIfAvailable(
                activity,
                segmentsRepository,
                REQUEST_SEGMENTS_TREE,
                TAG,
                () -> step = Step.WAIT_FOR_PICKER,
                () -> {
                    denySurroundingStreetStorage();
                    finishCheck();
                }
        );
        if (folderDialog == null) {
            step = Step.CONTINUE;
        }
    }

    boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_SEGMENTS_TREE) {
            return false;
        }
        if (step != Step.WAIT_FOR_PICKER) {
            return true;
        }
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null
                && AndroidBRouterFolderAccess.persistReadPermission(activity, data, data.getData())) {
            segmentsRepository.saveSegmentsTreeUri(activity, data.getData());
        } else {
            denySurroundingStreetStorage();
        }
        finishCheck();
        return true;
    }

    private void finishCheck() {
        step = Step.CONTINUE;
        folderDialog = null;
        advance();
    }

    void clear() {
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
}
