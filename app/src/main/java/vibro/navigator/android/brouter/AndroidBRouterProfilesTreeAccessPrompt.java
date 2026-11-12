package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.logging.AppLogger;

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
        AndroidTaskScheduler.main().postDelayed(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                Uri initialUri = findLocation(activity, profilesRepository).initialUri;
                showNow(activity, initialUri, requestCode, logTag, beforeLaunch, onCancel);
            }
        }, ViewConfiguration.getPressedStateDuration());
    }

    @NonNull
    static AndroidBRouterPickerLocation findLocation(
            @NonNull Activity activity,
            @NonNull BRouterProfilesRepository profilesRepository
    ) {
        Uri savedProfileHint = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
                ? profilesRepository.getCustomProfileUri(activity) : null;
        return AndroidBRouterPickerLocation.resolve(
                activity, profilesRepository.getProfilesTreePickerHint(activity, savedProfileHint), FOLDER_NAME);
    }

    static AlertDialog showNow(
            @NonNull Activity activity,
            @Nullable Uri initialUri,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch,
            @NonNull Runnable onCancel
    ) {
        if (initialUri == null) {
            return AndroidBRouterPickerLocation.showMissingFolder(activity, FOLDER_NAME,
                    () -> launchPicker(activity, null, requestCode, logTag, beforeLaunch), onCancel);
        }
        return new AlertDialog.Builder(activity)
                .setTitle(R.string.title_brouter_profiles_storage_access)
                .setMessage(activity.getString(R.string.msg_brouter_profiles_storage_access_prompt,
                        AndroidBRouterStorageInstructions.folderPath(initialUri, FOLDER_NAME)))
                .setPositiveButton(R.string.action_continue, (dialog, which) -> launchPicker(
                        activity,
                        initialUri,
                        requestCode,
                        logTag,
                        beforeLaunch
                ))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> onCancel.run())
                .setOnCancelListener(dialog -> onCancel.run())
                .show();
    }

    private static void launchPicker(
            @NonNull Activity activity,
            @Nullable Uri initialUri,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch
    ) {
        Intent intent = AndroidDocumentAccess.openDocumentTreeIntent(initialUri);
        beforeLaunch.run();
        AppLogger.i(logTag, "Launching BRouter profiles tree picker initialUri=" + safe(initialUri));
        activity.startActivityForResult(intent, requestCode);
    }

    @NonNull
    private static String safe(@Nullable Uri value) {
        return value == null ? "null" : value.toString();
    }
}
