package vibro.navigator.android.brouter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Collections;
import androidx.core.util.Supplier;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidDocumentTreeDirectories;
import vibro.navigator.android.storage.AndroidDocumentTreeFileAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterFolderGrant;
import vibro.navigator.brouter.BRouterProfileDependencies;
import vibro.navigator.brouter.BRouterProfilesRepository;

public final class AndroidBRouterProfilesRepositoryFactory {
    private static final ThreadLocal<ProfileQuery> ACTIVE_QUERY = new ThreadLocal<>();
    private static final AndroidBRouterFolderAccess.Kind PROFILES = AndroidBRouterFolderAccess.Kind.PROFILES;

    private AndroidBRouterProfilesRepositoryFactory() {
    }

    @NonNull
    public static BRouterProfilesRepository create() {
        return new BRouterProfilesRepository(createDependencies());
    }

    static <T> T withCancellation(CancellationSignal signal, Supplier<T> lookup) {
        return withCancellation(signal, lookup, () -> { });
    }

    static <T> T withCancellation(CancellationSignal signal, Supplier<T> lookup, Runnable onIncomplete) {
        ProfileQuery previous = ACTIVE_QUERY.get();
        ACTIVE_QUERY.set(new ProfileQuery(signal, onIncomplete));
        try {
            return lookup.get();
        } finally {
            if (previous == null) {
                ACTIVE_QUERY.remove();
            } else {
                ACTIVE_QUERY.set(previous);
            }
        }
    }

    @Nullable
    private static CancellationSignal cancellation() {
        ProfileQuery query = ACTIVE_QUERY.get();
        return query == null ? null : query.signal();
    }

    private static void reportIncomplete() {
        ProfileQuery query = ACTIVE_QUERY.get();
        if (query != null) {
            query.onIncomplete().run();
        }
    }

    private record ProfileQuery(CancellationSignal signal, Runnable onIncomplete) { }

    @NonNull
    private static BRouterProfileDependencies createDependencies() {
        return new BRouterProfileDependencies(
                new AndroidDocuments(),
                AndroidStorageVolumes::secondaryStorageRootIds,
                new AndroidPackagesAccess(),
                AndroidBRouterProfilesRepositoryFactory::hasProfilesReadPermission
        );
    }

    private static boolean hasProfilesReadPermission(Context context, @Nullable Uri tree) {
        boolean granted = AndroidBRouterFolderAccess.hasPersistedReadPermission(context, tree, PROFILES);
        if (!granted && tree != null && BRouterFolderGrant.directory(context, tree, true) != null
                && AndroidPersistedUriPermissions.hasReadPermission(context, tree)
                && AndroidBRouterFolderAccess.isVolumeAvailable(context, tree, AndroidBRouterFolderAccess.treeId(tree))) {
            reportIncomplete();
        }
        return granted;
    }

    private static final class AndroidDocuments implements BRouterProfileDependencies.DocumentAccess {
        @Nullable
        @Override
        public Uri buildTreeDocumentUri(@NonNull Uri treeUri) {
            return AndroidDocumentAccess.buildTreeDocumentUri(treeUri);
        }

        @Nullable
        @Override
        public Uri profilePickerDirectory(@NonNull Context context, @NonNull Uri treeUri) {
            Uri directory = AndroidBRouterFolderAccess.dataDirectory(context, treeUri, PROFILES);
            if (directory == null || !directory.equals(treeUri)) {
                return directory;
            }
            return buildTreeDocumentUri(treeUri);
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
            CancellationSignal signal = cancellation();
            if (signal == null) {
                return AndroidDocumentAccess.externalStorageDocumentExists(context, documentId);
            }
            signal.throwIfCanceled();
            return AndroidDocumentAccess.externalStorageDirectoryPresence(context, documentId, signal)
                    == AndroidDocumentAccess.DirectoryPresence.PRESENT;
        }

        @NonNull
        @Override
        public List<String> childDisplayNames(@NonNull Context context, @NonNull Uri treeUri) {
            Uri directory = AndroidBRouterFolderAccess.dataDirectory(context, treeUri, PROFILES);
            List<String> names = directory == null ? null
                    : AndroidDocumentTreeFileAccess.tryListChildDisplayNames(context, directory, cancellation());
            if (needsDirectoryRecovery(context, treeUri, directory, names)) {
                reportIncomplete();
                AndroidBRouterFolderAccess.recheckSharedFolder(context, treeUri);
            }
            return names == null ? Collections.emptyList() : names;
        }

        private static boolean needsDirectoryRecovery(Context context, Uri tree, @Nullable Uri directory,
                                                      @Nullable List<String> names) {
            if (names == null) {
                return AndroidPersistedUriPermissions.hasReadPermission(context, tree);
            }
            if (!names.isEmpty() || directory == null || tree.equals(directory)
                    || BRouterFolderGrant.isDirect(context, tree, true)) {
                return false;
            }
            // A provider can return an empty listing for an obsolete child document ID.
            Uri listed = AndroidDocumentTreeDirectories.childDirectories(context, tree, cancellation()).get(PROFILES.folder);
            return !directory.equals(listed);
        }

        @Nullable
        @Override
        public Uri childDocumentUri(@NonNull Context context, @NonNull Uri treeUri, @NonNull String displayName) {
            Uri directory = AndroidBRouterFolderAccess.dataDirectory(context, treeUri, PROFILES);
            return directory == null ? null : AndroidDocumentTreeFileAccess.childDocumentUri(
                    context, directory, displayName, cancellation());
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
