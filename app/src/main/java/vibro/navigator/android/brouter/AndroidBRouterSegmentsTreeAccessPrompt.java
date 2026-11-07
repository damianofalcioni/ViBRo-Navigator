package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.logging.AppLogger;

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
        show(activity, segmentsRepository, requestCode, logTag, beforeLaunch, onCancel, null);
    }

    public static void showIfAvailable(
            @NonNull Activity activity,
            @NonNull BRouterSegmentsRepository segmentsRepository,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable onCancel,
            @NonNull Runnable onUnavailable
    ) {
        show(activity, segmentsRepository, requestCode, logTag, () -> { }, onCancel, onUnavailable);
    }

    private static void show(
            @NonNull Activity activity,
            @NonNull BRouterSegmentsRepository segmentsRepository,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch,
            @NonNull Runnable onCancel,
            @Nullable Runnable onUnavailable
    ) {
        AndroidTaskScheduler.main().postDelayed(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            Uri initialUri = findInitialUri(activity, segmentsRepository);
            if (initialUri == null && onUnavailable != null) {
                onUnavailable.run();
                return;
            }
            showNow(activity, initialUri, requestCode, logTag, beforeLaunch, onCancel);
        }, ViewConfiguration.getPressedStateDuration());
    }

    @Nullable
    static Uri findInitialUri(@NonNull Activity activity, @NonNull BRouterSegmentsRepository segmentsRepository) {
        return AndroidBRouterPickerLocation.resolve(
                activity, initialDocumentUri(segmentsRepository.getSegmentsTreePickerInitialUri(activity)), FOLDER_NAME);
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
                .setTitle(R.string.title_brouter_segments_storage_access)
                .setMessage(activity.getString(R.string.msg_brouter_segments_storage_access_prompt,
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

    @Nullable
    private static Uri initialDocumentUri(@Nullable Uri uri) {
        // Android 10 DocumentsUI can fall back to Downloads for a bare tree URI.
        return uri != null && DocumentsContractCompat.isTreeUri(uri)
                ? AndroidDocumentAccess.buildTreeDocumentUri(uri) : uri;
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
        AppLogger.i(logTag, "Launching BRouter segments tree picker initialUri=" + safe(initialUri));
        activity.startActivityForResult(intent, requestCode);
    }

    @NonNull
    private static String safe(@Nullable Uri value) {
        return value == null ? "null" : value.toString();
    }
}
