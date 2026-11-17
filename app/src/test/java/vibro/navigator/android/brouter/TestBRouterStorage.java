package vibro.navigator.android.brouter;

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Looper;
import android.provider.DocumentsContract;

import androidx.test.core.app.ApplicationProvider;

import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.android.storage.AndroidStorageVolumes;

public final class TestBRouterStorage {
    private TestBRouterStorage() {
    }

    public static void createPrimaryFolder(String name) {
        createPrimaryDirectory("Android/media/btools.routingapp/brouter/" + name);
    }

    public static File createPrimaryDirectory(String path) {
        File root = AndroidStorageVolumes.storageRoot(ApplicationProvider.getApplicationContext(), "primary");
        File folder = new File(root, path);
        assertTrue(folder.isDirectory() || folder.mkdirs());
        return folder;
    }

    static boolean saveSelection(Activity activity, Intent result) {
        AtomicReference<Boolean> saved = new AtomicReference<>();
        AndroidBRouterFolderAccess.saveSelection(activity, result, saved::set);
        shadowOf(Looper.getMainLooper()).idle();
        return saved.get();
    }

    static void exposeDirectoryThroughProvider(Context context, String documentId) {
        Uri tree = AndroidDocumentAccess.buildExternalStorageTreeUri(documentId);
        registerDirectoryProvider(tree, () -> true);
        assertTrue(AndroidDocumentAccess.externalStorageDocumentExists(context, documentId));
    }

    public static void requireGrantToQueryPrimaryFolder(Context context) {
        Uri tree = AndroidDocumentAccess.buildExternalStorageTreeUri(
                "primary:Android/media/btools.routingapp/brouter");
        registerDirectoryProvider(tree, () -> AndroidPersistedUriPermissions.hasReadPermission(context, tree));
    }

    static void exposeNamedFolder(Uri tree, String displayName) {
        Uri document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
        ShadowContentResolver.registerProviderInternal(tree.getAuthority(),
                new DirectoryProvider(document, () -> true, displayName));
    }

    static void registerDirectoryProvider(Uri tree, BooleanSupplier canQuery) {
        Uri document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
        ShadowContentResolver.registerProviderInternal(document.getAuthority(),
                new DirectoryProvider(document, canQuery));
    }

    private static final class DirectoryProvider extends ContentProvider {
        private final Uri directory;
        private final BooleanSupplier canQuery;
        private final String displayName;

        DirectoryProvider(Uri directory, BooleanSupplier canQuery) {
            this(directory, canQuery, null);
        }

        DirectoryProvider(Uri directory, BooleanSupplier canQuery, String displayName) {
            this.directory = directory;
            this.canQuery = canQuery;
            this.displayName = displayName;
        }

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
            if (!canQuery.getAsBoolean()) {
                throw new SecurityException("Directory queries require a URI grant");
            }
            MatrixCursor cursor = new MatrixCursor(new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME});
            if (directory.equals(uri)) {
                cursor.addRow(new Object[]{DocumentsContract.getDocumentId(uri),
                        DocumentsContract.Document.MIME_TYPE_DIR, displayName});
            }
            return cursor;
        }

        @Override
        public String getType(Uri uri) {
            return directory.equals(uri) ? DocumentsContract.Document.MIME_TYPE_DIR : null;
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
