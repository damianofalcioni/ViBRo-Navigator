package vibro.navigator.android.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidDocumentReadFile;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
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
                AndroidPersistedUriPermissions::hasReadPermission
        ));
    }

    private static final class AndroidDocuments implements BRouterSegmentDependencies.DocumentAccess {
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

        @Nullable
        @Override
        public Uri childDocumentUri(@NonNull Context context, @NonNull Uri treeUri, @NonNull String displayName) {
            return AndroidDocumentAccess.childDocumentUri(context, treeUri, displayName);
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
