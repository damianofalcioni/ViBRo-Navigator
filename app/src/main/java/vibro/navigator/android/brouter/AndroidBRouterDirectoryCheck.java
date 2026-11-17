package vibro.navigator.android.brouter;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidDocumentTreeDirectories;

import static vibro.navigator.android.brouter.AndroidBRouterFolderAccess.FOLDER;
import static vibro.navigator.android.brouter.AndroidBRouterFolderAccess.Kind.PROFILES;
import static vibro.navigator.android.brouter.AndroidBRouterFolderAccess.Kind.SEGMENTS;

/** Verifies the selected tree and publishes each readable child as soon as it is known. */
final class AndroidBRouterDirectoryCheck {
    private static final Set<Uri> ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final String RESTRICTED = "Android/data/";

    record Directories(@Nullable Uri profiles, @Nullable Uri segments) {
        boolean isEmpty() {
            return profiles == null && segments == null;
        }
    }

    private AndroidBRouterDirectoryCheck() { }

    @Nullable
    static Directories verify(Context context, Uri tree, CancellationSignal signal,
                              AtomicReference<Directories> verified) {
        String id = AndroidBRouterFolderAccess.treeId(tree);
        if (id == null || !AndroidBRouterFolderAccess.isVolumeAvailable(context, tree, id)) {
            return null;
        }
        return inspectTree(context, tree, id, signal, verified, true);
    }

    static Directories inspect(Context context, Uri tree, String id, CancellationSignal signal,
                               AtomicReference<Directories> verified) {
        return inspectTree(context, tree, id, signal, verified, false);
    }

    private static Directories inspectTree(Context context, Uri tree, String id, CancellationSignal signal,
                                           AtomicReference<Directories> verified, boolean validateParent) {
        if (!ACTIVE.add(tree)) {
            return verified.get();
        }
        try {
            if (!validateParent || isParentFolder(context, tree, id, signal)) {
                inspectChildren(context, tree, id, signal, verified);
            }
            return verified.get();
        } finally {
            ACTIVE.remove(tree);
        }
    }

    private static void inspectChildren(Context context, Uri tree, String id, CancellationSignal signal,
                                        AtomicReference<Directories> verified) {
        Directories direct = new Directories(directChild(context, tree, id + "/" + PROFILES.folder),
                directChild(context, tree, id + "/" + SEGMENTS.folder));
        verified.set(direct);
        if (direct.profiles == null || direct.segments == null) {
            verifyProviderChildren(context, tree, signal, verified);
        }
    }

    private static void verifyProviderChildren(Context context, Uri tree, CancellationSignal signal,
                                               AtomicReference<Directories> verified) {
        Map<String, Uri> directories = AndroidDocumentTreeDirectories.childDirectories(context, tree, signal);
        Directories direct = verified.get();
        Uri profiles = verifyChild(context, direct.profiles, directories.get(PROFILES.folder), signal);
        verified.set(new Directories(profiles, direct.segments));
        Uri segments = verifyChild(context, direct.segments, directories.get(SEGMENTS.folder), signal);
        verified.set(new Directories(profiles, segments));
    }

    @Nullable
    private static Uri verifyChild(Context context, @Nullable Uri direct, @Nullable Uri listed,
                                   CancellationSignal signal) {
        signal.throwIfCanceled();
        if (direct != null) {
            return direct;
        }
        return listed != null && AndroidDocumentTreeDirectories.canList(context, listed, signal) ? listed : null;
    }

    @Nullable
    private static Uri directChild(Context context, Uri tree, String childId) {
        return AndroidDocumentAccess.isExternalStorageDocument(tree)
                && AndroidDocumentAccess.canReadExternalDirectory(context, childId)
                ? AndroidDocumentAccess.descendantDocumentUri(tree, childId) : null;
    }

    private static boolean isParentFolder(Context context, Uri tree, String id, CancellationSignal signal) {
        if (AndroidDocumentAccess.isExternalStorageDocument(tree)) {
            return isSelectableDocumentId(id) && FOLDER.equalsIgnoreCase(AndroidBRouterFolderAccess.folderName(id));
        }
        Uri document = AndroidDocumentAccess.buildTreeDocumentUri(tree);
        return document != null && FOLDER.equalsIgnoreCase(AndroidDocumentAccess.displayName(context, document, signal));
    }

    static boolean isSelectableDocumentId(String id) {
        int colon = id.indexOf(':');
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || colon < 0
                || "home".equalsIgnoreCase(id.substring(0, colon))
                || !id.regionMatches(true, colon + 1, RESTRICTED, 0, RESTRICTED.length());
    }
}
