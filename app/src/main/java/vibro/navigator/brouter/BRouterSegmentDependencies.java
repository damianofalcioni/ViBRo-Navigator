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
    final StorageVolumeAccess storageVolumeAccess;

    public BRouterSegmentDependencies(
            @NonNull DocumentAccess documentAccess,
            @NonNull StorageVolumeAccess storageVolumeAccess
    ) {
        this.documentAccess = documentAccess;
        this.storageVolumeAccess = storageVolumeAccess;
    }

    public interface DocumentAccess {
        @NonNull
        Uri buildExternalStorageTreeUri(@NonNull String documentId);

        boolean externalStorageDocumentExists(@NonNull Context context, @NonNull String documentId);

        @Nullable
        Uri childDocumentUri(@NonNull Context context, @NonNull Uri treeUri, @NonNull String displayName);

        @Nullable
        BRouterSegmentReadFile openReadFile(@NonNull Context context, @NonNull Uri documentUri) throws IOException;
    }

    public interface StorageVolumeAccess {
        @NonNull
        List<String> secondaryStorageRootIds(@NonNull Context context);
    }
}
