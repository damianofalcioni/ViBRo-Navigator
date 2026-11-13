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
import android.provider.DocumentsContract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import vibro.navigator.android.storage.AndroidDocumentAccess;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.R)
public class AndroidBRouterParentGrantTest {
    private static final String PARENT_ID = "primary:Android/media/btools.routingapp/brouter";
    private static final String PROFILES_ID = PARENT_ID + "/profiles2";
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
            assertTrue(AndroidBRouterFolderAccess.saveSelection(activity, grant(parent)));

            var profiles = AndroidBRouterProfilesRepositoryFactory.create();
            var segments = AndroidBRouterSegmentsRepositoryFactory.create();
            assertTrue(profiles.hasPersistedProfilesTreeAccess(activity));
            assertTrue(segments.hasPersistedSegmentsTreeAccess(activity));
            assertEquals(1, activity.getContentResolver().getPersistedUriPermissions().size());
            assertTrue(profiles.listProfiles(activity).contains(PROFILE_NAME));
            assertEquals(DocumentsContract.buildDocumentUriUsingTree(parent, PROFILES_ID),
                    profiles.getCustomProfilePickerInitialUri(activity));

            Uri directory = AndroidBRouterFolderAccess.dataDirectory(activity, parent, "segments4");
            assertEquals(DocumentsContract.buildDocumentUriUsingTree(parent, SEGMENTS_ID + "/" + TILE_NAME),
                    AndroidDocumentAccess.childDocumentUri(activity, directory, TILE_NAME));
        }
    }

    @Test
    @Config(sdk = {Build.VERSION_CODES.Q, Build.VERSION_CODES.R})
    public void legacyDataParentCanOnlyBeSelectedOnAndroid10() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri legacy = AndroidDocumentAccess.buildExternalStorageTreeUri(LEGACY_PARENT);
            assertEquals(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q,
                    AndroidBRouterFolderAccess.saveSelection(activity, grant(legacy)));
        }
    }

    @Test
    public void revokedParentFallsBackToAnExistingProfileGrant() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            var profiles = AndroidBRouterProfilesRepositoryFactory.create();
            Uri oldTree = AndroidDocumentAccess.buildExternalStorageTreeUri(PROFILES_ID);
            activity.getContentResolver().takePersistableUriPermission(oldTree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            profiles.saveProfilesTreeUri(activity, oldTree);
            Uri parent = AndroidDocumentAccess.buildExternalStorageTreeUri(PARENT_ID);
            assertTrue(AndroidBRouterFolderAccess.saveSelection(activity, grant(parent)));

            activity.getContentResolver().releasePersistableUriPermission(parent, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            assertTrue(profiles.hasPersistedProfilesTreeAccess(activity));
            assertFalse(AndroidBRouterSegmentsRepositoryFactory.create().hasPersistedSegmentsTreeAccess(activity));
        }
    }

    private static Intent grant(Uri uri) {
        return new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static final class ParentProvider extends ContentProvider {
        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
            MatrixCursor rows = new MatrixCursor(new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME});
            if (uri.getPathSegments().size() != 5 || !PARENT_ID.equals(DocumentsContract.getTreeDocumentId(uri))) {
                return rows;
            }
            String id = DocumentsContract.getDocumentId(uri);
            if (PROFILES_ID.equals(id)) {
                addFile(rows, id, PROFILE_NAME + ".brf");
            } else if (SEGMENTS_ID.equals(id)) {
                addFile(rows, id, TILE_NAME);
            }
            return rows;
        }

        private static void addFile(MatrixCursor rows, String directory, String fileName) {
            rows.addRow(new Object[]{directory + "/" + fileName, fileName});
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
