package vibro.navigator.android.brouter;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

import java.util.Collections;
import java.util.Set;
import java.util.Objects;
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
    private static final ConcurrentHashMap<Uri, Long> NEXT_REFRESH = new ConcurrentHashMap<>();
    private static final long REFRESH_INTERVAL_MS = 5_000L;

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

    static void refreshMissingDirectories(Context context, Uri tree, String id, boolean newDirectChild) {
        long now = SystemClock.elapsedRealtime();
        Long next = NEXT_REFRESH.get(tree);
        if (!newDirectChild && next != null && next > now) {
            return;
        }
        if (!REFRESHING.add(tree)) {
            return;
        }
        Context app = context.getApplicationContext();
        NEXT_REFRESH.put(tree, now + REFRESH_INTERVAL_MS);
        Directories before = new Directories(storedChild(app, tree, true), storedChild(app, tree, false));
        AtomicReference<Directories> verified = new AtomicReference<>(Directories.empty());
        AndroidBRouterFolderDiscovery.findWithFallback(
                signal -> AndroidBRouterDirectoryCheck.inspect(app, tree, id, signal, verified), verified::get,
                directories -> onDirectoriesRefreshed(app, tree, before, directories));
    }

    private static void onDirectoriesRefreshed(Context context, Uri tree, Directories before, Directories directories) {
        try {
            if (!AndroidPersistedUriPermissions.hasReadPermission(context, tree)) {
                return;
            }
            Child profiles = changedChild(context, tree, before.profiles(), directories.profiles(), true);
            Child segments = changedChild(context, tree, before.segments(), directories.segments(), false);
            if (profiles.uri() != null || segments.uri() != null) {
                BRouterFolderGrant.save(context, tree, profiles, segments, false, false);
            }
        } finally {
            REFRESHING.remove(tree);
        }
    }

    private static Child changedChild(Context context, Uri tree, Child before, Child child, boolean profiles) {
        Child current = storedChild(context, tree, profiles);
        if (child.uri() == null || child.equals(current) || !before.equals(current)) {
            return new Child(null, false, false);
        }
        Uri preferred = BRouterFolderGrant.parentFor(context, profiles);
        return new Child(child.uri(), child.direct(), preferred == null || Objects.equals(preferred, tree));
    }

    private static Child storedChild(Context context, Uri tree, boolean profiles) {
        return new Child(BRouterFolderGrant.directory(context, tree, profiles),
                BRouterFolderGrant.isDirect(context, tree, profiles), false);
    }
}
