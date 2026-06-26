package vibro.navigator.android.brouter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

import vibro.navigator.android.storage.AndroidFileReadFile;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterSegmentReadFile;

final class AndroidBRouterSegmentFileAccess {
    private AndroidBRouterSegmentFileAccess() {
    }

    static boolean canReadFiles(@NonNull Context context) {
        return AndroidLegacyExternalStorageAccess.canReadSharedExternalFiles(context);
    }

    @Nullable
    static BRouterSegmentReadFile openReadFile(
            @NonNull Context context,
            @NonNull String directoryDocumentId,
            @NonNull String fileName
    ) throws IOException {
        if (!canReadFiles(context)) {
            return null;
        }
        File directory = toDirectory(context, directoryDocumentId);
        if (directory == null) {
            return null;
        }
        File file = new File(directory, fileName);
        return file.isFile() ? AndroidFileReadFile.open(file) : null;
    }

    @Nullable
    private static File toDirectory(@NonNull Context context, @NonNull String documentId) {
        int separator = documentId.indexOf(':');
        if (separator <= 0 || separator >= documentId.length() - 1) {
            return null;
        }
        File root = AndroidStorageVolumes.storageRoot(context, documentId.substring(0, separator));
        if (root == null) {
            return null;
        }
        return new File(root, documentId.substring(separator + 1));
    }
}
