package vibro.navigator.android.brouter;

import android.app.Activity;
import android.net.Uri;
import android.os.CancellationSignal;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;

public final class AndroidBRouterProfilesTreeAccessPrompt {
    private AndroidBRouterProfilesTreeAccessPrompt() {
    }

    public static void show(
            @NonNull Activity activity,
            @NonNull BRouterProfilesRepository profilesRepository,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch,
            @NonNull Runnable onCancel
    ) {
        AndroidBRouterDeferredPrompt.request(activity,
                signal -> findLocation(activity, profilesRepository, Collections.emptySet(), signal),
                location -> AndroidBRouterFolderPrompt.show(activity, location.initialUri, requestCode, logTag,
                        beforeLaunch, onCancel, onCancel));
    }

    @NonNull
    static AndroidBRouterPickerLocation findLocation(
            @NonNull Activity activity,
            @NonNull BRouterProfilesRepository profilesRepository,
            Collection<String> excluded,
            CancellationSignal signal
    ) {
        Uri savedProfileHint = profilesRepository.getCustomProfileUri(activity);
        Uri hint = profilesRepository.getProfilesTreePickerHint(activity, savedProfileHint);
        String id = hint != null && AndroidDocumentAccess.isExternalStorageDocument(hint)
                ? AndroidDocumentAccess.documentId(hint) : null;
        Uri parent = parentHint(id);
        return AndroidBRouterPickerLocation.resolve(
                activity, parent, AndroidBRouterFolderAccess.FOLDER, excluded, signal);
    }

    @Nullable
    private static Uri parentHint(@Nullable String id) {
        if (id == null) {
            return null;
        }
        if (id.endsWith("/brouter/profiles2") || id.endsWith(":brouter/profiles2")) {
            return AndroidDocumentAccess.buildExternalStorageDocumentUri(id.substring(0, id.lastIndexOf('/')));
        }
        return null;
    }
}
