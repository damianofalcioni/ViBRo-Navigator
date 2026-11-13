package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.Collection;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterFolderAccess.Kind;
import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.settings.AppCompassSettings;

import static vibro.navigator.android.brouter.AndroidBRouterStorageSetup.REQUEST_PROFILES;

final class AndroidBRouterFolderRequests {
    private final Activity activity;
    private final BRouterProfilesRepository profiles = AndroidBRouterProfilesRepositoryFactory.create();
    private final BRouterSegmentsRepository segments = AndroidBRouterSegmentsRepositoryFactory.create();

    AndroidBRouterFolderRequests(Activity activity) {
        this.activity = activity;
    }

    boolean needsFolderAccess() {
        return !AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()
                && profiles.isBRouterInstalled(activity);
    }

    boolean needsGrant(int requestCode) {
        return requestCode == REQUEST_PROFILES
                ? !profiles.hasPersistedProfilesTreeAccess(activity)
                : AppCompassSettings.isSurroundingStreetsEnabled(activity)
                        && !segments.hasPersistedSegmentsTreeAccess(activity);
    }

    void findLocation(int requestCode, Collection<String> excluded, @Nullable AndroidBRouterPickerLocation fallback,
                      Consumer<AndroidBRouterPickerLocation> callback) {
        AndroidBRouterFolderDiscovery.find(signal -> resolveLocation(requestCode, excluded, signal), fallback, callback);
    }

    private AndroidBRouterPickerLocation resolveLocation(int requestCode, Collection<String> excluded,
                                                         CancellationSignal signal) {
        return requestCode == REQUEST_PROFILES
                ? AndroidBRouterProfilesTreeAccessPrompt.findLocation(activity, profiles, excluded, signal)
                : AndroidBRouterSegmentsTreeAccessPrompt.findLocation(activity, segments, excluded, signal);
    }

    void handleResult(int requestCode, int resultCode, @Nullable Intent data, AndroidBRouterPickerRequest picker,
                      Runnable onAdvance, Runnable onRetry) {
        if (resultCode == Activity.RESULT_OK && !persist(requestCode, data)) {
            int message = requestCode == REQUEST_PROFILES
                    ? R.string.msg_brouter_profiles_storage_permission_required
                    : R.string.msg_compass_surrounding_streets_storage_permission_required;
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            picker.complete(Completion.RETRY);
            onRetry.run();
        } else {
            picker.complete(Completion.ADVANCE);
            onAdvance.run();
        }
    }

    private boolean persist(int requestCode, @Nullable Intent data) {
        Kind kind = requestCode == REQUEST_PROFILES ? Kind.PROFILES : Kind.SEGMENTS;
        Uri uri = AndroidBRouterFolderAccess.persistSelection(activity, data, kind);
        if (uri == null) {
            return false;
        }
        if (kind == Kind.PROFILES) {
            profiles.saveProfilesTreeUri(activity, uri);
        } else {
            segments.saveSegmentsTreeUri(activity, uri);
        }
        return true;
    }
}
