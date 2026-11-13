package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.Collection;

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
        AndroidBRouterFolderDiscovery.find(
                signal -> AndroidBRouterProfilesTreeAccessPrompt.findLocation(activity, profiles, excluded, signal),
                fallback, callback);
    }

    void handleResult(int resultCode, @Nullable Intent data, AndroidBRouterPickerRequest picker,
                      Runnable onAdvance, Runnable onRetry) {
        if (resultCode == Activity.RESULT_OK && !AndroidBRouterFolderAccess.saveSelection(activity, data)) {
            Toast.makeText(activity, R.string.msg_brouter_folder_required, Toast.LENGTH_SHORT).show();
            picker.complete(Completion.RETRY);
            onRetry.run();
        } else {
            picker.complete(Completion.ADVANCE);
            onAdvance.run();
        }
    }
}
