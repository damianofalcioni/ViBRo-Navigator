package vibro.navigator.android.storage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

public final class AndroidOutputFolderAccess {
    private static final Map<Kind, String> FAILED_FOLDERS = new ConcurrentHashMap<>();
    private static final int ACCESS_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

    private AndroidOutputFolderAccess() {
    }

    @NonNull
    public static Intent picker(@NonNull Context context, @NonNull Kind kind) {
        return AndroidDocumentAccess.openDocumentTreeIntent(tree(context, kind))
                .addFlags(ACCESS_FLAGS | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
    }

    public static void accept(@NonNull Context context, @NonNull Kind kind, @NonNull Intent result)
            throws IOException {
        Uri uri = result.getData();
        if (uri == null || (result.getFlags() & ACCESS_FLAGS) != ACCESS_FLAGS
                || (result.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) == 0) {
            throw new IOException("Folder picker did not provide persistent read/write access");
        }
        context.getContentResolver().takePersistableUriPermission(uri, ACCESS_FLAGS);
        AndroidWritableDocumentTree.verifyWrite(context, uri);
        AppOutputFolderSettings.set(context, kind, uri.toString());
        markAvailable(kind);
    }

    @Nullable
    public static Uri ensureFolder(Context context, Kind kind) throws IOException {
        Uri selected = tree(context, kind);
        return selected == null ? null : ensureFolder(context, selected);
    }

    @NonNull
    public static Uri ensureFolder(Context context, @NonNull Uri selected) throws IOException {
        if (!AndroidOutputFolderRecovery.hasGrant(context, selected)) {
            throw new IOException("Output folder permission is missing");
        }
        Uri access = AndroidOutputFolderRecovery.accessUri(context, selected);
        if (AndroidOutputFolderRecovery.writableDirectory(context, access)) {
            return access;
        }
        Uri restored = AndroidOutputFolderRecovery.restore(context, selected);
        if (!AndroidOutputFolderRecovery.writableDirectory(context, restored)) {
            throw new IOException("Output folder is not writable");
        }
        return restored;
    }

    public static boolean isUsable(Context context, Kind kind) {
        try {
            Uri folder = ensureFolder(context, kind);
            if (folder == null) {
                return false;
            }
            if (isFailureMarked(context, kind)) {
                AndroidWritableDocumentTree.verifyWrite(context, folder);
                markAvailable(kind);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            markUnavailable(context, kind);
            return false;
        }
    }

    public static boolean isFailureMarked(Context context, Kind kind) {
        String selected = AppOutputFolderSettings.get(context, kind);
        return selected != null && selected.equals(FAILED_FOLDERS.get(kind));
    }

    public static void markUnavailable(Context context, Kind kind) {
        String selected = AppOutputFolderSettings.get(context, kind);
        if (selected != null) {
            FAILED_FOLDERS.put(kind, selected);
        }
    }

    public static void markAvailable(Kind kind) {
        FAILED_FOLDERS.remove(kind);
    }

    @Nullable
    public static Uri tree(@NonNull Context context, @NonNull Kind kind) {
        String value = AppOutputFolderSettings.get(context, kind);
        return value == null ? null : Uri.parse(value);
    }

    @NonNull
    public static String label(@NonNull Context context, @NonNull Kind kind) {
        Uri uri = tree(context, kind);
        if (uri == null) {
            return "";
        }
        try {
            return DocumentsContract.getTreeDocumentId(uri);
        } catch (IllegalArgumentException e) {
            return uri.toString();
        }
    }
}
