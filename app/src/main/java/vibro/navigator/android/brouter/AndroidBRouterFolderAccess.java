package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.MainThread;
import androidx.core.provider.DocumentsContractCompat;
import androidx.core.util.Consumer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterFolderGrant;
import vibro.navigator.brouter.BRouterFolderGrant.Child;

import vibro.navigator.android.brouter.AndroidBRouterDirectoryCheck.Directories;

public final class AndroidBRouterFolderAccess {
    static final String FOLDER = "brouter";
    private static final AtomicLong SELECTION_ORDER = new AtomicLong();
    private static long lastSavedSelection;
    private static long profilesOrder;
    private static long segmentsOrder;

    enum Kind {
        PROFILES("profiles2", "segments4"), SEGMENTS("segments4", "profiles2");

        final String folder;
        final String otherFolder;

        Kind(String folder, String otherFolder) {
            this.folder = folder;
            this.otherFolder = otherFolder;
        }
    }

    private AndroidBRouterFolderAccess() {
    }

    @MainThread
    public static void saveSelection(Activity activity, @Nullable Intent data, Consumer<Boolean> onResult) {
        Uri uri = data == null ? null : data.getData();
        if (uri == null || (data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
            onResult.accept(false);
            return;
        }
        long selection = SELECTION_ORDER.incrementAndGet();
        AtomicReference<Directories> verified = new AtomicReference<>(Directories.empty());
        AndroidBRouterFolderDiscovery.findWithFallback(
                signal -> AndroidBRouterDirectoryCheck.verify(activity, uri, signal, verified), verified::get,
                grant -> onResult.accept(saveVerifiedSelection(activity, data, uri, selection, grant)));
    }

    private static boolean saveVerifiedSelection(Activity activity, Intent data, Uri tree, long selection,
                                                 @Nullable Directories grant) {
        if (grant == null || grant.profiles().uri() == null && grant.segments().uri() == null
                || !AndroidDocumentAccess.persistReadPermission(activity, data, tree)) {
            return false;
        }
        Child profiles = prefer(grant.profiles(), selection >= profilesOrder);
        Child segments = prefer(grant.segments(), selection >= segmentsOrder);
        boolean latest = selection >= lastSavedSelection;
        lastSavedSelection = Math.max(lastSavedSelection, selection);
        if (profiles.prefer()) {
            profilesOrder = selection;
        }
        if (segments.prefer()) {
            segmentsOrder = selection;
        }
        BRouterFolderGrant.save(activity, tree, profiles, segments, latest);
        return true;
    }

    @Nullable
    static Uri dataDirectory(Context context, Uri tree, Kind kind) {
        boolean profiles = kind == Kind.PROFILES;
        if (BRouterFolderGrant.parents(context).contains(tree)) {
            return BRouterFolderGrant.directory(context, tree, profiles);
        }
        return tree;
    }

    public static boolean hasUsableParent(Context context) {
        for (Uri parent : BRouterFolderGrant.parents(context)) {
            if (hasUsableParent(context, parent)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasUsableParent(Context context, Uri parent) {
        return AndroidBRouterGrantedDirectories.hasKnownChild(context, parent, Kind.PROFILES) || AndroidBRouterGrantedDirectories.hasKnownChild(context, parent, Kind.SEGMENTS);
    }

    static boolean hasPersistedReadPermission(Context context, @Nullable Uri uri, Kind kind) {
        if (uri == null || !AndroidPersistedUriPermissions.hasReadPermission(context, uri)) {
            return false;
        }
        String id = treeId(uri);
        if (id == null || !isVolumeAvailable(context, uri, id)) {
            return false;
        }
        if (BRouterFolderGrant.parents(context).contains(uri)) {
            if (BRouterFolderGrant.directory(context, uri, kind == Kind.PROFILES) == null) {
                AndroidBRouterGrantedDirectories.refreshMissingDirectories(context, uri, id);
            }
            return AndroidBRouterGrantedDirectories.hasKnownChild(context, uri, kind);
        }
        // Old grants remain readable wherever Android continues to honor them.
        return !AndroidDocumentAccess.isExternalStorageDocument(uri) || !kind.otherFolder.equalsIgnoreCase(folderName(id));
    }

    private static Child prefer(Child child, boolean preferred) {
        return new Child(child.uri(), child.direct(), child.uri() != null && preferred);
    }

    static boolean isVolumeAvailable(Context context, Uri uri, @Nullable String id) {
        if (!AndroidDocumentAccess.isExternalStorageDocument(uri)) {
            return true;
        }
        int separator = id == null ? -1 : id.indexOf(':');
        if (separator <= 0) {
            return false;
        }
        String root = id.substring(0, separator);
        return "primary".equalsIgnoreCase(root) || "home".equalsIgnoreCase(root)
                || AndroidStorageVolumes.secondaryStorageRootIds(context).contains(root);
    }

    @Nullable
    static String treeId(Uri tree) {
        return DocumentsContractCompat.isTreeUri(tree) ? AndroidDocumentAccess.treeDocumentId(tree) : null;
    }

    static String folderName(String documentId) {
        String path = documentId.replaceAll("/+$", "");
        return path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf(':')) + 1);
    }

}
