package vibro.navigator.android.brouter;

import android.content.Context;
import android.net.Uri;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.android.brouter.AndroidBRouterDirectoryCheck.Directories;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.brouter.BRouterFolderGrant;
import vibro.navigator.brouter.BRouterFolderGrant.Child;

/** Rechecks incomplete parents and keeps status checks independent of document-provider calls. */
final class AndroidBRouterGrantedDirectories {
    private static final Set<Uri> REFRESHING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AndroidBRouterGrantedDirectories() { }

    static boolean hasKnownChild(Context context, Uri parent, AndroidBRouterFolderAccess.Kind kind) {
        boolean profiles = kind == AndroidBRouterFolderAccess.Kind.PROFILES;
        Uri directory = BRouterFolderGrant.directory(context, parent, profiles);
        return directory != null && AndroidPersistedUriPermissions.hasReadPermission(context, parent)
                && AndroidBRouterFolderAccess.isVolumeAvailable(context, parent, AndroidDocumentAccess.treeDocumentId(parent))
                && (!BRouterFolderGrant.isDirect(context, parent, profiles)
                    || directChildStillPresent(context, directory));
    }

    private static boolean directChildStillPresent(Context context, Uri directory) {
        String id = AndroidDocumentAccess.directoryDocumentId(directory);
        return id != null && AndroidDocumentAccess.directDirectoryPresence(context, id)
                != AndroidDocumentAccess.DirectoryPresence.MISSING;
    }

    static void refreshMissingDirectories(Context context, Uri tree, String id) {
        if (!REFRESHING.add(tree)) {
            return;
        }
        Context app = context.getApplicationContext();
        AtomicReference<Directories> verified = new AtomicReference<>(Directories.empty());
        AndroidBRouterFolderDiscovery.findWithFallback(
                signal -> AndroidBRouterDirectoryCheck.inspect(app, tree, id, signal, verified), verified::get,
                directories -> onDirectoriesRefreshed(app, tree, directories));
    }

    private static void onDirectoriesRefreshed(Context context, Uri tree, Directories directories) {
        try {
            if (!AndroidPersistedUriPermissions.hasReadPermission(context, tree)) {
                return;
            }
            Child profiles = missingChild(context, tree, directories.profiles(), true);
            Child segments = missingChild(context, tree, directories.segments(), false);
            if (profiles.uri() != null || segments.uri() != null) {
                BRouterFolderGrant.save(context, tree, profiles, segments, false);
            }
        } finally {
            REFRESHING.remove(tree);
        }
    }

    private static Child missingChild(Context context, Uri tree, Child child, boolean profiles) {
        return BRouterFolderGrant.directory(context, tree, profiles) == null
                ? new Child(child.uri(), child.direct(), BRouterFolderGrant.parentFor(context, profiles) == null)
                : new Child(null, false, false);
    }

}
