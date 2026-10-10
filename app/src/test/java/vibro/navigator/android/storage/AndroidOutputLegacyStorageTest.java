package vibro.navigator.android.storage;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import vibro.navigator.android.export.AndroidRouteGpxAutoSaver;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;
import vibro.navigator.settings.AppGpxSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28}, shadows = TestOutputMediaDirs.class)
public class AndroidOutputLegacyStorageTest {
    private static final String GPX = "<gpx>legacy route</gpx>";
    private static final String WRITE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final String DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents";
    private final Application app = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        AppOutputStorageSettings.disable(app);
        AndroidOutputStorage.resetFailures();
        AppLogger.init(app);
        AppLogger.setLoggingEnabled(app, false);
    }

    @Test
    public void grantedPermissionUsesPhoneDownloadsAndReadOnlySharing() throws IOException {
        shadowOf(app).grantPermissions(WRITE_PERMISSION);
        Uri uri = AndroidRouteGpxAutoSaver.saveUri(app, GPX);
        assertTrue(AndroidOutputStorage.current(app, Kind.GPX).label.contains("Download/ViBRo/gpx"));
        try (InputStream in = app.getContentResolver().openInputStream(uri)) {
            assertEquals(GPX, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertThrows(IOException.class, () -> app.getContentResolver().openOutputStream(uri));
    }

    @Test
    public void deniedPermissionDisablesSavingWithoutFallback() {
        shadowOf(app).denyPermissions(WRITE_PERMISSION);
        AppGpxSettings.setAutoSaveOnStopEnabled(app, true);
        assertThrows(IOException.class, () -> AndroidRouteGpxAutoSaver.saveUri(app, GPX));
        assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(app));
        assertFalse(AppLogger.setLoggingEnabled(app, true));
        assertFalse(AppLogger.isLoggingEnabled());
        assertFalse(AndroidOutputStorage.isUsable(app, Kind.GPX));
        assertTrue(AndroidOutputStorage.diagnosticLabel(app, Kind.GPX).contains("Download/ViBRo/gpx"));
    }

    @Test
    public void sdGrantCreatesFixedFoldersAndEjectionNeverRedirectsToPhone() throws IOException {
        shadowOf(app).grantPermissions(WRITE_PERMISSION);
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(card.context,
                DOCUMENTS_AUTHORITY, TestOutputStorageCard.ID + ":");
        grant(card.context, provider.tree(TestOutputStorageCard.ID + ":"));
        assertTrue(AppOutputStorageSettings.useSdCard(app));
        assertFalse(AppLogger.isLoggingEnabled());
        assertEquals(1, provider.names().length);
        Uri uri = AndroidRouteGpxAutoSaver.saveUri(card.context, GPX);
        assertTrue(uri.toString().contains("Download%2FViBRo%2Fgpx"));
        try (InputStream in = app.getContentResolver().openInputStream(uri)) {
            assertEquals(GPX, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        card.eject();
        assertThrows(IOException.class, () -> AndroidRouteGpxAutoSaver.saveUri(card.context, GPX));
        assertTrue(AndroidOutputStorage.current(card.context, Kind.GPX).label.contains(TestOutputStorageCard.ID));
        assertFalse(AndroidOutputStorage.isUsable(card.context, Kind.GPX));
        card.insert();
        assertEquals(uri.getAuthority(), AndroidRouteGpxAutoSaver.saveUri(card.context, GPX).getAuthority());
    }

    @Test
    public void grantForDifferentStorageOrCustomFolderCannotEnableSd() throws IOException {
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        Uri wrong = AndroidDocumentAccess.buildExternalStorageTreeUri("primary:Download");
        assertThrows(IOException.class, () -> grant(card.context, wrong));
        Uri custom = AndroidDocumentAccess.buildExternalStorageTreeUri(TestOutputStorageCard.ID + ":Trips");
        assertThrows(IOException.class, () -> grant(card.context, custom));
        assertFalse(AppOutputStorageSettings.useSdCard(app));
    }

    @Test
    public void savedGrantIsReusedOnlyWhileReadAndWriteAccessStillExist() throws IOException {
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(card.context,
                DOCUMENTS_AUTHORITY, TestOutputStorageCard.ID + ":");
        Uri tree = provider.tree(TestOutputStorageCard.ID + ":");
        grant(card.context, tree);
        AppOutputStorageSettings.disable(app);
        AndroidOutputStorageVolumes.Card volume = AndroidOutputStorageVolumes.find(card.context, "");
        assertFalse(AndroidOutputStorageAccess.needsCardGrant(card.context, volume));
        assertEquals(tree.toString(), AndroidOutputStorageAccess.existingGrant(card.context, volume));
        app.getContentResolver().releasePersistableUriPermission(tree, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertTrue(AndroidOutputStorageAccess.needsCardGrant(card.context, volume));
        assertNull(AndroidOutputStorageAccess.existingGrant(card.context, volume));
    }

    @Test
    public void savedGrantForAnotherCardCannotBeReused() throws IOException {
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(card.context,
                DOCUMENTS_AUTHORITY, "OTHER-1234:Download");
        AppOutputStorageSettings.select(app, TestOutputStorageCard.ID, provider.tree("OTHER-1234:Download").toString());
        assertTrue(AndroidOutputStorageAccess.needsCardGrant(card.context,
                AndroidOutputStorageVolumes.find(card.context, "")));
    }

    @Test
    public void existingGrantCanBeReusedBeforeSelectionWasSaved() throws IOException {
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(card.context,
                DOCUMENTS_AUTHORITY, TestOutputStorageCard.ID + ":Download");
        AndroidOutputStorageVolumes.Card volume = AndroidOutputStorageVolumes.find(card.context, "");
        assertFalse(AndroidOutputStorageAccess.needsCardGrant(card.context, volume));
        AndroidOutputStorageAccess.enable(card.context, volume,
                AndroidOutputStorageAccess.existingGrant(card.context, volume));
        assertEquals(provider.tree(TestOutputStorageCard.ID + ":Download").toString(), AppOutputStorageSettings.tree(app));
        assertTrue(AppOutputStorageSettings.useSdCard(app));
    }

    @Test
    public void clearDeletesOnlyMatchingFilesFromConfirmedFolder() throws IOException {
        shadowOf(app).grantPermissions(WRITE_PERMISSION);
        AndroidRouteGpxAutoSaver.saveUri(app, GPX);
        AndroidOutputDestination selected = AndroidOutputStorage.current(app, Kind.GPX);
        File unrelated = new File(selected.directory, "keep.gpx");
        Files.writeString(unrelated.toPath(), "unrelated");
        File subfolder = new File(selected.directory, "keep-folder");
        assertTrue(subfolder.mkdir());
        AndroidOutputFolderCleaner.clear(app, Kind.GPX, selected.token());
        assertTrue(unrelated.isFile());
        assertTrue(subfolder.isDirectory());
        assertEquals(2, selected.directory.list().length);
    }

    private static void grant(Context context, Uri uri) throws IOException {
        AndroidOutputStorageAccess.accept(context, new Intent().setData(uri).addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
    }
}
