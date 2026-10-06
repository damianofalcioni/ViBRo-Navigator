package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.logging.AppLogger;

public final class AndroidBRouterSegmentsTreeAccessPrompt {

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
                showNow(activity, segmentsRepository, requestCode, logTag, beforeLaunch, onCancel);
            }
        }, ViewConfiguration.getPressedStateDuration());
    }

    private static void showNow(
            @NonNull Activity activity,
            @NonNull BRouterSegmentsRepository segmentsRepository,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch,
            @NonNull Runnable onCancel
    ) {
        Uri initialUri = segmentsRepository.getSegmentsTreePickerInitialUri(activity);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.title_brouter_segments_storage_access)
                .setMessage(activity.getString(R.string.msg_brouter_segments_storage_access_prompt,
                        AndroidBRouterStorageInstructions.folderPath(initialUri, "segments4")))
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
        AppLogger.i(logTag, "Launching BRouter segments tree picker initialUri=" + safe(initialUri));
        activity.startActivityForResult(intent, requestCode);
    }

    @NonNull
    private static String safe(@Nullable Uri value) {
        return value == null ? "null" : value.toString();
    }
}
