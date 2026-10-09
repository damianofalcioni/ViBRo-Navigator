package vibro.navigator.android.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import vibro.navigator.android.export.AndroidRouteGpxAutoSaver;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AndroidOutputFolderRecoveryTest {
    private static final String AUTHORITY = "com.android.externalstorage.documents";
    private static final String ROOT = "primary:Documents";
    private static final String GPX = "<gpx />";
    private final Context context = ApplicationProvider.getApplicationContext();
    private TestOutputDocumentsProvider provider;

    @Before
    public void setUp() throws IOException {
        provider = TestOutputDocumentsProvider.install(context, AUTHORITY, ROOT);
    }

    @Test
    public void missingDirectoriesAreCreatedOnlyUnderGrantedParent() throws IOException {
        String target = ROOT + "/ViBRo/gpx";
        AppOutputFolderSettings.set(context, Kind.GPX, provider.tree(target).toString());
        Uri accessible = AndroidOutputFolderAccess.ensureFolder(context, Kind.GPX);
        assertEquals(target, DocumentsContract.getDocumentId(accessible));
        Uri saved = AndroidRouteGpxAutoSaver.saveUri(context, GPX);
        assertEquals(AUTHORITY, saved.getAuthority());
        assertTrue(DocumentsContract.getDocumentId(saved).startsWith(target + "/vibro-navigator-route-"));
        assertEquals(GPX, provider.read(DocumentsContract.getDocumentId(saved)));
        assertTrue(AndroidOutputFolderAccess.isUsable(context, Kind.GPX));
    }

    @Test
    public void deletedSelectedRootWithoutParentGrantFallsBack() throws IOException {
        String target = provider.createDocument(ROOT, DocumentsContract.Document.MIME_TYPE_DIR, "ViBRo");
        Uri selected = provider.tree(target);
        context.getContentResolver().takePersistableUriPermission(selected, accessFlags());
        context.getContentResolver().releasePersistableUriPermission(provider.tree(ROOT), accessFlags());
        AppOutputFolderSettings.set(context, Kind.GPX, selected.toString());
        provider.deleteDocument(target);
        assertThrows(IOException.class, () -> AndroidOutputFolderAccess.ensureFolder(context, Kind.GPX));
        Uri saved = AndroidRouteGpxAutoSaver.saveUri(context, GPX);
        assertNotEquals(AUTHORITY, saved.getAuthority());
        assertEquals(0, provider.names().length);
        assertFalse(AndroidOutputFolderAccess.isUsable(context, Kind.GPX));
    }

    @Test
    public void revokedPermissionUsesDefaultAndRegrantRestoresAccess() throws IOException {
        AppOutputFolderSettings.set(context, Kind.GPX, provider.tree(ROOT).toString());
        context.getContentResolver().releasePersistableUriPermission(provider.tree(ROOT), accessFlags());
        assertNotEquals(AUTHORITY, AndroidRouteGpxAutoSaver.saveUri(context, GPX).getAuthority());
        assertFalse(AndroidOutputFolderAccess.isUsable(context, Kind.GPX));
        AndroidOutputFolderAccess.accept(context, Kind.GPX, new Intent().setData(provider.tree(ROOT))
                .setFlags(accessFlags() | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
        assertTrue(AndroidOutputFolderAccess.isUsable(context, Kind.GPX));
        assertEquals(AUTHORITY, AndroidRouteGpxAutoSaver.saveUri(context, GPX).getAuthority());
    }

    @Test
    public void unrelatedFoldersAndTraversalCannotUseAncestorGrant() {
        assertNull(AndroidOutputFolderRecovery.relativePath(ROOT, "primary:Documents2/ViBRo"));
        assertNull(AndroidOutputFolderRecovery.relativePath(ROOT, "card:Documents/ViBRo"));
        assertNull(AndroidOutputFolderRecovery.relativePath(ROOT, ROOT + "/../Other"));
        assertNull(AndroidOutputFolderRecovery.relativePath(ROOT, ROOT + "/"));
        assertEquals("ViBRo/logs", AndroidOutputFolderRecovery.relativePath(ROOT, ROOT + "/ViBRo/logs"));
        assertEquals("Documents/ViBRo", AndroidOutputFolderRecovery.relativePath("primary:", ROOT + "/ViBRo"));
        assertNull(AndroidOutputFolderRecovery.relativePath("primary:", "card:Documents/ViBRo"));
        assertNull(AndroidOutputFolderRecovery.relativePath("primary:", "primary:../Other"));
    }

    private static int accessFlags() {
        return Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
    }
}
