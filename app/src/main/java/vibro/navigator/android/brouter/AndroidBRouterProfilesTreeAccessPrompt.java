package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.CancellationSignal;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfilesRepository;

public final class AndroidBRouterProfilesTreeAccessPrompt {
    private static final String FOLDER_NAME = "profiles2";

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
                location -> showNow(activity, location.initialUri, requestCode, logTag, beforeLaunch, onCancel, onCancel));
    }

    @NonNull
    static AndroidBRouterPickerLocation findLocation(
            @NonNull Activity activity,
            @NonNull BRouterProfilesRepository profilesRepository,
            Collection<String> excluded,
            CancellationSignal signal
    ) {
        Uri savedProfileHint = profilesRepository.getCustomProfileUri(activity);
        return AndroidBRouterPickerLocation.resolve(
                activity, profilesRepository.getProfilesTreePickerHint(activity, savedProfileHint),
                FOLDER_NAME, excluded, signal);
    }

    static AlertDialog showNow(
            @NonNull Activity activity,
            @Nullable Uri initialUri,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch,
            @NonNull Runnable onCancel,
            @NonNull Runnable onLaunchFailure
    ) {
        if (initialUri == null) {
            return AndroidBRouterPickerLocation.showMissingFolder(activity, FOLDER_NAME,
                    () -> AndroidBRouterTreePicker.launch(activity, null, requestCode, logTag,
                            beforeLaunch, onLaunchFailure), onCancel);
        }
        return new AlertDialog.Builder(activity)
                .setTitle(R.string.title_brouter_profiles_storage_access)
                .setMessage(activity.getString(R.string.msg_brouter_profiles_storage_access_prompt,
                        AndroidBRouterStorageInstructions.folderPath(initialUri, FOLDER_NAME)))
                .setPositiveButton(R.string.action_continue, (dialog, which) -> AndroidBRouterTreePicker.launch(
                        activity,
                        initialUri,
                        requestCode,
                        logTag,
                        beforeLaunch,
                        onLaunchFailure
                ))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> onCancel.run())
                .setOnCancelListener(dialog -> onCancel.run())
                .show();
    }
}
