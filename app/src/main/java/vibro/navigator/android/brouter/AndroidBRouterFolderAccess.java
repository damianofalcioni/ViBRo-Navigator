package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.MainThread;
import androidx.core.provider.DocumentsContractCompat;
import androidx.core.util.Consumer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterFolderGrant;

import vibro.navigator.android.brouter.AndroidBRouterDirectoryCheck.Directories;

public final class AndroidBRouterFolderAccess {
    static final String FOLDER = "brouter";
    private static final AtomicBoolean MIGRATING = new AtomicBoolean();
    private static final AtomicLong SELECTION_ORDER = new AtomicLong();
    private static long lastSavedSelection;

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
        AtomicReference<Directories> verified = new AtomicReference<>(new Directories(null, null));
        AndroidBRouterFolderDiscovery.findWithFallback(
                signal -> AndroidBRouterDirectoryCheck.verify(activity, uri, signal, verified), verified::get,
                grant -> onResult.accept(saveVerifiedSelection(activity, data, uri, selection, grant)));
    }

    private static boolean saveVerifiedSelection(Activity activity, Intent data, Uri tree, long selection,
                                                 @Nullable Directories grant) {
        if (grant == null || grant.isEmpty()) {
            return false;
        }
        if (selection < lastSavedSelection) {
            return true;
        }
        if (!AndroidDocumentAccess.persistReadPermission(activity, data, tree)) {
            return false;
        }
        lastSavedSelection = selection;
        BRouterFolderGrant.save(activity, tree, grant.profiles(), grant.segments());
        return true;
    }

    @Nullable
    static Uri dataDirectory(Context context, Uri tree, Kind kind) {
        boolean profiles = kind == Kind.PROFILES;
        if (tree.equals(BRouterFolderGrant.parentFor(context, profiles))) {
            return BRouterFolderGrant.directory(context, profiles);
        }
        return tree.equals(BRouterFolderGrant.get(context)) ? null : tree;
    }

    public static boolean hasUsableParent(Context context) {
        return hasPersistedReadPermission(context, BRouterFolderGrant.parentFor(context, true), Kind.PROFILES)
                || hasPersistedReadPermission(context, BRouterFolderGrant.parentFor(context, false), Kind.SEGMENTS);
    }

    static boolean hasUsableParent(Context context, Uri parent) {
        return parent.equals(BRouterFolderGrant.parentFor(context, true))
                && hasPersistedReadPermission(context, parent, Kind.PROFILES)
                || parent.equals(BRouterFolderGrant.parentFor(context, false))
                && hasPersistedReadPermission(context, parent, Kind.SEGMENTS);
    }

    static boolean hasPersistedReadPermission(Context context, @Nullable Uri uri, Kind kind) {
        if (uri == null || !AndroidPersistedUriPermissions.hasReadPermission(context, uri)) {
            return false;
        }
        String id = treeId(uri);
        if (id == null || !isVolumeAvailable(context, uri, id)) {
            return false;
        }
        boolean profiles = kind == Kind.PROFILES;
        if (uri.equals(BRouterFolderGrant.parentFor(context, profiles))) {
            return true;
        }
        if (uri.equals(BRouterFolderGrant.get(context))) {
            refreshMissingDirectories(context, uri, id);
            return uri.equals(BRouterFolderGrant.parentFor(context, profiles));
        }
        // Old grants remain readable wherever Android continues to honor them.
        return !AndroidDocumentAccess.isExternalStorageDocument(uri) || !kind.otherFolder.equalsIgnoreCase(folderName(id));
    }

    private static void refreshMissingDirectories(Context context, Uri tree, String id) {
        if (!MIGRATING.compareAndSet(false, true)) {
            return;
        }
        Context app = context.getApplicationContext();
        AtomicReference<Directories> verified = new AtomicReference<>(new Directories(null, null));
        AndroidBRouterFolderDiscovery.findWithFallback(
                signal -> AndroidBRouterDirectoryCheck.inspect(app, tree, id, signal, verified), verified::get,
                directories -> onDirectoriesRefreshed(app, tree, directories));
    }

    private static void onDirectoriesRefreshed(Context context, Uri tree, Directories directories) {
        MIGRATING.set(false);
        if (!tree.equals(BRouterFolderGrant.get(context))) {
            return;
        }
        Uri profiles = tree.equals(BRouterFolderGrant.parentFor(context, true)) ? null : directories.profiles();
        Uri segments = tree.equals(BRouterFolderGrant.parentFor(context, false)) ? null : directories.segments();
        if (profiles != null || segments != null) {
            BRouterFolderGrant.save(context, tree, profiles, segments);
        }
    }

    static boolean isVolumeAvailable(Context context, Uri uri, String id) {
        if (!AndroidDocumentAccess.isExternalStorageDocument(uri)) {
            return true;
        }
        int separator = id.indexOf(':');
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
