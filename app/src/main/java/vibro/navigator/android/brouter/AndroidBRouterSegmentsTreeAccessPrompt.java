package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import androidx.core.util.Consumer;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.brouter.BRouterSegmentsRepository;

public final class AndroidBRouterSegmentsTreeAccessPrompt {
    private static final String FOLDER_NAME = "segments4";

    private AndroidBRouterSegmentsTreeAccessPrompt() {
    }

    public static void show(
            @NonNull Activity activity,
            @NonNull BRouterSegmentsRepository segmentsRepository,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch,
            @NonNull Runnable onCancel
    ) {
        AndroidTaskScheduler.main().postDelayed(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                AndroidBRouterFolderDiscovery.find(() -> findLocation(activity, segmentsRepository).initialUri, null, uri -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        showNow(activity, uri, requestCode, logTag, beforeLaunch, onCancel, onCancel);
                    }
                });
            }
        }, ViewConfiguration.getPressedStateDuration());
    }

    public static void findAvailableFolder(
            @NonNull Activity activity,
            @NonNull BRouterSegmentsRepository repository,
            @NonNull Consumer<Uri> onResult
    ) {
        AndroidBRouterFolderDiscovery.find(() -> findLocation(activity, repository).confirmedUri(), null, onResult);
    }

    @NonNull
    static AndroidBRouterPickerLocation findLocation(
            @NonNull Activity activity,
            @NonNull BRouterSegmentsRepository segmentsRepository
    ) {
        return AndroidBRouterPickerLocation.resolve(
                activity, initialDocumentUri(segmentsRepository.getGrantedSegmentsTreeUri(activity)), FOLDER_NAME);
    }

    public static AlertDialog showNow(
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
                .setTitle(R.string.title_brouter_segments_storage_access)
                .setMessage(activity.getString(R.string.msg_brouter_segments_storage_access_prompt,
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

    @Nullable
    private static Uri initialDocumentUri(@Nullable Uri uri) {
        // Android 10 DocumentsUI can fall back to Downloads for a bare tree URI.
        return uri != null && DocumentsContractCompat.isTreeUri(uri)
                ? AndroidDocumentAccess.buildTreeDocumentUri(uri) : uri;
    }
}
