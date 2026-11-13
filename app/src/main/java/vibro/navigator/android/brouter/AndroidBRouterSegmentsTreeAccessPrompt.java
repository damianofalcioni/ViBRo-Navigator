package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.CancellationSignal;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import androidx.core.util.Consumer;

import vibro.navigator.R;
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
        AndroidBRouterDeferredPrompt.request(activity,
                signal -> findLocation(activity, segmentsRepository, Collections.emptySet(), signal),
                location -> showNow(activity, location.initialUri, requestCode, logTag, beforeLaunch, onCancel, onCancel));
    }

    public static void findAvailableFolder(
            @NonNull Activity activity,
            @NonNull BRouterSegmentsRepository repository,
            @NonNull Consumer<Uri> onResult
    ) {
        findAvailableFolder(activity, repository, Collections.emptySet(), onResult);
    }

    public static void findAvailableFolder(Activity activity, BRouterSegmentsRepository repository,
                                           Collection<String> excluded, Consumer<Uri> onResult) {
        AndroidBRouterFolderDiscovery.find(
                signal -> findLocation(activity, repository, excluded, signal).confirmedUri(), null, onResult);
    }

    @NonNull
    static AndroidBRouterPickerLocation findLocation(
            @NonNull Activity activity,
            @NonNull BRouterSegmentsRepository segmentsRepository,
            Collection<String> excluded,
            CancellationSignal signal
    ) {
        return AndroidBRouterPickerLocation.resolve(
                activity, initialDocumentUri(segmentsRepository.getGrantedSegmentsTreeUri(activity)),
                FOLDER_NAME, excluded, signal);
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
