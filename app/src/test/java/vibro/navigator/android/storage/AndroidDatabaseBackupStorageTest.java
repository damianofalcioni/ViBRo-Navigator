package vibro.navigator.android.storage;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.core.content.IntentCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import vibro.navigator.settings.AppDataBackup;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;
import vibro.navigator.settings.AppSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, shadows = TestOutputMediaDirs.class)
public class AndroidDatabaseBackupStorageTest {
    private static final String NAME = "vibro-navigator-backup-20261012123000.json";
    private final Application app = ApplicationProvider.getApplicationContext();
    private TestDownloadsProvider downloads;

    @Before
    public void setUp() {
        AppOutputStorageSettings.disable(app);
        AndroidOutputStorage.resetFailures();
        downloads = TestDownloadsProvider.install(app);
    }

    @Test
    public void phoneBackupIsPublishedInBackupFolderAndRestoresSettings() throws Exception {
        AppSettings.setImperialUnitsEnabled(app, true);
        String name = AndroidDatabaseBackupStorage.exportDatabase(app);
        Uri uri = downloads.lastUri;
        assertEquals("external_primary", uri.getPathSegments().get(0));
        assertEquals(name, downloads.row(uri).getAsString(MediaStore.MediaColumns.DISPLAY_NAME));
        assertTrue(name.matches("vibro-navigator-backup-\\d{14}\\.json"));
        assertEquals("Download/ViBRo/backup/", downloads.row(uri).getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
        assertEquals(AppDataBackup.MIME_TYPE, app.getContentResolver().getType(uri));
        assertEquals(Integer.valueOf(0), downloads.row(uri).getAsInteger(MediaStore.MediaColumns.IS_PENDING));
        AppSettings.setImperialUnitsEnabled(app, false);
        AndroidDatabaseBackupStorage.importDatabase(app, uri);
        assertTrue(AppSettings.isImperialUnitsEnabled(app));
    }

    @Test
    public void sdBackupsUseSelectedVolumeAndEjectionDoesNotRedirect() throws Exception {
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        AndroidOutputStorageAccess.enable(card.context, AndroidOutputStorageVolumes.find(card.context, ""), null);
        assertEquals(NAME, AndroidDatabaseBackupStorage.save(card.context, "{}", NAME));
        Uri uri = downloads.lastUri;
        assertEquals("abcd-1234", uri.getPathSegments().get(0));
        assertEquals("{}", downloads.read(uri));
        assertEquals("Download/ViBRo/backup/", downloads.row(uri).getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
        card.eject();
        assertThrows(IOException.class, () -> AndroidDatabaseBackupStorage.exportDatabase(card.context));
        assertEquals(1, downloads.count());
        assertTrue(AppOutputStorageSettings.useSdCard(app));
    }

    @Test
    public void saveReturnsProviderAssignedName() throws IOException {
        AndroidOutputFile first = AndroidOutputStorage.create(app, Kind.BACKUP, AppDataBackup.MIME_TYPE, NAME);
        first.publish();
        String assignedName = "vibro-navigator-backup-20261012123000 (1).json";
        // A provider can rename a newly created document when its requested name already exists.
        downloads.assignedName = assignedName;
        assertEquals(assignedName, AndroidDatabaseBackupStorage.save(app, "{}", NAME));
    }

    @Test
    public void failedWriteOrPublicationRemovesPendingBackup() {
        downloads.failWrites = true;
        assertThrows(IOException.class, () -> AndroidDatabaseBackupStorage.save(app, "{}", NAME));
        assertEquals(0, downloads.count());
        downloads.failWrites = false;
        downloads.failPublish = true;
        assertThrows(IOException.class, () -> AndroidDatabaseBackupStorage.save(app, "{}", NAME));
        assertEquals(0, downloads.count());
    }

    @Test
    public void importStartsInSelectedBackupFolderEvenIfPreparationFails() {
        assertPicker(AndroidDatabaseBackupStorage.importIntent(app), "primary:Download/ViBRo/backup");
        AppOutputStorageSettings.select(app, TestOutputStorageCard.ID, null);
        downloads.failWrites = true;
        assertPicker(AndroidDatabaseBackupStorage.importIntent(app), TestOutputStorageCard.ID + ":Download/ViBRo/backup");
        assertEquals(0, downloads.count());
        assertTrue(AppOutputStorageSettings.useSdCard(app));
    }

    private static void assertPicker(Intent intent, String documentId) {
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.getAction());
        assertEquals("*/*", intent.getType());
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE));
        Uri folder = IntentCompat.getParcelableExtra(intent, DocumentsContract.EXTRA_INITIAL_URI, Uri.class);
        assertEquals(documentId, DocumentsContract.getDocumentId(folder));
    }
}
