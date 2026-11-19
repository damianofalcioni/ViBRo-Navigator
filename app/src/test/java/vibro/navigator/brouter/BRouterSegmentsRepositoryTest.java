package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import java.util.concurrent.CountDownLatch;

import vibro.navigator.nav.compass.CompassStreetVisibility;

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
    public void reloadsCachedSourcesAndMissesAfterAnotherInstanceSavesGrant() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Uri tree = Uri.parse("content://segments/tree/granted");
        TestSegmentDependencies testDependencies = TestSegmentDependencies.createWithGrantedTree(tree);
        BRouterSegmentsRepository active = new BRouterSegmentsRepository(testDependencies.dependencies);
        BRouterSegmentsRepository picker = new BRouterSegmentsRepository(testDependencies.dependencies);
        String tile = "E015_N45.rd5";
        assertFalse(active.readGrantedTile(activity, tile, uri -> true));
        active.recordMissingSegmentFile(tile);

        picker.saveSegmentsTreeUri(activity, tree);
        active.readGrantedTile(activity, tile, uri -> true);

        assertEquals(tree, testDependencies.documentAccess.firstChildTreeUri);
    }

    @Test
    public void loadSurroundingStreets_prefersSavedSegmentsTreeWhenReadGrantExists() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Uri treeUri = Uri.parse("content://segments/tree/granted");
        TestSegmentDependencies testDependencies = TestSegmentDependencies.createWithGrantedTree(treeUri);
        BRouterSegmentsRepository repository = new BRouterSegmentsRepository(testDependencies.dependencies);
        repository.saveSegmentsTreeUri(activity, treeUri);

        repository.loadSurroundingStreets(activity, 48.2d, 16.3d, 80d, 10, CompassStreetVisibility.all());

        assertEquals(treeUri, testDependencies.documentAccess.firstChildTreeUri);
    }

    @Test
    public void unreadableTileInNewGrantFallsBackToTheLegacyTile() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri legacy = Uri.parse("content://test.legacy/tree/segments4");
            Uri parent = Uri.parse("content://test.shared/tree/brouter");
            var repository = new BRouterSegmentsRepository(new BRouterSegmentDependencies(
                    new TileCopies(parent), new TestFileAccess(), context -> Collections.emptyList(),
                    (context, uri) -> legacy.equals(uri) || parent.equals(uri)));
            repository.saveSegmentsTreeUri(activity, legacy);
            int original = loadFixtureStreets(repository, activity);
            assertTrue(original > 0);

            BRouterFolderGrant.save(activity, parent, null, parent.buildUpon().appendPath("segments4").build());

            assertEquals(original, loadFixtureStreets(repository, activity));
        }
    }

    @Test
    public void stalledPreferredTileReachesTheHealthyFallback() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri legacy = Uri.parse("content://test.legacy/tree/segments4");
            Uri parent = Uri.parse("content://test.shared/tree/brouter");
            var documents = new StalledTileCopies(parent);
            var repository = new BRouterSegmentsRepository(new BRouterSegmentDependencies(
                    documents, new TestFileAccess(), context -> Collections.emptyList(),
                    (context, uri) -> legacy.equals(uri) || parent.equals(uri)));
            repository.saveSegmentsTreeUri(activity, legacy);
            BRouterFolderGrant.save(activity, parent, null, childFolder(parent));
            try {
                assertTrue(loadFixtureStreets(repository, activity) > 0);
                assertEquals(0L, documents.started.getCount());
            } finally {
                documents.release.countDown();
            }
        }
    }

    @Test
    public void remountedCardIsDetectedWithoutSavingTheGrantAgain() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri card = Uri.parse("content://test.card/tree/segments4");
            var mounted = new java.util.concurrent.atomic.AtomicBoolean();
            var repository = new BRouterSegmentsRepository(new BRouterSegmentDependencies(
                    new TileCopies(Uri.EMPTY), new TestFileAccess(), context -> Collections.emptyList(),
                    (context, uri) -> card.equals(uri) && mounted.get()));
            repository.saveSegmentsTreeUri(activity, card);
            assertFalse(repository.readGrantedTile(activity, "E015_N45.rd5", tile -> true));

            mounted.set(true);

            assertTrue(repository.readGrantedTile(activity, "E015_N50.rd5", tile -> true));
        }
    }

    @Test
    public void preferredSourceIsRetriedAfterATransientFailure() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri earlier = Uri.parse("content://test.earlier/tree/brouter");
            Uri preferred = Uri.parse("content://test.preferred/tree/brouter");
            var repository = new BRouterSegmentsRepository(new BRouterSegmentDependencies(
                    new TileCopies(preferred), new TestFileAccess(), context -> Collections.emptyList(),
                    (context, uri) -> earlier.equals(uri) || preferred.equals(uri)));
            BRouterFolderGrant.save(activity, earlier, null, childFolder(earlier));
            BRouterFolderGrant.save(activity, preferred, null, childFolder(preferred));
            String tile = "E015_N45.rd5";
            assertTrue(repository.readGrantedTile(activity, tile,
                    uri -> earlier.getAuthority().equals(uri.getAuthority())));

            var source = new java.util.concurrent.atomic.AtomicReference<String>();
            assertTrue(repository.readGrantedTile(activity, tile, uri -> {
                source.set(uri.getAuthority());
                return true;
            }));
            assertEquals(preferred.getAuthority(), source.get());
        }
    }

    private static Uri childFolder(Uri tree) {
        return tree.buildUpon().appendPath("segments4").build();
    }

    private static int loadFixtureStreets(BRouterSegmentsRepository repository, Context context) {
        return repository.loadSurroundingStreets(context, 48.18773346166013d, 16.38147556524725d,
                360d, 100, CompassStreetVisibility.all()).segments.size();
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

    private static class TestDocumentAccess implements BRouterSegmentDependencies.DocumentAccess {
        @Nullable
        private Uri firstChildTreeUri;

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

    private static class TileCopies extends TestDocumentAccess {
        private final Uri inaccessibleParent;

        TileCopies(Uri inaccessibleParent) {
            this.inaccessibleParent = inaccessibleParent;
        }

        @Override
        public Uri childDocumentUri(Context context, Uri tree, String name) {
            return tree.buildUpon().appendPath("document").appendPath(name).build();
        }

        @Override
        public BRouterSegmentReadFile openReadFile(Context context, Uri file) throws IOException {
            if (inaccessibleParent.getAuthority().equals(file.getAuthority())) {
                throw new IOException("The second source lists the tile but cannot open it");
            }
            return new BRouterStreetTestFile();
        }
    }

    private static final class StalledTileCopies extends TileCopies {
        private final String stalledAuthority;
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        StalledTileCopies(Uri parent) {
            super(Uri.EMPTY);
            stalledAuthority = parent.getAuthority();
        }

        @Override
        public BRouterSegmentReadFile openReadFile(Context context, Uri file) throws IOException {
            if (stalledAuthority.equals(file.getAuthority())) {
                started.countDown();
                while (release.getCount() > 0) {
                    try {
                        release.await();
                    } catch (InterruptedException ignored) {
                        // A remote provider can keep its call outstanding after cancellation.
                    }
                }
            }
            return new BRouterStreetTestFile();
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
