package vibro.navigator.android.storage;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import androidx.core.content.IntentCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import vibro.navigator.android.export.AndroidRouteGpxFileProvider;
import vibro.navigator.settings.AppDataBackup;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28}, shadows = TestOutputMediaDirs.class)
public class AndroidDatabaseBackupLegacyStorageTest {
    private static final String NAME = "vibro-navigator-backup-20261012123000.json";
    private static final String GRANT_ID = TestOutputStorageCard.ID + ":Download";
    private final Application app = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        AppOutputStorageSettings.disable(app);
        AndroidOutputStorage.resetFailures();
        shadowOf(app).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Test
    public void phoneBackupUsesPublicFolderAndKeepsCollidingFiles() throws IOException {
        assertEquals(NAME, AndroidDatabaseBackupStorage.save(app, "first", NAME));
        File directory = AndroidOutputStorage.requested(app, Kind.BACKUP).directory;
        assertTrue(directory.getAbsolutePath().replace('\\', '/').endsWith("Download/ViBRo/backup"));
        String second = AndroidDatabaseBackupStorage.save(app, "second", NAME);
        assertEquals("vibro-navigator-backup-20261012123000-2.json", second);
        assertEquals("first", Files.readString(new File(directory, NAME).toPath()));
        assertEquals("second", Files.readString(new File(directory, second).toPath()));
        Uri uri = AndroidRouteGpxFileProvider.uriForFile(app, new File(directory, second));
        assertEquals(AppDataBackup.MIME_TYPE, app.getContentResolver().getType(uri));
        assertThrows(IOException.class, () -> app.getContentResolver().openOutputStream(uri));
        assertInitialFolder(AndroidDatabaseBackupStorage.importIntent(app), "primary:Download/ViBRo/backup");
    }

    @Test
    public void missingPhonePermissionFailsWithoutChangingStorage() {
        shadowOf(app).denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        assertThrows(IOException.class, () -> AndroidDatabaseBackupStorage.save(app, "{}", NAME));
        assertFalse(AppOutputStorageSettings.useSdCard(app));
        assertFalse(AndroidOutputStorage.current(app, Kind.BACKUP).directory.exists());
    }

    @Test
    public void sdBackupReusesAncestorGrantAndImportStartsInBackupSubfolder() throws IOException {
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(card.context,
                "com.android.externalstorage.documents", GRANT_ID);
        AndroidOutputStorageAccess.enable(card.context, AndroidOutputStorageVolumes.find(card.context, ""),
                provider.tree(GRANT_ID).toString());
        assertEquals(NAME, AndroidDatabaseBackupStorage.save(card.context, "{}", NAME));
        assertEquals("{}", provider.read(TestOutputStorageCard.ID + ":Download/ViBRo/backup/" + NAME));
        Intent intent = AndroidDatabaseBackupStorage.importIntent(card.context);
        assertInitialFolder(intent, TestOutputStorageCard.ID + ":Download/ViBRo/backup");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri folder = IntentCompat.getParcelableExtra(intent, DocumentsContract.EXTRA_INITIAL_URI, Uri.class);
            assertEquals(GRANT_ID, DocumentsContract.getTreeDocumentId(folder));
        }
        app.getContentResolver().releasePersistableUriPermission(provider.tree(GRANT_ID),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertThrows(IOException.class, () -> AndroidDatabaseBackupStorage.save(card.context, "{}", NAME));
    }

    private static void assertInitialFolder(Intent intent, String expected) {
        Uri folder = IntentCompat.getParcelableExtra(intent, DocumentsContract.EXTRA_INITIAL_URI, Uri.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(expected, DocumentsContract.getDocumentId(folder));
        } else {
            assertNull(folder);
        }
    }
}
