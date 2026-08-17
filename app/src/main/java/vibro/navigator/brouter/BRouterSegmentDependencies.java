package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

public final class BRouterSegmentDependencies {
    @NonNull
    final DocumentAccess documentAccess;
    @NonNull
    final FileAccess fileAccess;
    @NonNull
    final StorageVolumeAccess storageVolumeAccess;
    @NonNull
    final UriPermissionAccess uriPermissionAccess;

    public BRouterSegmentDependencies(
            @NonNull DocumentAccess documentAccess,
            @NonNull FileAccess fileAccess,
            @NonNull StorageVolumeAccess storageVolumeAccess,
            @NonNull UriPermissionAccess uriPermissionAccess
    ) {
        this.documentAccess = documentAccess;
        this.fileAccess = fileAccess;
        this.storageVolumeAccess = storageVolumeAccess;
        this.uriPermissionAccess = uriPermissionAccess;
    }

    public interface DocumentAccess {
        @NonNull
        Uri buildExternalStorageDocumentUri(@NonNull String documentId);

        @NonNull
        Uri buildExternalStorageTreeUri(@NonNull String documentId);

        boolean externalStorageDocumentExists(@NonNull Context context, @NonNull String documentId);

        @Nullable
        Uri childDocumentUri(@NonNull Context context, @NonNull Uri treeUri, @NonNull String displayName);

        @Nullable
        BRouterSegmentReadFile openReadFile(@NonNull Context context, @NonNull Uri documentUri) throws IOException;
    }

    public interface FileAccess {
        boolean canReadFiles(@NonNull Context context);

        @Nullable
        BRouterSegmentReadFile openReadFile(
                @NonNull Context context,
                @NonNull String directoryDocumentId,
                @NonNull String fileName
        ) throws IOException;
    }

    public interface StorageVolumeAccess {
        @NonNull
        List<String> secondaryStorageRootIds(@NonNull Context context);
    }

    public interface UriPermissionAccess {
        boolean hasReadPermission(@NonNull Context context, @Nullable Uri uri);
    }
}
