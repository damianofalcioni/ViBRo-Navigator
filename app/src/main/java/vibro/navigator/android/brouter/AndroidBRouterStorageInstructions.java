package vibro.navigator.android.brouter;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.DocumentsContractCompat;

import vibro.navigator.android.storage.AndroidDocumentAccess;

final class AndroidBRouterStorageInstructions {
    private AndroidBRouterStorageInstructions() {
    }

    @NonNull
    static String folderPath(@Nullable Uri initialUri, @NonNull String fallbackFolder) {
        if (initialUri == null || !AndroidDocumentAccess.isExternalStorageDocument(initialUri)) {
            return fallbackFolder;
        }
        String documentId = DocumentsContractCompat.isTreeUri(initialUri)
                ? AndroidDocumentAccess.treeDocumentId(initialUri)
                : AndroidDocumentAccess.documentId(initialUri);
        return relativePath(documentId, fallbackFolder);
    }

    @NonNull
    static String relativePath(@Nullable String documentId, @NonNull String fallbackFolder) {
        if (documentId == null) {
            return fallbackFolder;
        }
        int separator = documentId.indexOf(':');
        return separator >= 0 && separator < documentId.length() - 1
                ? documentId.substring(separator + 1) : fallbackFolder;
    }
}
