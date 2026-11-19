package vibro.navigator.android.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidDocumentReadFile;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterSegmentDependencies;
import vibro.navigator.brouter.BRouterSegmentReadFile;
import vibro.navigator.brouter.BRouterSegmentsRepository;

public final class AndroidBRouterSegmentsRepositoryFactory {
    private AndroidBRouterSegmentsRepositoryFactory() {
    }

    @NonNull
    public static BRouterSegmentsRepository create() {
        return new BRouterSegmentsRepository(new BRouterSegmentDependencies(
                new AndroidDocuments(),
                new AndroidSegmentFiles(),
                AndroidStorageVolumes::secondaryStorageRootIds,
                (context, uri) -> AndroidBRouterFolderAccess.hasPersistedReadPermission(
                        context, uri, AndroidBRouterFolderAccess.Kind.SEGMENTS)
        ));
    }

    private static final class AndroidDocuments implements BRouterSegmentDependencies.DocumentAccess {
        @NonNull
        @Override
        public Uri buildExternalStorageTreeUri(@NonNull String documentId) {
            return AndroidDocumentAccess.buildExternalStorageTreeUri(documentId);
        }

        @Override
        public boolean externalStorageDocumentExists(@NonNull Context context, @NonNull String documentId) {
            return AndroidDocumentAccess.externalStorageDocumentExists(context, documentId);
        }

        @Nullable
        @Override
        public Uri childDocumentUri(@NonNull Context context, @NonNull Uri treeUri, @NonNull String displayName) {
            Uri directory = AndroidBRouterFolderAccess.dataDirectory(context, treeUri,
                    AndroidBRouterFolderAccess.Kind.SEGMENTS);
            Uri file = directory == null ? null : AndroidDocumentAccess.childDocumentUri(context, directory, displayName);
            if (file == null) {
                AndroidBRouterFolderAccess.recheckSharedFolder(context, treeUri);
            }
            return file;
        }

        @Override
        public String documentIdentity(Uri uri) {
            String id = AndroidDocumentAccess.documentId(uri);
            return id == null ? uri.toString() : uri.getAuthority() + ":" + id;
        }

        @Nullable
        @Override
        public BRouterSegmentReadFile openReadFile(@NonNull Context context, @NonNull Uri documentUri)
                throws IOException {
            return AndroidDocumentReadFile.open(context, documentUri);
        }
    }

    private static final class AndroidSegmentFiles implements BRouterSegmentDependencies.FileAccess {
        @Override
        public boolean canReadFiles(@NonNull Context context) {
            return AndroidBRouterSegmentFileAccess.canReadFiles(context);
        }

        @Nullable
        @Override
        public BRouterSegmentReadFile openReadFile(
                @NonNull Context context,
                @NonNull String directoryDocumentId,
                @NonNull String fileName
        ) throws IOException {
            return AndroidBRouterSegmentFileAccess.openReadFile(context, directoryDocumentId, fileName);
        }
    }
}
