package vibro.navigator.android.brouter;

import android.content.Context;
import android.net.Uri;
import android.os.CancellationSignal;

import java.util.regex.Pattern;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidDocumentTreeDirectories;

/** Verifies an explicitly selected tile directory, including BRouter's secondary segment source. */
final class AndroidBRouterTileDirectory {
    private static final Pattern TILE_NAME = Pattern.compile("[EW][0-9]{1,3}_[NS][0-9]{1,2}\\.rd5");

    private static long lastSavedSelection;

    private AndroidBRouterTileDirectory() { }

    static void save(Context context, Uri tree, long selection) {
        if (selection >= lastSavedSelection) {
            lastSavedSelection = selection;
            AndroidBRouterSegmentsRepositoryFactory.create().saveSegmentsTreeUri(context, tree);
        }
    }

    static boolean hasTiles(Context context, Uri tree, CancellationSignal signal) {
        String id = AndroidBRouterFolderAccess.treeId(tree);
        if (id == null || isDownloadsRoot(tree, id) || !AndroidBRouterFolderAccess.isVolumeAvailable(context, tree, id)) {
            return false;
        }
        if (AndroidDocumentAccess.isExternalStorageDocument(tree) && !isSelectableTileDirectory(id)) {
            return false;
        }
        return AndroidDocumentTreeDirectories.hasFileMatching(context, tree, id, TILE_NAME, signal);
    }

    private static boolean isDownloadsRoot(Uri tree, String id) {
        return "com.android.providers.downloads.documents".equals(tree.getAuthority())
                && "downloads".equals(id);
    }

    private static boolean isSelectableTileDirectory(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        return !path.isEmpty() && !"Download".equalsIgnoreCase(path)
                && !"profiles2".equalsIgnoreCase(AndroidBRouterFolderAccess.folderName(id))
                && AndroidBRouterDirectoryCheck.isSelectableDocumentId(id);
    }
}
