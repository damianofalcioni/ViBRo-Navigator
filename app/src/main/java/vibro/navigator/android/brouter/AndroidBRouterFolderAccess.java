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
        if (!isBRouterFolderSelection(uri, kind)) {
            AppLogger.w("BRouterFolderAccess", "Rejected BRouter folder selection uri=" + uri);
            return false;
        }
        return AndroidDocumentAccess.persistReadPermission(activity, data, uri);
    }

    static boolean hasPersistedReadPermission(@NonNull Context context, @Nullable Uri uri, @NonNull Kind kind) {
        return uri != null && isBRouterFolderSelection(uri, kind)
                && AndroidPersistedUriPermissions.hasReadPermission(context, uri);
    }

    private static boolean isBRouterFolderSelection(@NonNull Uri uri, @NonNull Kind kind) {
        if (!DocumentsContractCompat.isTreeUri(uri)) {
            return false;
        }
        String id = AndroidDocumentAccess.treeDocumentId(uri);
        if (id == null || kind.otherFolder.equalsIgnoreCase(folderName(id))) {
            return false;
        }
        if (AndroidDocumentAccess.isExternalStorageDocument(uri)) {
            return !"Download".equalsIgnoreCase(id.substring(id.indexOf(':') + 1));
        }
        if (DOWNLOADS_AUTHORITY.equals(uri.getAuthority())) {
            return !"downloads".equals(id) && !id.matches("raw:/(sdcard|storage/(emulated/\\d+|[^/]+))/Download");
        }
        return true;
    }

    private static String folderName(String documentId) {
        String path = documentId.replaceAll("/+$", "");
        return path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf(':')) + 1);
    }
}
