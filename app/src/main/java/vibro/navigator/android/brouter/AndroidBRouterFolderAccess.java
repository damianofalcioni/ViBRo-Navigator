package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;

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
import vibro.navigator.brouter.BRouterFolderGrant.SelectionKind;

import vibro.navigator.android.brouter.AndroidBRouterDirectoryCheck.Directories;

public final class AndroidBRouterFolderAccess {
    static final String FOLDER = "brouter";
    private static final AtomicLong SELECTION_ORDER = new AtomicLong();

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
        saveSelection(activity, data, onResult, false);
    }

    @MainThread
    public static void saveStreetSelection(Activity activity, @Nullable Intent data, Consumer<Boolean> onResult) {
        saveSelection(activity, data, onResult, true);
    }

    private static void saveSelection(Activity activity, @Nullable Intent data, Consumer<Boolean> onResult,
                                      boolean streetsOnly) {
        Uri uri = data == null ? null : data.getData();
        if (uri == null || (data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
            onResult.accept(false);
            return;
        }
        long selection = SELECTION_ORDER.incrementAndGet();
        AtomicReference<Directories> verified = new AtomicReference<>(Directories.empty());
        AndroidBRouterFolderDiscovery.findWithFallback(
                signal -> verifySelection(activity, uri, signal, verified),
                () -> new Selection(verified.get(), false),
                grant -> onResult.accept(saveVerifiedSelection(activity, data, uri, selection, grant, streetsOnly)));
    }

    private static Selection verifySelection(Context context, Uri tree, CancellationSignal signal,
                                             AtomicReference<Directories> verified) {
        Directories parent = AndroidBRouterDirectoryCheck.verify(context, tree, signal, verified);
        boolean directTiles = parent != null && !parent.hasData()
                && AndroidBRouterTileDirectory.hasTiles(context, tree, signal);
        return new Selection(parent, directTiles);
    }

    private record Selection(@Nullable Directories parent, boolean directTiles) {
        private static long lastSharedSelection;
        private static long profilesOrder;
        private static long segmentsOrder;

        boolean usable(boolean streetsOnly) {
            return directTiles || (parent != null && (streetsOnly ? parent.segments().uri() != null : parent.hasData()));
        }

        void saveParent(Context context, Uri tree, long selection, boolean streetsOnly) {
            Child segments = prefer(parent.segments(), selection >= segmentsOrder);
            if (segments.prefer()) {
                segmentsOrder = selection;
            }
            if (streetsOnly) {
                BRouterFolderGrant.save(context, tree, new Child(null, false, false), segments, false, SelectionKind.STREETS);
                return;
            }
            Child profiles = prefer(parent.profiles(), selection >= profilesOrder);
            boolean latest = selection >= lastSharedSelection;
            lastSharedSelection = Math.max(lastSharedSelection, selection);
            if (profiles.prefer()) {
                profilesOrder = selection;
            }
            BRouterFolderGrant.save(context, tree, profiles, segments, latest, SelectionKind.SHARED);
        }
    }

    private static boolean saveVerifiedSelection(Activity activity, Intent data, Uri tree, long selection,
                                                 @Nullable Selection grant, boolean streetsOnly) {
        if (grant == null || !grant.usable(streetsOnly)
                || !AndroidDocumentAccess.persistReadPermission(activity, data, tree)) {
            return false;
        }
        if (grant.directTiles()) {
            AndroidBRouterTileDirectory.save(activity, tree, selection);
        } else {
            grant.saveParent(activity, tree, selection, streetsOnly);
        }
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
            if (!AndroidBRouterGrantedDirectories.hasKnownChild(context, uri, kind)) {
                boolean localChild = AndroidDocumentAccess.isExternalStorageDocument(uri)
                        && AndroidDocumentAccess.canReadExternalDirectory(context, id + "/" + kind.folder);
                AndroidBRouterGrantedDirectories.refreshMissingDirectories(context, uri, id, localChild);
            }
            return AndroidBRouterGrantedDirectories.hasKnownChild(context, uri, kind);
        }
        // Old grants remain readable wherever Android continues to honor them.
        return !AndroidDocumentAccess.isExternalStorageDocument(uri) || !kind.otherFolder.equalsIgnoreCase(folderName(id));
    }

    static void recheckSharedFolder(Context context, Uri tree) {
        String id = treeId(tree);
        if (id != null && BRouterFolderGrant.parents(context).contains(tree)
                && AndroidPersistedUriPermissions.hasReadPermission(context, tree)) {
            AndroidBRouterGrantedDirectories.refreshMissingDirectories(context, tree, id, false);
        }
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
