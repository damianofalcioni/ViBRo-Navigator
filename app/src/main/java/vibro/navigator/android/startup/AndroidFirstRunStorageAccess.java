package vibro.navigator.android.startup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterProfilesTreeAccessPrompt;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPrompt;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.settings.AppCompassSettings;

/** Folder grants are only needed on scoped-storage devices with BRouter installed. */
final class AndroidFirstRunStorageAccess {
    static final int REQUEST_PROFILES = 1102;
    static final int REQUEST_SEGMENTS = 1103;
    private static final String TAG = "FirstRunStorage";

    private final Activity activity;
    private final BRouterProfilesRepository profiles = AndroidBRouterProfilesRepositoryFactory.create();
    private final BRouterSegmentsRepository segments = AndroidBRouterSegmentsRepositoryFactory.create();
    private AlertDialog folderDialog;

    AndroidFirstRunStorageAccess(Activity activity) {
        this.activity = activity;
    }

    boolean requestProfiles(Runnable beforeLaunch, Runnable onCancel) {
        if (!needsFolderAccess() || profiles.hasPersistedProfilesTreeAccess(activity)) {
            return false;
        }
        AndroidBRouterProfilesTreeAccessPrompt.show(activity, profiles, REQUEST_PROFILES, TAG,
                beforeLaunch, onCancel, dialog -> folderDialog = dialog);
        return true;
    }

    boolean requestSegments(Runnable beforeLaunch, Runnable onCancel) {
        if (!needsFolderAccess() || !AppCompassSettings.isSurroundingStreetsEnabled(activity)
                || segments.hasPersistedSegmentsTreeAccess(activity)) {
            return false;
        }
        AndroidBRouterSegmentsTreeAccessPrompt.show(activity, segments, REQUEST_SEGMENTS, TAG,
                beforeLaunch, onCancel, dialog -> folderDialog = dialog);
        return true;
    }

    boolean handleResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_PROFILES && requestCode != REQUEST_SEGMENTS) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            persist(requestCode, data);
        }
        return true;
    }

    private void persist(int requestCode, Intent data) {
        if (!AndroidDocumentAccess.persistReadPermission(activity, data, data.getData())) {
            return;
        }
        if (requestCode == REQUEST_PROFILES) {
            profiles.saveProfilesTreeUri(activity, data.getData());
        } else {
            segments.saveSegmentsTreeUri(activity, data.getData());
        }
    }

    private boolean needsFolderAccess() {
        return !AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()
                && profiles.isBRouterInstalled(activity);
    }

    void dispose() {
        if (folderDialog != null) {
            folderDialog.dismiss();
        }
    }
}
