package vibro.navigator.android.brouter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterProfileDependencies;
import vibro.navigator.brouter.BRouterProfilesRepository;

public final class AndroidBRouterProfilesRepositoryFactory {
    private AndroidBRouterProfilesRepositoryFactory() {
    }

    @NonNull
    public static BRouterProfilesRepository create() {
        return new BRouterProfilesRepository(createDependencies());
    }

    @NonNull
    private static BRouterProfileDependencies createDependencies() {
        return new BRouterProfileDependencies(
                new AndroidDocuments(),
                AndroidStorageVolumes::secondaryStorageRootIds,
                new AndroidPackagesAccess(),
                AndroidBRouterProfilesRepositoryFactory::hasPersistedReadPermission
        );
    }

    private static boolean hasPersistedReadPermission(@NonNull Context context, @Nullable Uri uri) {
        return AndroidPersistedUriPermissions.hasReadPermission(context, uri);
    }

    private static final class AndroidDocuments implements BRouterProfileDependencies.DocumentAccess {
        @Nullable
        @Override
        public Uri buildTreeDocumentUri(@NonNull Uri treeUri) {
            return AndroidDocumentAccess.buildTreeDocumentUri(treeUri);
        }

        @Override
        public boolean isExternalStorageDocument(@NonNull Uri uri) {
            return AndroidDocumentAccess.isExternalStorageDocument(uri);
        }

        @Nullable
        @Override
        public String documentId(@NonNull Uri documentUri) {
            return AndroidDocumentAccess.documentId(documentUri);
        }

        @NonNull
        @Override
        public Uri buildExternalStorageDocumentUri(@NonNull String documentId) {
            return AndroidDocumentAccess.buildExternalStorageDocumentUri(documentId);
        }

        @NonNull
        @Override
        public Uri buildExternalStorageTreeUri(@NonNull String documentId) {
            return AndroidDocumentAccess.buildExternalStorageTreeUri(documentId);
        }

        @Override
        public boolean externalStorageDocumentExists(@NonNull Context context, @NonNull String documentId) {
            return AndroidDocumentAccess.externalStorageDocumentExists(context, documentId);
        }

        @NonNull
        @Override
        public List<String> childDisplayNames(@NonNull Context context, @NonNull Uri treeUri) {
            return AndroidDocumentAccess.childDisplayNames(context, treeUri);
        }

        @Nullable
        @Override
        public Uri childDocumentUri(@NonNull Context context, @NonNull Uri treeUri, @NonNull String displayName) {
            return AndroidDocumentAccess.childDocumentUri(context, treeUri, displayName);
        }

        @Nullable
        @Override
        public String readText(@NonNull Context context, @NonNull Uri documentUri) {
            return AndroidDocumentAccess.readText(context, documentUri);
        }
    }

    private static final class AndroidPackagesAccess implements BRouterProfileDependencies.PackageAccess {
        @Override
        public boolean isInstalled(@NonNull Context context, @NonNull String packageName) {
            try {
                context.getPackageManager().getPackageInfo(packageName, 0);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        @Nullable
        @Override
        public String sourceDir(@NonNull Context context, @NonNull String packageName) {
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
                return appInfo.sourceDir;
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
    }
}
