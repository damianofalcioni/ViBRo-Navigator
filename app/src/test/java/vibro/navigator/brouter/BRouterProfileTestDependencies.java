package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public final class BRouterProfileTestDependencies {
    private BRouterProfileTestDependencies() {
    }

    @NonNull
    public static BRouterProfileDependencies create() {
        return new BRouterProfileDependencies(
                new TestDocumentAccess(),
                context -> Collections.emptyList(),
                new TestPackageAccess(),
                (context, uri) -> false
        );
    }

    private static final class TestDocumentAccess implements BRouterProfileDependencies.DocumentAccess {
        @Nullable
        @Override
        public Uri buildTreeDocumentUri(@NonNull Uri treeUri) {
            return treeUri;
        }

        @Override
        public boolean isExternalStorageDocument(@NonNull Uri uri) {
            return true;
        }

        @Nullable
        @Override
        public String documentId(@NonNull Uri documentUri) {
            return documentUri.getLastPathSegment();
        }

        @NonNull
        @Override
        public Uri buildExternalStorageDocumentUri(@NonNull String documentId) {
            return Uri.parse("content://test.documents/document/" + documentId);
        }

        @NonNull
        @Override
        public Uri buildExternalStorageTreeUri(@NonNull String documentId) {
            return Uri.parse("content://test.documents/tree/" + documentId);
        }

        @Override
        public boolean externalStorageDocumentExists(@NonNull Context context, @NonNull String documentId) {
            return false;
        }

        @NonNull
        @Override
        public List<String> childDisplayNames(@NonNull Context context, @NonNull Uri treeUri) {
            return Collections.emptyList();
        }
    }

    private static final class TestPackageAccess implements BRouterProfileDependencies.PackageAccess {
        @Override
        public boolean isInstalled(@NonNull Context context, @NonNull String packageName) {
            return false;
        }

        @Nullable
        @Override
        public String sourceDir(@NonNull Context context, @NonNull String packageName) {
            return null;
        }
    }
}
