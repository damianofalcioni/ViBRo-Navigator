package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;
import androidx.core.util.Consumer;
import androidx.core.util.Supplier;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterFolderGrant;

public final class AndroidBRouterFolderAccess {
    static final String FOLDER = "brouter";
    private static final AtomicBoolean MIGRATING = new AtomicBoolean();

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

    public static void saveSelection(Activity activity, @Nullable Intent data, Consumer<Boolean> onResult) {
        Uri uri = data == null ? null : data.getData();
        if (uri == null || (data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
            onResult.accept(false);
            return;
        }
        AndroidBRouterFolderDiscovery.find(
                signal -> verifySelection(activity, uri, signal),
                null, grant -> {
                    boolean saved = grant != null && AndroidDocumentAccess.persistReadPermission(activity, data, uri);
                    if (saved) {
                        BRouterFolderGrant.save(activity, uri, grant.profiles, grant.segments);
                    }
                    onResult.accept(saved);
                });
    }

    @Nullable
    private static Directories verifySelection(Context context, Uri tree, CancellationSignal signal) {
        String id = treeId(tree);
        if (id == null || !isVolumeAvailable(context, tree, id) || !isParentFolder(context, tree, id, signal)) {
            return null;
        }
        Directories directories = inspectDirectories(context, tree, id, signal);
        return directories.isEmpty() ? null : directories;
    }

    private static Directories inspectDirectories(Context context, Uri tree, String id, CancellationSignal signal) {
        Uri profiles = directChild(context, tree, id, Kind.PROFILES.folder);
        Uri segments = directChild(context, tree, id, Kind.SEGMENTS.folder);
        if (profiles != null && segments != null) {
            return new Directories(profiles, segments);
        }
        Map<String, Uri> directories = ProviderLookups.run(
                tree, () -> AndroidDocumentAccess.childDirectories(context, tree, signal), Collections.emptyMap());
        return new Directories(preferDirect(profiles, directories.get(Kind.PROFILES.folder)),
                preferDirect(segments, directories.get(Kind.SEGMENTS.folder)));
    }

    @Nullable
    private static Uri preferDirect(@Nullable Uri direct, @Nullable Uri listed) {
        return direct == null ? listed : direct;
    }

    @Nullable
    private static Uri directChild(Context context, Uri tree, String id, String folder) {
        String childId = id + "/" + folder;
        return AndroidDocumentAccess.isExternalStorageDocument(tree)
                && AndroidDocumentAccess.canReadExternalDirectory(context, childId)
                ? AndroidDocumentAccess.descendantDocumentUri(tree, childId) : null;
    }

    private static boolean isParentFolder(Context context, Uri uri, String id, CancellationSignal signal) {
        if (AndroidDocumentAccess.isExternalStorageDocument(uri)) {
            return isSelectableDocumentId(id) && FOLDER.equalsIgnoreCase(folderName(id));
        }
        Uri document = AndroidDocumentAccess.buildTreeDocumentUri(uri);
        return document != null && FOLDER.equalsIgnoreCase(ProviderLookups.run(
                uri, () -> AndroidDocumentAccess.displayName(context, document, signal), null));
    }

    static boolean isSelectableDocumentId(String id) {
        String restricted = "Android/data/";
        int colon = id.indexOf(':');
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || colon < 0
                || !id.regionMatches(true, colon + 1, restricted, 0, restricted.length());
    }

    @Nullable
    static Uri dataDirectory(Context context, Uri tree, Kind kind) {
        return tree.equals(BRouterFolderGrant.get(context))
                ? BRouterFolderGrant.directory(context, kind == Kind.PROFILES) : tree;
    }

    static boolean hasPersistedReadPermission(Context context, @Nullable Uri uri, Kind kind) {
        if (uri == null || !AndroidPersistedUriPermissions.hasReadPermission(context, uri)) {
            return false;
        }
        String id = treeId(uri);
        if (id == null || !isVolumeAvailable(context, uri, id)) {
            return false;
        }
        if (uri.equals(BRouterFolderGrant.get(context))) {
            verifyPreviousParent(context, uri, id);
            return BRouterFolderGrant.directory(context, kind == Kind.PROFILES) != null;
        }
        // Old grants remain readable wherever Android continues to honor them.
        return !AndroidDocumentAccess.isExternalStorageDocument(uri) || !kind.otherFolder.equalsIgnoreCase(folderName(id));
    }

    private static void verifyPreviousParent(Context context, Uri tree, String id) {
        if (BRouterFolderGrant.directoriesVerified(context) || !MIGRATING.compareAndSet(false, true)) {
            return;
        }
        Context app = context.getApplicationContext();
        AndroidBRouterFolderDiscovery.find(
                signal -> inspectDirectories(app, tree, id, signal),
                null, directories -> {
                    MIGRATING.set(false);
                    if (directories != null && tree.equals(BRouterFolderGrant.get(app))
                            && !BRouterFolderGrant.directoriesVerified(app)) {
                        BRouterFolderGrant.save(app, tree, directories.profiles, directories.segments);
                    }
                });
    }

    private static boolean isVolumeAvailable(Context context, Uri uri, String id) {
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
    private static String treeId(Uri tree) {
        return DocumentsContractCompat.isTreeUri(tree) ? AndroidDocumentAccess.treeDocumentId(tree) : null;
    }

    private static String folderName(String documentId) {
        String path = documentId.replaceAll("/+$", "");
        return path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf(':')) + 1);
    }

    private record Directories(@Nullable Uri profiles, @Nullable Uri segments) {
        boolean isEmpty() {
            return profiles == null && segments == null;
        }
    }

    private static final class ProviderLookups {
        private static final Set<String> ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());

        @Nullable
        static <T> T run(Uri tree, Supplier<T> work, @Nullable T fallback) {
            String authority = tree.getAuthority();
            if (authority == null || !ACTIVE.add(authority)) {
                return fallback;
            }
            try {
                return work.get();
            } finally {
                ACTIVE.remove(authority);
            }
        }
    }
}
