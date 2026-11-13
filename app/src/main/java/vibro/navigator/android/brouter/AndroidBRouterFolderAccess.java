package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterFolderGrant;

public final class AndroidBRouterFolderAccess {
    static final String FOLDER = "brouter";
    private static final String DOWNLOADS_AUTHORITY = "com.android.providers.downloads.documents";

    enum Kind {
        PROFILES("segments4"), SEGMENTS("profiles2");

        private final String otherFolder;

        Kind(String otherFolder) {
            this.otherFolder = otherFolder;
        }
    }

    private AndroidBRouterFolderAccess() {
    }

    public static boolean saveSelection(@NonNull Activity activity, @Nullable Intent data) {
        Uri uri = data == null ? null : data.getData();
        if (uri == null || !isVolumeAvailable(activity, uri) || !isParentFolder(activity, uri)
                || !AndroidDocumentAccess.persistReadPermission(activity, data, uri)) {
            return false;
        }
        BRouterFolderGrant.save(activity, uri);
        return true;
    }

    private static boolean isParentFolder(Context context, Uri uri) {
        if (!DocumentsContractCompat.isTreeUri(uri)) {
            return false;
        }
        String id = AndroidDocumentAccess.treeDocumentId(uri);
        if (id == null) {
            return false;
        }
        if (AndroidDocumentAccess.isExternalStorageDocument(uri)) {
            return isSelectableDocumentId(id) && FOLDER.equalsIgnoreCase(folderName(id));
        }
        Uri document = AndroidDocumentAccess.buildTreeDocumentUri(uri);
        return document != null && FOLDER.equalsIgnoreCase(AndroidDocumentAccess.displayName(context, document));
    }

    static boolean isSelectableDocumentId(String id) {
        String restricted = "Android/data/";
        int colon = id.indexOf(':');
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || colon < 0
                || !id.regionMatches(true, colon + 1, restricted, 0, restricted.length());
    }

    @Nullable
    static Uri dataDirectory(Context context, Uri parentTree, String folder) {
        if (!parentTree.equals(BRouterFolderGrant.get(context))) {
            return parentTree;
        }
        if (AndroidDocumentAccess.isExternalStorageDocument(parentTree)) {
            String parentId = AndroidDocumentAccess.treeDocumentId(parentTree);
            return parentId == null ? null
                    : AndroidDocumentAccess.descendantDocumentUri(parentTree, parentId + "/" + folder);
        }
        return AndroidDocumentAccess.childDocumentUri(context, parentTree, folder);
    }

    static boolean hasPersistedReadPermission(@NonNull Context context, @Nullable Uri uri, @NonNull Kind kind) {
        return uri != null && AndroidPersistedUriPermissions.hasReadPermission(context, uri)
                && isVolumeAvailable(context, uri)
                && (uri.equals(BRouterFolderGrant.get(context))
                        ? isParentFolder(context, uri) : isBRouterFolderSelection(context, uri, kind));
    }

    private static boolean isVolumeAvailable(Context context, Uri uri) {
        if (!AndroidDocumentAccess.isExternalStorageDocument(uri)) {
            return true;
        }
        String id = AndroidDocumentAccess.treeDocumentId(uri);
        int separator = id == null ? -1 : id.indexOf(':');
        if (separator <= 0) {
            return false;
        }
        String root = id.substring(0, separator);
        return "primary".equalsIgnoreCase(root) || AndroidStorageVolumes.secondaryStorageRootIds(context).contains(root);
    }

    private static boolean isBRouterFolderSelection(Context context, Uri uri, Kind kind) {
        if (!DocumentsContractCompat.isTreeUri(uri)) {
            return false;
        }
        String id = AndroidDocumentAccess.treeDocumentId(uri);
        if (id == null) {
            return false;
        }
        if (AndroidDocumentAccess.isExternalStorageDocument(uri)) {
            return isSelectableDocumentId(id) && !kind.otherFolder.equalsIgnoreCase(folderName(id))
                    && !"Download".equalsIgnoreCase(id.substring(id.indexOf(':') + 1));
        }
        if (DOWNLOADS_AUTHORITY.equals(uri.getAuthority()) && isDownloadsRoot(id)) {
            return false;
        }
        Uri document = AndroidDocumentAccess.buildTreeDocumentUri(uri);
        return document != null && !kind.otherFolder.equalsIgnoreCase(AndroidDocumentAccess.displayName(context, document));
    }

    private static boolean isDownloadsRoot(String id) {
        return "downloads".equals(id) || id.matches("raw:/(sdcard|storage/(emulated/\\d+|[^/]+))/Download");
    }

    private static String folderName(String documentId) {
        String path = documentId.replaceAll("/+$", "");
        return path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf(':')) + 1);
    }
}
