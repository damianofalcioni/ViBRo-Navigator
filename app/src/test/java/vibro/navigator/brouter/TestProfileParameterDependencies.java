package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

final class TestProfileParameterDependencies {
    private TestProfileParameterDependencies() {
    }

    @NonNull
    static BRouterProfileDependencies create(@NonNull Uri profileUri, @NonNull String profileText) {
        return new BRouterProfileDependencies(
                new TestDocumentAccess(profileUri, profileText),
                context -> Collections.emptyList(),
                new TestPackageAccess(),
                (context, uri) -> uri != null && uri.equals(profileUri)
        );
    }

    private static final class TestDocumentAccess implements BRouterProfileDependencies.DocumentAccess {
        @NonNull
        private final Uri profileUri;
        @NonNull
        private final String profileText;

        private TestDocumentAccess(@NonNull Uri profileUri, @NonNull String profileText) {
            this.profileUri = profileUri;
            this.profileText = profileText;
        }

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
            return profileUri;
        }

        @NonNull
        @Override
        public Uri buildExternalStorageTreeUri(@NonNull String documentId) {
            return Uri.parse("content://profiles/tree/" + documentId);
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

        @Nullable
        @Override
        public Uri childDocumentUri(@NonNull Context context, @NonNull Uri treeUri, @NonNull String displayName) {
            return null;
        }

        @Nullable
        @Override
        public String readText(@NonNull Context context, @NonNull Uri documentUri) {
            return profileUri.equals(documentUri) ? profileText : null;
        }
    }

    private static final class TestPackageAccess implements BRouterProfileDependencies.PackageAccess {
        @Override
        public boolean isInstalled(@NonNull Context context, @NonNull String packageName) {
            return true;
        }

        @Nullable
        @Override
        public String sourceDir(@NonNull Context context, @NonNull String packageName) {
            return null;
        }
    }
}
