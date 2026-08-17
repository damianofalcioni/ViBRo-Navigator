package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class BRouterSegmentsRepositoryTest {
    @Test
    public void saveSegmentsTreeUri_roundTripsSavedValue() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterSegmentsRepository repository = new BRouterSegmentsRepository(TestSegmentDependencies.create());
        Uri treeUri = Uri.parse("content://segments/tree/granted");

        repository.saveSegmentsTreeUri(activity, treeUri);

        assertEquals(treeUri, repository.getSegmentsTreeUri(activity));
    }

    @Test
    public void loadSurroundingStreets_prefersSavedSegmentsTreeWhenReadGrantExists() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Uri treeUri = Uri.parse("content://segments/tree/granted");
        TestSegmentDependencies testDependencies = TestSegmentDependencies.createWithGrantedTree(treeUri);
        BRouterSegmentsRepository repository = new BRouterSegmentsRepository(testDependencies.dependencies);
        repository.saveSegmentsTreeUri(activity, treeUri);

        repository.loadSurroundingStreets(activity, 48.2d, 16.3d, 80d, 10);

        assertEquals(treeUri, testDependencies.documentAccess.firstChildTreeUri);
    }

    private static final class TestSegmentDependencies {
        @NonNull
        final BRouterSegmentDependencies dependencies;
        @NonNull
        final TestDocumentAccess documentAccess;

        private TestSegmentDependencies(
                @NonNull BRouterSegmentDependencies dependencies,
                @NonNull TestDocumentAccess documentAccess
        ) {
            this.dependencies = dependencies;
            this.documentAccess = documentAccess;
        }

        @NonNull
        static BRouterSegmentDependencies create() {
            return createWithGrantedTree(null).dependencies;
        }

        @NonNull
        static TestSegmentDependencies createWithGrantedTree(@Nullable Uri grantedTreeUri) {
            TestDocumentAccess documentAccess = new TestDocumentAccess();
            return new TestSegmentDependencies(
                    new BRouterSegmentDependencies(
                            documentAccess,
                            new TestFileAccess(),
                            context -> Collections.emptyList(),
                            (context, uri) -> uri != null && uri.equals(grantedTreeUri)
                    ),
                    documentAccess
            );
        }
    }

    private static final class TestDocumentAccess implements BRouterSegmentDependencies.DocumentAccess {
        @Nullable
        private Uri firstChildTreeUri;

        @NonNull
        @Override
        public Uri buildExternalStorageDocumentUri(@NonNull String documentId) {
            return Uri.parse("content://segments/document/" + documentId);
        }

        @NonNull
        @Override
        public Uri buildExternalStorageTreeUri(@NonNull String documentId) {
            return Uri.parse("content://segments/tree/" + documentId);
        }

        @Override
        public boolean externalStorageDocumentExists(@NonNull Context context, @NonNull String documentId) {
            return false;
        }

        @Nullable
        @Override
        public Uri childDocumentUri(@NonNull Context context, @NonNull Uri treeUri, @NonNull String displayName) {
            if (firstChildTreeUri == null) {
                firstChildTreeUri = treeUri;
            }
            return null;
        }

        @Nullable
        @Override
        public BRouterSegmentReadFile openReadFile(@NonNull Context context, @NonNull Uri documentUri)
                throws IOException {
            return null;
        }
    }

    private static final class TestFileAccess implements BRouterSegmentDependencies.FileAccess {
        @Override
        public boolean canReadFiles(@NonNull Context context) {
            return false;
        }

        @Nullable
        @Override
        public BRouterSegmentReadFile openReadFile(
                @NonNull Context context,
                @NonNull String directoryDocumentId,
                @NonNull String fileName
        ) throws IOException {
            return null;
        }
    }
}
