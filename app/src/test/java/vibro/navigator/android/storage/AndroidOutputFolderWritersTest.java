package vibro.navigator.android.storage;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import vibro.navigator.android.export.AndroidRouteGpxAutoSaver;
import vibro.navigator.android.export.AndroidRouteGpxViewIntent;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {29, 35}, shadows = TestOutputMediaDirs.class)
public class AndroidOutputFolderWritersTest {
    private static final String GPX = "<gpx>route</gpx>";
    private static final String MEDIA = "media";
    private static final String TAG = "Test";
    private final Context context = ApplicationProvider.getApplicationContext();
    private TestDownloadsProvider provider;

    @Before
    public void setUp() {
        AppOutputStorageSettings.disable(context);
        AndroidOutputStorage.resetFailures();
        provider = TestDownloadsProvider.install(context);
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
    }

    @Test
    public void manualAndAutomaticGpxUseDownloadsWithReadGrants() throws IOException {
        Uri auto = AndroidRouteGpxAutoSaver.saveUri(context, GPX);
        Intent manual = AndroidRouteGpxViewIntent.create(context, GPX);
        assertEquals(MEDIA, auto.getAuthority());
        assertNotEquals(auto, manual.getData());
        assertEquals(GPX, provider.read(auto));
        assertEquals(GPX, provider.read(manual.getData()));
        assertEquals("Download/ViBRo/gpx/", provider.row(auto).getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
        assertEquals(Integer.valueOf(0), provider.row(auto).getAsInteger(MediaStore.MediaColumns.IS_PENDING));
        assertTrue((manual.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    }

    @Test
    public void oldCustomFolderSelectionsAreIgnored() throws IOException {
        AppOutputFolderSettings.set(context, Kind.GPX, "content://old/tree/custom");
        assertEquals(MEDIA, AndroidRouteGpxAutoSaver.saveUri(context, GPX).getAuthority());
    }

    @Test
    public void rejectedDownloadsWriteRemovesPartialWithoutFallback() {
        provider.failWrites = true;
        assertThrows(IOException.class, () -> AndroidRouteGpxAutoSaver.saveUri(context, GPX));
        assertEquals(0, provider.count());
        assertTrue(AndroidOutputStorage.current(context, Kind.GPX).label.contains("Download/ViBRo/gpx"));
    }

    @Test
    public void failedPublicationRemovesPendingDownloadWithoutFallback() {
        provider.failPublish = true;
        assertThrows(IOException.class, () -> AndroidRouteGpxAutoSaver.saveUri(context, GPX));
        assertEquals(0, provider.count());
    }

    @Test
    public void logsPublishSystemDetailsAndKeepOneDestination() throws IOException {
        AppLogger.setLoggingEnabled(context, true);
        AppLogger.i(TAG, "first entry");
        AppLogger.anomaly(context, TAG, "anomaly entry", null);
        Uri uri = Uri.parse(AppLogger.getLogFilePath(context));
        String text = provider.read(uri);
        assertTrue(text.split("\n", 2)[0].contains("Log session system info"));
        assertTrue(text.contains("first entry"));
        assertTrue(text.contains("anomaly entry"));
        assertEquals(1, provider.count());
        assertEquals(Integer.valueOf(0), provider.row(uri).getAsInteger(MediaStore.MediaColumns.IS_PENDING));
        assertEquals("Download/ViBRo/logs/", provider.row(uri).getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
        AppLogger.setLoggingEnabled(context, false);
    }

    @Test
    public void disabledLoggingCreatesNoFileUntilAnomaly() throws IOException {
        AppLogger.i(TAG, "disabled message");
        assertEquals(0, provider.count());
        AppLogger.anomaly(context, TAG, "crash", null);
        assertEquals(1, provider.count());
        assertTrue(provider.read(Uri.parse(AppLogger.getLogFilePath(context))).contains("crash"));
        AppLogger.init(context);
    }

    @Test
    public void writeFailureDisablesLogsAndReenablingStartsASelectedFolderSession() throws IOException {
        AppLogger.setLoggingEnabled(context, true);
        AppLogger.i(TAG, "old history");
        Uri first = Uri.parse(AppLogger.getLogFilePath(context));
        provider.failWrites = true;
        AppLogger.refreshOutputFolder(context);
        assertFalse(AppLogger.isLoggingEnabled());
        AppLogger.i(TAG, "disabled entry");
        assertFalse(provider.read(first).contains("disabled entry"));
        assertEquals(1, provider.count());
        provider.failWrites = false;
        assertTrue(AndroidOutputStorage.isUsable(context, Kind.LOGS));
        AppLogger.setLoggingEnabled(context, true);
        AppLogger.i(TAG, "recovered entry");
        assertTrue(provider.read(Uri.parse(AppLogger.getLogFilePath(context))).contains("recovered entry"));
        AppLogger.setLoggingEnabled(context, false);
    }
}
