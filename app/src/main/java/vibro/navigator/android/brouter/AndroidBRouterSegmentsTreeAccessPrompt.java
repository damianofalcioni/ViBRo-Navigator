package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
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
        new AlertDialog.Builder(activity)
                .setTitle(R.string.title_brouter_segments_storage_access)
                .setMessage(R.string.msg_brouter_segments_storage_access_prompt)
                .setPositiveButton(R.string.action_continue, (dialog, which) -> launchPicker(
                        activity,
                        segmentsRepository,
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
            @NonNull BRouterSegmentsRepository segmentsRepository,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch
    ) {
        Uri initialUri = segmentsRepository.getSegmentsTreePickerInitialUri(activity);
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
