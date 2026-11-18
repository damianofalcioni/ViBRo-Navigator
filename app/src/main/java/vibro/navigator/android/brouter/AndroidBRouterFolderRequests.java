package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.settings.AppCompassSettings;

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

    boolean needsGrant() {
        return !profiles.hasPersistedProfilesTreeAccess(activity)
                || (AppCompassSettings.isSurroundingStreetsEnabled(activity)
                        && !segments.hasPersistedSegmentsTreeAccess(activity));
    }

    void findLocation(Collection<String> excluded, @Nullable AndroidBRouterPickerLocation fallback,
                      Consumer<AndroidBRouterPickerLocation> callback) {
        AtomicReference<AndroidBRouterPickerLocation> confirmed = new AtomicReference<>(fallback);
        boolean profilesMissing = fallback == null && !profiles.hasPersistedProfilesTreeAccess(activity);
        boolean segmentsMissing = fallback == null && AppCompassSettings.isSurroundingStreetsEnabled(activity)
                && !segments.hasPersistedSegmentsTreeAccess(activity);
        var wanted = profilesMissing == segmentsMissing ? null : profilesMissing
                ? AndroidBRouterFolderAccess.Kind.PROFILES : AndroidBRouterFolderAccess.Kind.SEGMENTS;
        AndroidBRouterFolderDiscovery.findWithFallback(
                signal -> AndroidBRouterProfilesTreeAccessPrompt.findLocation(
                        activity, profiles, excluded, signal, confirmed::set, wanted),
                confirmed::get, callback);
    }

    static void acceptLegacyPickerResult(Activity activity, boolean profiles, int resultCode, @Nullable Intent data) {
        Uri uri = data == null ? null : data.getData();
        if (resultCode != Activity.RESULT_OK || uri == null || AndroidBRouterFolderAccess.treeId(uri) == null
                || !AndroidDocumentAccess.persistReadPermission(activity, data, uri)) {
            return;
        }
        if (profiles) {
            AndroidBRouterProfilesRepositoryFactory.create().saveProfilesTreeUri(activity, uri);
        } else {
            AndroidBRouterSegmentsRepositoryFactory.create().saveSegmentsTreeUri(activity, uri);
        }
    }

    void handleResult(int resultCode, @Nullable Intent data, AndroidBRouterPickerRequest picker,
                      Consumer<Completion> onCompletion) {
        if (resultCode != Activity.RESULT_OK) {
            picker.complete(Completion.CANCELED);
            onCompletion.accept(Completion.CANCELED);
            return;
        }
        picker.beginValidation();
        AndroidBRouterFolderAccess.saveSelection(activity, data,
                saved -> finishValidation(picker, onCompletion, saved));
    }

    private void finishValidation(AndroidBRouterPickerRequest picker, Consumer<Completion> onCompletion, boolean saved) {
        Completion completion = saved ? Completion.ADVANCE : Completion.RETRY;
        picker.complete(completion);
        if (activity.isDestroyed() || activity.isFinishing()) {
            return;
        }
        if (!saved) {
            Toast.makeText(activity, R.string.msg_brouter_folder_required, Toast.LENGTH_SHORT).show();
        }
        onCompletion.accept(completion);
    }
}
