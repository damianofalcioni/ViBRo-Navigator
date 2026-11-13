package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.logging.AppLogger;

public final class AndroidBRouterFolderAccess {
    private static final String DOWNLOADS_AUTHORITY = "com.android.providers.downloads.documents";

    public enum Kind {
        PROFILES("segments4"), SEGMENTS("profiles2");

        private final String otherFolder;

        Kind(String otherFolder) {
            this.otherFolder = otherFolder;
        }
    }

    private AndroidBRouterFolderAccess() {
    }

    @Nullable
    public static Uri persistSelection(@NonNull Activity activity, @Nullable Intent data, @NonNull Kind kind) {
        Uri uri = data == null ? null : data.getData();
        return uri != null && persistReadPermission(activity, data, uri, kind) ? uri : null;
    }

    public static boolean persistReadPermission(@NonNull Activity activity, @NonNull Intent data, @NonNull Uri uri,
                                                @NonNull Kind kind) {
        if (!isVolumeAvailable(activity, uri) || !isBRouterFolderSelection(activity, uri, kind)) {
            AppLogger.w("BRouterFolderAccess", "Rejected BRouter folder selection uri=" + uri);
            return false;
        }
        return AndroidDocumentAccess.persistReadPermission(activity, data, uri);
    }

    static boolean hasPersistedReadPermission(@NonNull Context context, @Nullable Uri uri, @NonNull Kind kind) {
        return uri != null && AndroidPersistedUriPermissions.hasReadPermission(context, uri)
                && isVolumeAvailable(context, uri)
                && isBRouterFolderSelection(context, uri, kind);
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
            return !kind.otherFolder.equalsIgnoreCase(folderName(id))
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
