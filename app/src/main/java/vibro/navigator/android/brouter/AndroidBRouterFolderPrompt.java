package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import vibro.navigator.R;

final class AndroidBRouterFolderPrompt {
    private AndroidBRouterFolderPrompt() {
    }

    static AlertDialog show(Activity activity, @Nullable Uri initialUri, int requestCode, String logTag,
                            Runnable beforeLaunch, Runnable onCancel, Runnable onLaunchFailure) {
        String folder = AndroidBRouterFolderAccess.FOLDER;
        if (initialUri == null) {
            return AndroidBRouterPickerLocation.showMissingFolder(activity, folder,
                    () -> AndroidBRouterTreePicker.launch(activity, null, requestCode, logTag,
                            beforeLaunch, onLaunchFailure), onCancel);
        }
        return new AlertDialog.Builder(activity)
                .setTitle(R.string.title_brouter_storage_access)
                .setMessage(withTileFolderHint(activity, activity.getString(R.string.msg_brouter_storage_access_prompt,
                        AndroidBRouterStorageInstructions.folderPath(initialUri, folder))))
                .setPositiveButton(R.string.action_continue, (dialog, which) -> AndroidBRouterTreePicker.launch(
                        activity, initialUri, requestCode, logTag, beforeLaunch, onLaunchFailure))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> onCancel.run())
                .setOnCancelListener(dialog -> onCancel.run())
                .show();
    }

    static String withTileFolderHint(Context context, String message) {
        return message + "\n\n" + context.getString(R.string.msg_brouter_direct_tiles_hint);
    }
}
