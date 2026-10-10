package vibro.navigator.android.storage;

import static org.junit.Assert.*;

import android.app.Application;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import vibro.navigator.android.export.AndroidRouteGpxAutoSaver;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;
import vibro.navigator.settings.AppGpxSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, shadows = TestOutputMediaDirs.class)
public class AndroidOutputModernStorageTest {
    private static final String GPX = "<gpx>SD route</gpx>";
    private final Application app = ApplicationProvider.getApplicationContext();
    private TestDownloadsProvider downloads;

    @Before
    public void setUp() {
        AppOutputStorageSettings.disable(app);
        AndroidOutputStorage.resetFailures();
        downloads = TestDownloadsProvider.install(app);
        AppLogger.init(app);
        AppLogger.setLoggingEnabled(app, false);
    }

    @Test
    public void modernSdIsVerifiedWithoutGrantOrEnablingLogsAndUsesItsOwnVolume() throws IOException {
        TestOutputStorageCard card = enableSd();
        assertTrue(AppOutputStorageSettings.useSdCard(app));
        assertFalse(AppLogger.isLoggingEnabled());
        assertEquals(0, downloads.count());
        Uri uri = AndroidRouteGpxAutoSaver.saveUri(card.context, GPX);
        assertEquals("abcd-1234", uri.getPathSegments().get(0));
        assertEquals(GPX, downloads.read(uri));
        assertEquals(Integer.valueOf(0), downloads.row(uri).getAsInteger(MediaStore.MediaColumns.IS_PENDING));
        card.eject();
        assertThrows(IOException.class, () -> AndroidRouteGpxAutoSaver.saveUri(card.context, GPX));
        assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(card.context));
        assertFalse(AndroidOutputStorage.isUsable(card.context, Kind.GPX));
        card.insert();
        assertEquals("abcd-1234", AndroidRouteGpxAutoSaver.saveUri(card.context, GPX).getPathSegments().get(0));
    }

    @Test
    public void confirmedDeletionStaysOnSdAndPreservesOtherFilesAndOwners() throws IOException {
        TestOutputStorageCard card = enableSd();
        Uri selected = AndroidRouteGpxAutoSaver.saveUri(card.context, GPX);
        AndroidOutputDestination destination = AndroidOutputStorage.current(card.context, Kind.GPX);
        Uri collision = AndroidRouteGpxAutoSaver.saveUri(card.context, GPX);
        String name = downloads.row(collision).getAsString(MediaStore.MediaColumns.DISPLAY_NAME);
        downloads.row(collision).put(MediaStore.MediaColumns.DISPLAY_NAME,
                name.substring(0, name.length() - 4) + " (1).gpx");
        Uri foreign = AndroidRouteGpxAutoSaver.saveUri(card.context, GPX);
        downloads.row(foreign).put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, "other.application");
        AndroidOutputFile unrelated = AndroidOutputFile.createWritable(card.context, destination,
                "application/gpx+xml", "keep.gpx");
        unrelated.publish();
        AppOutputStorageSettings.disable(app);
        Uri phone = AndroidRouteGpxAutoSaver.saveUri(card.context, GPX);
        AndroidOutputFolderCleaner.clear(card.context, Kind.GPX, destination.token());
        assertNull(downloads.row(selected));
        assertNull(downloads.row(collision));
        assertNotNull(downloads.row(foreign));
        assertNotNull(downloads.row(unrelated.uri));
        assertNotNull(downloads.row(phone));
    }

    @Test
    public void failedSdWriteCreatesNoPhoneMediaOrInternalFallback() throws IOException {
        TestOutputStorageCard card = enableSd();
        downloads.failWrites = true;
        assertThrows(IOException.class, () -> AndroidRouteGpxAutoSaver.saveUri(card.context, GPX));
        assertEquals(0, downloads.count());
        assertFalse(new File(app.getFilesDir(), "gpx").exists());
        assertEquals("abcd-1234", AndroidOutputStorage.current(card.context, Kind.GPX).volume);
    }

    private TestOutputStorageCard enableSd() throws IOException {
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        AndroidOutputStorageAccess.enable(card.context, AndroidOutputStorageVolumes.find(card.context, ""), null);
        return card;
    }
}
