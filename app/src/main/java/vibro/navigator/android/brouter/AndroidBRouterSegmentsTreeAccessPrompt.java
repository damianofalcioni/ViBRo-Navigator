package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.CancellationSignal;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

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
                (signal, progress) -> findLocation(activity, Collections.emptySet(), signal, progress),
                location -> showNow(activity, location.initialUri, requestCode, logTag, beforeLaunch, onCancel, onCancel));
    }

    public static void cancel(Activity activity, int requestCode) {
        AndroidBRouterDeferredPrompt.cancel(activity, requestCode);
    }

    public static void findAvailableFolder(Activity activity,
                                           Collection<String> excluded, Consumer<Uri> onResult) {
        AtomicReference<Uri> fallback = new AtomicReference<>();
        AndroidBRouterFolderDiscovery.findWithFallback(
                signal -> findLocation(activity, excluded, signal, location -> fallback.set(location.confirmedUri()))
                        .confirmedUri(), fallback::get, onResult);
    }

    @NonNull
    private static AndroidBRouterPickerLocation findLocation(
            @NonNull Activity activity,
            Collection<String> excluded,
            CancellationSignal signal,
            Consumer<AndroidBRouterPickerLocation> progress
    ) {
        return AndroidBRouterPickerLocation.resolve(
                activity, null, AndroidBRouterFolderAccess.FOLDER, AndroidBRouterFolderAccess.Kind.SEGMENTS.folder,
                excluded, signal, progress);
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
                .setMessage(R.string.msg_brouter_segments_folder_required)
                .setPositiveButton(R.string.action_browse, (dialog, which) ->
                        AndroidBRouterTreePicker.launch(activity, initialUri, requestCode, logTag, beforeLaunch, onSkip))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> onSkip.run())
                .setOnCancelListener(dialog -> onSkip.run())
                .show();
    }
}
