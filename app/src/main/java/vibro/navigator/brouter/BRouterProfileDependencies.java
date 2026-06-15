package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class BRouterProfileDependencies {
    @NonNull
    final DocumentAccess documentAccess;
    @NonNull
    final StorageVolumeAccess storageVolumeAccess;
    @NonNull
    final PackageAccess packageAccess;
    @NonNull
    final UriPermissionAccess uriPermissionAccess;

    public BRouterProfileDependencies(
            @NonNull DocumentAccess documentAccess,
            @NonNull StorageVolumeAccess storageVolumeAccess,
            @NonNull PackageAccess packageAccess,
            @NonNull UriPermissionAccess uriPermissionAccess
    ) {
        this.documentAccess = documentAccess;
        this.storageVolumeAccess = storageVolumeAccess;
        this.packageAccess = packageAccess;
        this.uriPermissionAccess = uriPermissionAccess;
    }

    public interface DocumentAccess {
        @Nullable
        Uri buildTreeDocumentUri(@NonNull Uri treeUri);

        boolean isExternalStorageDocument(@NonNull Uri uri);

        @Nullable
        String documentId(@NonNull Uri documentUri);

        @NonNull
        Uri buildExternalStorageDocumentUri(@NonNull String documentId);

        @NonNull
        Uri buildExternalStorageTreeUri(@NonNull String documentId);

        boolean externalStorageDocumentExists(@NonNull Context context, @NonNull String documentId);

        @NonNull
        List<String> childDisplayNames(@NonNull Context context, @NonNull Uri treeUri);

        @Nullable
        Uri childDocumentUri(@NonNull Context context, @NonNull Uri treeUri, @NonNull String displayName);

        @Nullable
        String readText(@NonNull Context context, @NonNull Uri documentUri);
    }

    public interface StorageVolumeAccess {
        @NonNull
        List<String> secondaryStorageRootIds(@NonNull Context context);
    }

    public interface PackageAccess {
        boolean isInstalled(@NonNull Context context, @NonNull String packageName);

        @Nullable
        String sourceDir(@NonNull Context context, @NonNull String packageName);
    }

    public interface UriPermissionAccess {
        boolean hasReadPermission(@NonNull Context context, @Nullable Uri uri);
    }
}
