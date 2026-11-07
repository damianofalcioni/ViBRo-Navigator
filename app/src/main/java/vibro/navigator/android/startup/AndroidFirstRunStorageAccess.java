package vibro.navigator.android.startup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterFolderAccess;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterProfilesTreeAccessPrompt;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPrompt;
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

    boolean handleResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data,
            Runnable onAdvance,
            Runnable onRetry
    ) {
        if (requestCode != REQUEST_PROFILES && requestCode != REQUEST_SEGMENTS) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK && !persist(requestCode, data)) {
            int message = requestCode == REQUEST_PROFILES
                    ? R.string.msg_brouter_profiles_storage_permission_required
                    : R.string.msg_compass_surrounding_streets_storage_permission_required;
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            onRetry.run();
        } else {
            onAdvance.run();
        }
        return true;
    }

    private boolean persist(int requestCode, @Nullable Intent data) {
        if (data == null) {
            return false;
        }
        Uri uri = data.getData();
        if (uri == null || !AndroidBRouterFolderAccess.persistReadPermission(activity, data, uri)) {
            return false;
        }
        if (requestCode == REQUEST_PROFILES) {
            profiles.saveProfilesTreeUri(activity, uri);
        } else {
            segments.saveSegmentsTreeUri(activity, uri);
        }
        return true;
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
