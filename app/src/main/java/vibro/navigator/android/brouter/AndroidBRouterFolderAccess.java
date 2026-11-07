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

    private AndroidBRouterFolderAccess() {
    }

    public static boolean persistReadPermission(@NonNull Activity activity, @NonNull Intent data, @NonNull Uri uri) {
        if (!isBRouterFolderSelection(uri)) {
            AppLogger.w("BRouterFolderAccess", "Rejected BRouter folder selection uri=" + uri);
            return false;
        }
        return AndroidDocumentAccess.persistReadPermission(activity, data, uri);
    }

    static boolean hasPersistedReadPermission(@NonNull Context context, @Nullable Uri uri) {
        return uri != null && isBRouterFolderSelection(uri)
                && AndroidPersistedUriPermissions.hasReadPermission(context, uri);
    }

    private static boolean isBRouterFolderSelection(@NonNull Uri uri) {
        if (!DocumentsContractCompat.isTreeUri(uri)) {
            return false;
        }
        String id = AndroidDocumentAccess.treeDocumentId(uri);
        if (id == null) {
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
}
