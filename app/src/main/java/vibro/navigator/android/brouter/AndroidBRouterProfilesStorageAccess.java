package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;

/** Uses the shared legacy read permission or a persisted folder grant, depending on Android. */
public final class AndroidBRouterProfilesStorageAccess {
    private AndroidBRouterProfilesStorageAccess() {
    }

    public static boolean hasAccess(
            @NonNull Context context,
            @NonNull BRouterProfilesRepository profilesRepository
    ) {
        if (AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()) {
            return AndroidLegacyExternalStorageAccess.hasReadPermission(context);
        }
        return profilesRepository.hasPersistedProfilesTreeAccess(context);
    }

    public static void request(
            @NonNull Activity activity,
            @NonNull BRouterProfilesRepository profilesRepository,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch,
            @NonNull Runnable onCancel
    ) {
        if (AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()) {
            AndroidLegacyExternalStorageAccess.requestReadPermission(activity, requestCode);
            return;
        }
        AndroidBRouterProfilesTreeAccessPrompt.show(
                activity, profilesRepository, requestCode, logTag, beforeLaunch, onCancel);
    }
}
