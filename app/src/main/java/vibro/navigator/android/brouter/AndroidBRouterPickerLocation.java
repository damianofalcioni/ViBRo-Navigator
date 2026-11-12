package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidDocumentAccess.DirectoryPresence;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterDirectoryCandidates;

final class AndroidBRouterPickerLocation {
    @Nullable
    final Uri initialUri;
    final boolean confirmed;

    private AndroidBRouterPickerLocation(@Nullable Uri initialUri, boolean confirmed) {
        this.initialUri = initialUri;
        this.confirmed = confirmed;
    }

    @NonNull
    static AndroidBRouterPickerLocation browse() {
        return new AndroidBRouterPickerLocation(null, false);
    }

    @NonNull
    static AndroidBRouterPickerLocation resolve(@NonNull Context context, @Nullable Uri hint, @NonNull String folderName) {
        if (hint != null && DocumentsContractCompat.isTreeUri(hint)) {
            return new AndroidBRouterPickerLocation(hint, true);
        }
        String preferredId = hint == null ? null : AndroidDocumentAccess.documentId(hint);
        return findSelectableFolder(context, folderName, preferredId);
    }

    @Nullable
    Uri confirmedUri() {
        return confirmed ? initialUri : null;
    }

    @NonNull
    private static AndroidBRouterPickerLocation findSelectableFolder(
            @NonNull Context context,
            @NonNull String folderName,
            @Nullable String preferredId
    ) {
        String unverifiedId = null;
        List<String> candidates = BRouterDirectoryCandidates.buildLikelyDocumentIdCandidates(
                AndroidStorageVolumes.secondaryStorageRootIds(context), folderName);
        for (String id : candidates) {
            DirectoryPresence presence = selectablePresence(context, id);
            if (presence == DirectoryPresence.PRESENT) {
                return new AndroidBRouterPickerLocation(AndroidDocumentAccess.buildExternalStorageDocumentUri(id), true);
            }
            if (presence == DirectoryPresence.UNKNOWN && (unverifiedId == null || id.equals(preferredId))) {
                unverifiedId = id;
            }
        }
        return fallbackLocation(context, candidates, preferredId, unverifiedId);
    }

    private static AndroidBRouterPickerLocation fallbackLocation(Context context, List<String> candidates,
                                                                 @Nullable String preferredId,
                                                                 @Nullable String unverifiedId) {
        if (preferredId != null && !candidates.contains(preferredId)) {
            DirectoryPresence presence = selectablePresence(context, preferredId);
            if (presence != DirectoryPresence.MISSING) {
                return new AndroidBRouterPickerLocation(AndroidDocumentAccess.buildExternalStorageDocumentUri(preferredId),
                        presence == DirectoryPresence.PRESENT);
            }
        }
        Uri hint = unverifiedId == null ? null : AndroidDocumentAccess.buildExternalStorageDocumentUri(unverifiedId);
        return new AndroidBRouterPickerLocation(hint, false);
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
