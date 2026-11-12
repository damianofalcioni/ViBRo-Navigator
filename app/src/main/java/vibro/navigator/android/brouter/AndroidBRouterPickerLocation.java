package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidDocumentAccess.DirectoryPresence;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterDirectoryCandidates;

final class AndroidBRouterPickerLocation {
    private AndroidBRouterPickerLocation() {
    }

    @Nullable
    static Uri resolve(@NonNull Context context, @Nullable Uri hint, @NonNull String folderName) {
        if (hint != null && DocumentsContractCompat.isTreeUri(hint)) {
            return hint;
        }
        String preferredId = hint == null ? null : AndroidDocumentAccess.documentId(hint);
        return findSelectableFolder(context, folderName, preferredId);
    }

    @Nullable
    private static Uri findSelectableFolder(
            @NonNull Context context,
            @NonNull String folderName,
            @Nullable String preferredId
    ) {
        String unverifiedId = null;
        for (String id : BRouterDirectoryCandidates.buildLikelyDocumentIdCandidates(
                AndroidStorageVolumes.secondaryStorageRootIds(context), folderName)) {
            DirectoryPresence presence = selectablePresence(context, id);
            if (presence == DirectoryPresence.PRESENT) {
                return AndroidDocumentAccess.buildExternalStorageDocumentUri(id);
            }
            if (presence == DirectoryPresence.UNKNOWN && (unverifiedId == null || id.equals(preferredId))) {
                unverifiedId = id;
            }
        }
        return unverifiedId == null ? null : AndroidDocumentAccess.buildExternalStorageDocumentUri(unverifiedId);
    }

    @NonNull
    private static DirectoryPresence selectablePresence(@NonNull Context context, @NonNull String id) {
        // New tree grants for Android/data are restricted on Android 11 and later.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && id.contains(":Android/data/")
                ? DirectoryPresence.MISSING : AndroidDocumentAccess.externalStorageDirectoryPresence(context, id);
    }

    @NonNull
    static AlertDialog showMissingFolder(
            @NonNull Activity activity,
            @NonNull String folderName,
            @NonNull Runnable onBrowse,
            @NonNull Runnable onCancel
    ) {
        return new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.msg_brouter_storage_folder_missing, folderName))
                .setPositiveButton(R.string.action_browse, (dialog, which) -> onBrowse.run())
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> onCancel.run())
                .setOnCancelListener(dialog -> onCancel.run())
                .show();
    }
}
