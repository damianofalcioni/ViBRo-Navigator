package vibro.navigator.android.brouter;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidDocumentTreeDirectories;
import vibro.navigator.brouter.BRouterFolderGrant.Child;

import static vibro.navigator.android.brouter.AndroidBRouterFolderAccess.FOLDER;
import static vibro.navigator.android.brouter.AndroidBRouterFolderAccess.Kind.PROFILES;
import static vibro.navigator.android.brouter.AndroidBRouterFolderAccess.Kind.SEGMENTS;

/** Verifies the selected tree and publishes each readable child as soon as it is known. */
final class AndroidBRouterDirectoryCheck {
    private static final String RESTRICTED = "Android/data/";

    record Directories(Child profiles, Child segments) {
        static Directories empty() {
            return new Directories(child(null, false), child(null, false));
        }
    }

    private static Child child(@Nullable Uri uri, boolean direct) {
        return new Child(uri, direct, false);
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
        boolean external = AndroidDocumentAccess.isExternalStorageDocument(tree);
        if (!validExternalParent(context, tree, id, signal, external && validateParent)) {
            return verified.get();
        }
        Directories direct = new Directories(child(directChild(context, tree, id + "/" + PROFILES.folder), true),
                child(directChild(context, tree, id + "/" + SEGMENTS.folder), true));
        verified.set(direct);
        if (direct.profiles().uri() == null || direct.segments().uri() == null) {
            inspectProvider(context, tree, id, signal, verified, !external && validateParent);
        }
        return verified.get();
    }

    private static boolean validExternalParent(Context context, Uri tree, String id,
                                               CancellationSignal signal, boolean validate) {
        return !validate || isParentFolder(context, tree, id, signal);
    }

    private static void inspectProvider(Context context, Uri tree, String id, CancellationSignal signal,
                                        AtomicReference<Directories> verified, boolean validate) {
        if (!validate || isParentFolder(context, tree, id, signal)) {
            verifyProviderChildren(context, tree, signal, verified);
        }
    }

    private static void verifyProviderChildren(Context context, Uri tree, CancellationSignal signal,
                                               AtomicReference<Directories> verified) {
        Map<String, Uri> directories = AndroidDocumentTreeDirectories.childDirectories(context, tree, signal);
        Directories direct = verified.get();
        Uri profiles = direct.profiles().uri() == null ? directories.get(PROFILES.folder) : null;
        Uri segments = direct.segments().uri() == null ? directories.get(SEGMENTS.folder) : null;
        if (profiles == null || segments == null) {
            verifyProfile(context, profiles, signal, verified);
            verifySegments(context, segments, signal, verified);
            return;
        }
        verifyBoth(context, profiles, segments, signal, verified);
    }

    private static void verifyBoth(Context context, Uri profiles, Uri segments, CancellationSignal signal,
                                   AtomicReference<Directories> verified) {
        CancellationSignal profileSignal = new CancellationSignal();
        FutureTask<Void> profileLookup = new FutureTask<>(() -> verifyProfile(context, profiles, profileSignal, verified), null);
        if (!AndroidBRouterFolderDiscovery.executeChildLookup(profileLookup)) {
            verifySegments(context, segments, signal, verified);
            return;
        }
        signal.setOnCancelListener(() -> AndroidBRouterFolderDiscovery.cancelTask(profileLookup, profileSignal));
        try {
            verifySegments(context, segments, signal, verified);
            profileLookup.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
            // The other directory can still supply a usable grant.
        } finally {
            signal.setOnCancelListener(null);
            AndroidBRouterFolderDiscovery.cancelTask(profileLookup, profileSignal);
        }
    }

    private static void verifyProfile(Context context, @Nullable Uri uri, CancellationSignal signal,
                                      AtomicReference<Directories> verified) {
        if (verifyChild(context, uri, signal)) {
            synchronized (verified) {
                verified.set(new Directories(child(uri, false), verified.get().segments()));
            }
        }
    }

    private static void verifySegments(Context context, @Nullable Uri uri, CancellationSignal signal,
                                       AtomicReference<Directories> verified) {
        if (verifyChild(context, uri, signal)) {
            synchronized (verified) {
                verified.set(new Directories(verified.get().profiles(), child(uri, false)));
            }
        }
    }

    private static boolean verifyChild(Context context, @Nullable Uri listed, CancellationSignal signal) {
        if (listed == null) {
            return false;
        }
        signal.throwIfCanceled();
        return AndroidDocumentTreeDirectories.canList(context, listed, signal);
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
