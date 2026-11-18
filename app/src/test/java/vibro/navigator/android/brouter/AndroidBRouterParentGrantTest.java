package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.OperationCanceledException;
import android.provider.DocumentsContract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;
import java.nio.file.Files;

import vibro.navigator.android.storage.AndroidDocumentAccess;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.R)
public class AndroidBRouterParentGrantTest {
    private static final String PARENT_ID = "primary:Android/media/btools.routingapp/brouter";
    private static final String PROFILES = "profiles2";
    private static final String SEGMENTS = "segments4";
    private static final String PROFILES_ID = PARENT_ID + "/" + PROFILES;
    private static final String SEGMENTS_ID = PARENT_ID + "/segments4";
    private static final String PROFILE_NAME = "shared-parent-test";
    private static final String TILE_NAME = "E15_N45.rd5";
    private static final String LEGACY_PARENT = "primary:Android/data/btools.routingapp/files/brouter";

    @Test
    public void onePersistedGrantListsProfilesAndResolvesTilesThroughTheParent() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri parent = AndroidDocumentAccess.buildExternalStorageTreeUri(PARENT_ID);
            ShadowContentResolver.registerProviderInternal(parent.getAuthority(), new ParentProvider());
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(parent)));

            var profiles = AndroidBRouterProfilesRepositoryFactory.create();
            var segments = AndroidBRouterSegmentsRepositoryFactory.create();
            assertTrue(profiles.hasPersistedProfilesTreeAccess(activity));
            assertTrue(segments.hasPersistedSegmentsTreeAccess(activity));
            assertEquals(1, activity.getContentResolver().getPersistedUriPermissions().size());
            assertTrue(profiles.listProfiles(activity).contains(PROFILE_NAME));
            assertEquals(DocumentsContract.buildDocumentUriUsingTree(parent, PROFILES_ID),
                    profiles.getCustomProfilePickerInitialUri(activity));

            Uri directory = AndroidBRouterFolderAccess.dataDirectory(activity, parent,
                    AndroidBRouterFolderAccess.Kind.SEGMENTS);
            assertEquals(DocumentsContract.buildDocumentUriUsingTree(parent, SEGMENTS_ID + "/" + TILE_NAME),
                    AndroidDocumentAccess.childDocumentUri(activity, directory, TILE_NAME));
        }
    }

    @Test
    @Config(sdk = {Build.VERSION_CODES.Q, Build.VERSION_CODES.R})
    public void legacyDataParentCanOnlyBeSelectedOnAndroid10() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            TestBRouterStorage.createPrimaryDirectory(LEGACY_PARENT.substring("primary:".length()) + "/profiles2");
            Uri legacy = AndroidDocumentAccess.buildExternalStorageTreeUri(LEGACY_PARENT);
            assertEquals(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q,
                    TestBRouterStorage.saveSelection(activity, grant(legacy)));
        }
    }

    @Test
    public void revokedParentFallsBackToAnExistingProfileGrant() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            TestBRouterStorage.createPrimaryFolder(PROFILES);
            var profiles = AndroidBRouterProfilesRepositoryFactory.create();
            Uri oldTree = AndroidDocumentAccess.buildExternalStorageTreeUri(PROFILES_ID);
            activity.getContentResolver().takePersistableUriPermission(oldTree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            profiles.saveProfilesTreeUri(activity, oldTree);
            Uri parent = AndroidDocumentAccess.buildExternalStorageTreeUri(PARENT_ID);
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(parent)));

            activity.getContentResolver().releasePersistableUriPermission(parent, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            assertTrue(profiles.hasPersistedProfilesTreeAccess(activity));
            assertFalse(AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(activity));
        }
    }

    @Test
    public void emptyParentIsRejectedAndPartialParentKeepsLegacyProfiles() throws Exception {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            var profiles = AndroidBRouterProfilesRepositoryFactory.create();
            saveLegacyProfile(activity, "primary:Download");
            Uri parent = AndroidDocumentAccess.buildExternalStorageTreeUri(PARENT_ID);
            TestBRouterStorage.createPrimaryFolder("");
            assertFalse(TestBRouterStorage.saveSelection(activity, grant(parent)));
            assertTrue(profiles.listProfiles(activity).contains(PROFILE_NAME));

            TestBRouterStorage.createPrimaryFolder(SEGMENTS);
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(parent)));
            assertTrue(AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(activity));
            assertTrue(profiles.listProfiles(activity).contains(PROFILE_NAME));

            TestBRouterStorage.createPrimaryFolder(PROFILES);
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(parent)));
            assertTrue(profiles.listProfiles(activity).contains(PROFILE_NAME));
        }
    }

    @Test
    public void sharedGrantSavedBeforeVerificationMetadataRemainsUsable() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            TestBRouterStorage.createPrimaryFolder(PROFILES);
            Uri tree = AndroidDocumentAccess.buildExternalStorageTreeUri(PARENT_ID);
            activity.getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.getSharedPreferences("vibenavigator_brouter", Activity.MODE_PRIVATE)
                    .edit().putString("brouter_tree_uri", tree.toString()).apply();

            assertTrue(AndroidBRouterProfilesRepositoryFactory.create().hasPersistedProfilesTreeAccess(activity));
            assertFalse(AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(activity));
        }
    }

    @Test
    public void documentsRootAcceptsNewParentAndLegacyGrants() throws Exception {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            saveLegacyProfile(activity, "home:custom/profiles2");
            var profiles = AndroidBRouterProfilesRepositoryFactory.create();
            assertTrue(profiles.listProfiles(activity).contains(PROFILE_NAME));

            TestBRouterStorage.createPrimaryDirectory("Documents/brouter/segments4");
            Uri parent = AndroidDocumentAccess.buildExternalStorageTreeUri("home:brouter");
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(parent)));
            assertTrue(AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(activity));
        }
    }

    @Test
    public void existingParentEnablesASubfolderCreatedAfterGranting() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            TestBRouterStorage.createPrimaryDirectory("Documents/brouter/profiles2");
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(tree("home:brouter"))));
            var segments = AndroidBRouterSegmentsRepositoryFactory.create();
            assertFalse(segments.hasPersistedSegmentsTreeAccess(activity));

            TestBRouterStorage.createPrimaryDirectory("Documents/brouter/segments4");

            assertTrue(segments.hasPersistedSegmentsTreeAccess(activity));
            assertEquals(1, activity.getContentResolver().getPersistedUriPermissions().size());
        }
    }

    @Test
    public void partialReplacementPreservesTheOtherGrantedFolder() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            TestBRouterStorage.createPrimaryDirectory("offline/brouter/profiles2");
            TestBRouterStorage.createPrimaryFolder(SEGMENTS);
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(tree("primary:offline/brouter"))));
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(tree(PARENT_ID))));

            var profiles = AndroidBRouterProfilesRepositoryFactory.create();
            assertTrue(profiles.hasPersistedProfilesTreeAccess(activity));
            assertTrue(AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(activity));
            assertEquals("primary:offline/brouter/profiles2",
                    AndroidDocumentAccess.documentId(profiles.getCustomProfilePickerInitialUri(activity)));
        }
    }

    @Test
    public void documentsRelativeAndroidDirectoryRemainsSelectable() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            TestBRouterStorage.createPrimaryDirectory("Documents/Android/data/brouter/profiles2");
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(tree("home:Android/data/brouter"))));
        }
    }

    @Test
    public void providerListingDoesNotMakeAnUnreadableChildUsable() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri tree = AndroidDocumentAccess.buildExternalStorageTreeUri(PARENT_ID);
            ShadowContentResolver.registerProviderInternal(tree.getAuthority(), new ParentProvider() {
                @Override
                public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
                    if (!PARENT_ID.equals(DocumentsContract.getDocumentId(uri))) {
                        throw new SecurityException("Child is visible but its contents are locked");
                    }
                    return super.query(uri, projection, selection, args, sort);
                }
            });

            assertFalse(TestBRouterStorage.saveSelection(activity, grant(tree)));
            assertTrue(activity.getContentResolver().getPersistedUriPermissions().isEmpty());
        }
    }

    @Test
    public void earlierParentIsRecheckedForStreetDataAfterTheNewerGrantIsRevoked() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri earlier = tree("primary:alternate/brouter");
            Uri later = tree(PARENT_ID);
            TestBRouterStorage.createPrimaryDirectory("alternate/brouter/" + PROFILES);
            TestBRouterStorage.createPrimaryFolder(SEGMENTS);
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(earlier)));
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(later)));

            activity.getContentResolver().releasePersistableUriPermission(later, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            TestBRouterStorage.createPrimaryDirectory("alternate/brouter/" + SEGMENTS);

            assertEquals(earlier, AndroidBRouterSegmentsRepositoryFactory.create().getGrantedSegmentsTreeUri(activity));
        }
    }

    @Test
    public void removingTheOnlyVerifiedLocalDirectoryMakesTheParentUnusable() throws Exception {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            File directory = TestBRouterStorage.createPrimaryDirectory(
                    "Android/media/btools.routingapp/brouter/" + SEGMENTS);
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(tree(PARENT_ID))));
            assertTrue(AndroidBRouterFolderAccess.hasUsableParent(activity));

            Files.delete(directory.toPath());

            assertFalse(AndroidBRouterFolderAccess.hasUsableParent(activity));
        }
    }

    @Test
    public void profilesOnlyGrantDoesNotInvalidateTheStreetSource() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            TestBRouterStorage.createPrimaryFolder(SEGMENTS);
            Uri streetSource = tree(PARENT_ID);
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(streetSource)));
            var revisionChanges = new java.util.concurrent.atomic.AtomicInteger();
            Runnable stopObserving = vibro.navigator.brouter.BRouterSegmentSourceSettings.observe(
                    activity, revisionChanges::incrementAndGet);
            try {
                TestBRouterStorage.createPrimaryDirectory("alternate/brouter/" + PROFILES);
                assertTrue(TestBRouterStorage.saveSelection(activity, grant(tree("primary:alternate/brouter"))));

                assertEquals(streetSource, AndroidBRouterSegmentsRepositoryFactory.create().getGrantedSegmentsTreeUri(activity));
                assertEquals(0, revisionChanges.get());
            } finally {
                stopObserving.run();
            }
        }
    }

    private static Uri tree(String id) {
        return AndroidDocumentAccess.buildExternalStorageTreeUri(id);
    }

    @Test
    public void laterAndroidKeepsUsableLegacyDataGrantAndToleratesProviderCancellation() throws Exception {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            saveLegacyProfile(activity, LEGACY_PARENT + "/profiles2");
            Uri uri = AndroidDocumentAccess.buildExternalStorageTreeUri(LEGACY_PARENT);
            ShadowContentResolver.registerProviderInternal(uri.getAuthority(), new ParentProvider() {
                @Override
                public Cursor query(Uri document, String[] projection, String selection, String[] args, String sort) {
                    throw new OperationCanceledException();
                }
            });
            assertTrue(AndroidBRouterProfilesRepositoryFactory.create().listProfiles(activity).contains(PROFILE_NAME));
        }
    }

    @Test
    public void savedOpaqueGrantStatusDoesNotQueryItsProvider() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri tree = DocumentsContract.buildTreeDocumentUri("test.brouter.documents", "root");
            var provider = new ParentProvider();
            ShadowContentResolver.registerProviderInternal(tree.getAuthority(), provider);
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(tree)));
            int queries = provider.queries;
            assertTrue(AndroidBRouterProfilesRepositoryFactory.create().hasPersistedProfilesTreeAccess(activity));
            assertTrue(AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(activity));
            assertEquals(queries, provider.queries);
        }
    }

    private static void saveLegacyProfile(Activity activity, String id) throws java.io.IOException {
        String path = id.startsWith("home:") ? "Documents/" + id.substring(5) : id.substring(id.indexOf(':') + 1);
        File folder = TestBRouterStorage.createPrimaryDirectory(path);
        Files.writeString(new File(folder, PROFILE_NAME + ".brf").toPath(), "# Profile");
        Uri tree = AndroidDocumentAccess.buildExternalStorageTreeUri(id);
        activity.getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        AndroidBRouterProfilesRepositoryFactory.create().saveProfilesTreeUri(activity, tree);
    }

    private static Intent grant(Uri uri) {
        return new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    static class ParentProvider extends ContentProvider {
        int queries;
        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
            queries++;
            MatrixCursor rows = new MatrixCursor(new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE});
            if ("root".equals(DocumentsContract.getTreeDocumentId(uri))) {
                return addOpaqueDirectories(uri, rows);
            }
            if (uri.getPathSegments().size() != 5 || !PARENT_ID.equals(DocumentsContract.getTreeDocumentId(uri))) {
                return rows;
            }
            String id = DocumentsContract.getDocumentId(uri);
            if (PARENT_ID.equals(id)) {
                rows.addRow(new Object[]{PROFILES_ID, PROFILES, DocumentsContract.Document.MIME_TYPE_DIR});
                rows.addRow(new Object[]{SEGMENTS_ID, SEGMENTS, DocumentsContract.Document.MIME_TYPE_DIR});
            } else if (PROFILES_ID.equals(id)) {
                addFile(rows, id, PROFILE_NAME + ".brf");
            } else if (SEGMENTS_ID.equals(id)) {
                addFile(rows, id, TILE_NAME);
            }
            return rows;
        }

        private static Cursor addOpaqueDirectories(Uri uri, MatrixCursor rows) {
            if (uri.getPathSegments().size() == 4) {
                rows.addRow(new Object[]{"root", "brouter", DocumentsContract.Document.MIME_TYPE_DIR});
            } else {
                rows.addRow(new Object[]{"profile-dir", PROFILES, DocumentsContract.Document.MIME_TYPE_DIR});
                rows.addRow(new Object[]{"segment-dir", SEGMENTS, DocumentsContract.Document.MIME_TYPE_DIR});
            }
            return rows;
        }

        private static void addFile(MatrixCursor rows, String directory, String fileName) {
            rows.addRow(new Object[]{directory + "/" + fileName, fileName, "application/octet-stream"});
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(Uri uri, String selection, String[] args) {
            throw new UnsupportedOperationException();
        }
    }
}
