package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Collection;
import androidx.core.util.Consumer;

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
        return resolve(context, hint, folderName, null, excluded, signal, ignored -> { });
    }

    static AndroidBRouterPickerLocation resolve(Context context, @Nullable Uri hint, String folderName,
                                                @Nullable String wantedChild, Collection<String> excluded,
                                                CancellationSignal signal, Consumer<AndroidBRouterPickerLocation> progress) {
        if (hint != null && DocumentsContractCompat.isTreeUri(hint) && !excluded.contains(hint.toString())) {
            return confirmed(hint);
        }
        String preferredId = hint == null ? null : AndroidDocumentAccess.documentId(hint);
        List<Candidate> candidates = candidates(context, folderName, preferredId, excluded, signal);
        if (wantedChild == null) {
            return findSelectableFolder(context, candidates, preferredId, signal, progress);
        }
        return findWithChild(context, candidates, wantedChild, signal, progress);
    }

    private static List<Candidate> candidates(Context context, String folderName, @Nullable String preferredId,
                                              Collection<String> excluded, CancellationSignal signal) {
        List<String> ids = BRouterDirectoryCandidates.buildLikelyDocumentIdCandidates(
                AndroidStorageVolumes.secondaryStorageRootIds(context), folderName);
        if (preferredId != null && !ids.contains(preferredId)) {
            ids.add(preferredId);
        }
        List<Candidate> candidates = new ArrayList<>();
        for (String id : ids) {
            signal.throwIfCanceled();
            Uri uri = AndroidDocumentAccess.buildExternalStorageDocumentUri(id);
            if (!excluded.contains(uri.toString()) && AndroidBRouterDirectoryCheck.isSelectableDocumentId(id)) {
                candidates.add(new Candidate(id, uri, AndroidDocumentAccess.isDirectExternalDirectory(context, id)));
            }
        }
        return candidates;
    }

    private static AndroidBRouterPickerLocation findSelectableFolder(Context context, List<Candidate> candidates,
                                                                     @Nullable String preferredId,
                                                                     CancellationSignal signal,
                                                                     Consumer<AndroidBRouterPickerLocation> progress) {
        publishDirectFallback(candidates, progress);
        Uri unverified = null;
        for (Candidate candidate : candidates) {
            DirectoryPresence presence = candidate.presence(context, signal);
            if (presence == DirectoryPresence.PRESENT) {
                return confirmed(candidate.uri);
            }
            if (presence == DirectoryPresence.UNKNOWN && (unverified == null || candidate.id.equals(preferredId))) {
                unverified = candidate.uri;
            }
        }
        return new AndroidBRouterPickerLocation(unverified, false);
    }

    private static AndroidBRouterPickerLocation findWithChild(Context context, List<Candidate> candidates, String child,
                                                              CancellationSignal signal,
                                                              Consumer<AndroidBRouterPickerLocation> progress) {
        publishDirectFallback(candidates, progress);
        publishDirectChild(context, candidates, child, signal, progress);
        AndroidBRouterPickerLocation fallback = browse();
        for (Candidate candidate : candidates) {
            DirectoryPresence parent = candidate.presence(context, signal);
            if (parent == DirectoryPresence.PRESENT && candidate.hasChild(context, child, signal)) {
                return confirmed(candidate.uri);
            }
            fallback = candidate.preferOver(fallback, parent);
        }
        return fallback;
    }

    private static void publishDirectChild(Context context, List<Candidate> candidates, String child,
                                           CancellationSignal signal, Consumer<AndroidBRouterPickerLocation> progress) {
        for (Candidate candidate : candidates) {
            signal.throwIfCanceled();
            if (AndroidDocumentAccess.isDirectExternalDirectory(context, candidate.id + "/" + child)) {
                progress.accept(confirmed(candidate.uri));
                return;
            }
        }
    }

    private static void publishDirectFallback(List<Candidate> candidates, Consumer<AndroidBRouterPickerLocation> progress) {
        for (Candidate candidate : candidates) {
            if (candidate.direct) {
                progress.accept(confirmed(candidate.uri));
                return;
            }
        }
    }

    static AndroidBRouterPickerLocation confirmed(Uri uri) {
        return new AndroidBRouterPickerLocation(uri, true);
    }

    @Nullable
    Uri confirmedUri() {
        return confirmed ? initialUri : null;
    }

    private record Candidate(String id, Uri uri, boolean direct) {
        DirectoryPresence presence(Context context, CancellationSignal signal) {
            signal.throwIfCanceled();
            return direct ? DirectoryPresence.PRESENT
                    : AndroidDocumentAccess.externalStorageDirectoryPresence(context, id, signal);
        }

        boolean hasChild(Context context, String child, CancellationSignal signal) {
            return AndroidDocumentAccess.externalStorageDirectoryPresence(context, id + "/" + child, signal)
                    == DirectoryPresence.PRESENT;
        }

        AndroidBRouterPickerLocation preferOver(AndroidBRouterPickerLocation fallback, DirectoryPresence presence) {
            if (presence == DirectoryPresence.PRESENT && !fallback.confirmed) {
                return confirmed(uri);
            }
            return presence == DirectoryPresence.UNKNOWN && fallback.initialUri == null
                    ? new AndroidBRouterPickerLocation(uri, false) : fallback;
        }
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
