package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.CancellationSignal;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.util.Consumer;

import vibro.navigator.R;

public final class AndroidBRouterSegmentsTreeAccessPrompt {
    private AndroidBRouterSegmentsTreeAccessPrompt() {
    }

    public static void show(
            @NonNull Activity activity,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch,
            @NonNull Runnable onCancel
    ) {
        AndroidBRouterDeferredPrompt.request(activity, requestCode, true,
                signal -> findLocation(activity, Collections.emptySet(), signal),
                location -> showNow(activity, location.initialUri, requestCode, logTag, beforeLaunch, onCancel, onCancel));
    }

    public static void findAvailableFolder(Activity activity,
                                           Collection<String> excluded, Consumer<Uri> onResult) {
        AndroidBRouterFolderDiscovery.find(
                signal -> findLocation(activity, excluded, signal).confirmedUri(), null, onResult);
    }

    @NonNull
    private static AndroidBRouterPickerLocation findLocation(
            @NonNull Activity activity,
            Collection<String> excluded,
            CancellationSignal signal
    ) {
        return AndroidBRouterPickerLocation.resolve(
                activity, null, AndroidBRouterFolderAccess.FOLDER, excluded, signal);
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
        return AndroidBRouterFolderPrompt.show(activity, initialUri, requestCode, logTag,
                beforeLaunch, onCancel, onLaunchFailure);
    }

    public static AlertDialog showRetry(Activity activity, @Nullable Uri initialUri, int requestCode,
                                        String logTag, Runnable beforeLaunch, Runnable onSkip) {
        return new AlertDialog.Builder(activity)
                .setTitle(R.string.title_brouter_storage_access)
                .setMessage(R.string.msg_brouter_folder_required)
                .setPositiveButton(R.string.action_browse, (dialog, which) ->
                        AndroidBRouterTreePicker.launch(activity, initialUri, requestCode, logTag, beforeLaunch, onSkip))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> onSkip.run())
                .setOnCancelListener(dialog -> onSkip.run())
                .show();
    }
}
