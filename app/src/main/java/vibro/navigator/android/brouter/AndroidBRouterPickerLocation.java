package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import java.util.List;
import java.util.Collections;
import java.util.Collection;

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
        return resolve(context, hint, folderName, Collections.emptySet(), new CancellationSignal());
    }

    static AndroidBRouterPickerLocation resolve(Context context, @Nullable Uri hint, String folderName,
                                                Collection<String> excluded, CancellationSignal signal) {
        if (hint != null && DocumentsContractCompat.isTreeUri(hint)) {
            return new AndroidBRouterPickerLocation(hint, true);
        }
        String preferredId = hint == null ? null : AndroidDocumentAccess.documentId(hint);
        return findSelectableFolder(context, folderName, preferredId, excluded, signal);
    }

    @Nullable
    Uri confirmedUri() {
        return confirmed ? initialUri : null;
    }

    @NonNull
    private static AndroidBRouterPickerLocation findSelectableFolder(
            @NonNull Context context,
            @NonNull String folderName,
            @Nullable String preferredId,
            Collection<String> excluded,
            CancellationSignal signal
    ) {
        String unverifiedId = null;
        List<String> candidates = BRouterDirectoryCandidates.buildLikelyDocumentIdCandidates(
                AndroidStorageVolumes.secondaryStorageRootIds(context), folderName);
        for (String id : candidates) {
            Uri uri = AndroidDocumentAccess.buildExternalStorageDocumentUri(id);
            if (excluded.contains(uri.toString())) {
                continue;
            }
            DirectoryPresence presence = selectablePresence(context, id, signal);
            if (presence == DirectoryPresence.PRESENT) {
                return new AndroidBRouterPickerLocation(uri, true);
            }
            if (presence == DirectoryPresence.UNKNOWN && (unverifiedId == null || id.equals(preferredId))) {
                unverifiedId = id;
            }
        }
        return fallbackLocation(context, candidates, preferredId, unverifiedId, excluded, signal);
    }

    private static AndroidBRouterPickerLocation fallbackLocation(Context context, List<String> candidates,
                                                                 @Nullable String preferredId,
                                                                 @Nullable String unverifiedId,
                                                                 Collection<String> excluded,
                                                                 CancellationSignal signal) {
        if (preferredId != null && !candidates.contains(preferredId)) {
            AndroidBRouterPickerLocation preferred = verifyHint(context, preferredId, excluded, signal);
            if (preferred != null) {
                return preferred;
            }
        }
        Uri hint = unverifiedId == null ? null : AndroidDocumentAccess.buildExternalStorageDocumentUri(unverifiedId);
        return new AndroidBRouterPickerLocation(hint, false);
    }

    @Nullable
    private static AndroidBRouterPickerLocation verifyHint(Context context, String id, Collection<String> excluded,
                                                          CancellationSignal signal) {
        Uri uri = AndroidDocumentAccess.buildExternalStorageDocumentUri(id);
        if (excluded.contains(uri.toString())) {
            return null;
        }
        DirectoryPresence presence = selectablePresence(context, id, signal);
        return presence == DirectoryPresence.MISSING ? null
                : new AndroidBRouterPickerLocation(uri, presence == DirectoryPresence.PRESENT);
    }

    @NonNull
    private static DirectoryPresence selectablePresence(Context context, String id, CancellationSignal signal) {
        return AndroidBRouterFolderAccess.isSelectableDocumentId(id)
                ? AndroidDocumentAccess.externalStorageDirectoryPresence(context, id, signal) : DirectoryPresence.MISSING;
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
