package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.Collection;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;
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
        Collection<String> candidates = fallback == null ? excludeAlreadyGrantedParent(excluded) : excluded;
        AndroidBRouterFolderDiscovery.findWithFallback(
                signal -> AndroidBRouterProfilesTreeAccessPrompt.findLocation(activity, profiles, candidates, signal,
                        confirmed::set), confirmed::get, callback);
    }

    private Collection<String> excludeAlreadyGrantedParent(Collection<String> excluded) {
        var grantedParent = AndroidBRouterProfilesTreeAccessPrompt.savedParent(activity, excluded);
        if (grantedParent == null) {
            return excluded;
        }
        var unavailable = new ArrayList<>(excluded);
        unavailable.add(grantedParent.toString());
        return unavailable;
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
