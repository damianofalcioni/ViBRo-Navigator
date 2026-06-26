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
                AndroidStorageVolumes::secondaryStorageRootIds
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
            return AndroidDocumentAccess.childDocumentUri(context, treeUri, displayName);
        }

        @Nullable
        @Override
        public BRouterSegmentReadFile openReadFile(@NonNull Context context, @NonNull Uri documentUri)
                throws IOException {
            return AndroidDocumentReadFile.open(context, documentUri);
        }
    }
}
