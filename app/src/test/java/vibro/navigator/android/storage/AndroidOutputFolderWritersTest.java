package vibro.navigator.android.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import vibro.navigator.android.export.AndroidRouteGpxAutoSaver;
import vibro.navigator.android.export.AndroidRouteGpxViewIntent;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AndroidOutputFolderWritersTest {
    private static final String GPX = "<gpx>route</gpx>";
    private static final String TAG = "Test";
    private final Context context = ApplicationProvider.getApplicationContext();
    private TestOutputDocumentsProvider provider;

    @Before
    public void setUp() throws IOException {
        provider = TestOutputDocumentsProvider.install(context);
        AppOutputFolderSettings.set(context, Kind.GPX, TestOutputDocumentsProvider.TREE.toString());
        AppOutputFolderSettings.set(context, Kind.LOGS, TestOutputDocumentsProvider.TREE.toString());
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
    }

    @Test
    public void manualAndAutomaticGpxUseSameFolderWithReadGrants() throws IOException {
        Uri auto = AndroidRouteGpxAutoSaver.saveUri(context, GPX);
        Intent manual = AndroidRouteGpxViewIntent.create(context, GPX);
        assertEquals(TestOutputDocumentsProvider.TREE.getAuthority(), manual.getData().getAuthority());
        assertNotEquals(auto, manual.getData());
        assertTrue((manual.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertEquals(GPX, provider.read(DocumentsContract.getDocumentId(auto)));
        assertEquals(GPX, provider.read(DocumentsContract.getDocumentId(manual.getData())));
    }

    @Test
    public void collisionNeverOverwritesExistingDocument() throws IOException {
        Uri first = AndroidWritableDocumentTree.createFile(context, TestOutputDocumentsProvider.TREE,
                "text/plain", "route.gpx");
        Uri second = AndroidWritableDocumentTree.createFile(context, TestOutputDocumentsProvider.TREE,
                "text/plain", "route.gpx");
        assertTrue(DocumentsContract.getDocumentId(first).endsWith("/route.gpx"));
        assertTrue(DocumentsContract.getDocumentId(second).endsWith("/route-2.gpx"));
    }

    @Test
    public void unavailableGpxFolderSilentlyUsesDefaultAndReportsRedStatus() throws IOException {
        provider.unavailable = true;
        Uri saved = AndroidRouteGpxAutoSaver.saveUri(context, GPX);
        assertDefaultGpx(saved);
        assertFalse(AndroidOutputFolderAccess.isUsable(context, Kind.GPX));
    }

    @Test
    public void failedCustomWriteRemovesPartialFileAndUsesDefault() throws IOException {
        provider.failWrites = true;
        assertDefaultGpx(AndroidRouteGpxAutoSaver.saveUri(context, GPX));
        assertEquals(0, provider.names().length);
        assertFalse(AndroidOutputFolderAccess.isUsable(context, Kind.GPX));
    }

    @Test
    public void loggingWritesOnlyToSelectedFolderIncludingSystemDetailsAndAnomalies() throws IOException {
        int localCount = localLogCount();
        AppLogger.setLoggingEnabled(context, true);
        AppLogger.dMultiline(TAG, "Details", "line one\nline two");
        AppLogger.anomaly(context, TAG, "crash details", null);
        assertTrue(AppLogger.getLogFilePath(context).startsWith("content://vibro.test.documents/"));
        AppLogger.setLoggingEnabled(context, false);
        assertEquals(localCount, localLogCount());
        assertEquals(1, provider.names().length);
        String content = provider.read(provider.names()[0]);
        String firstLine = content.split("\n", 2)[0];
        assertTrue(firstLine.contains("INFO/AppLogger"));
        assertTrue(firstLine.contains("Log session system info"));
        assertTrue(content.contains("line one\nline two"));
        assertTrue(content.contains("crash details"));
    }

    @Test
    public void loggingSwitchesToDefaultWhenCustomFolderDisappears() throws IOException {
        AppLogger.setLoggingEnabled(context, true);
        AppLogger.i(TAG, "before failure");
        provider.unavailable = true;
        AppLogger.i(TAG, "after failure");
        String path = AppLogger.getLogFilePath(context);
        assertFalse(path.startsWith("content:"));
        String fallback = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
        assertTrue(fallback.contains("after failure"));
        assertFalse(fallback.contains("before failure"));
        assertFalse(AndroidOutputFolderAccess.isUsable(context, Kind.LOGS));
        provider.unavailable = false;
        assertTrue(AndroidOutputFolderAccess.isUsable(context, Kind.LOGS));
        AppLogger.i(TAG, "after recovery");
        String recovered = provider.read(DocumentsContract.getDocumentId(
                Uri.parse(AppLogger.getLogFilePath(context))));
        assertTrue(recovered.contains("after recovery"));
        assertFalse(recovered.contains("after failure"));
        assertFalse(new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8)
                .contains("after recovery"));
        AppLogger.setLoggingEnabled(context, false);
    }

    @Test
    public void changingLogFolderStartsNewFileWithoutCopyingOldHistory() throws IOException {
        AppOutputFolderSettings.set(context, Kind.LOGS, null);
        AppLogger.setLoggingEnabled(context, true);
        AppLogger.i(TAG, "old folder entry");
        AppOutputFolderSettings.set(context, Kind.LOGS, TestOutputDocumentsProvider.TREE.toString());
        AppLogger.refreshOutputFolder(context);
        AppLogger.i(TAG, "new folder entry");
        AppLogger.setLoggingEnabled(context, false);
        String custom = provider.read(provider.names()[0]);
        assertFalse(custom.contains("old folder entry"));
        assertTrue(custom.contains("new folder entry"));
    }

    @Test
    public void anomaliesUseCustomFolderWhenDetailedLoggingIsOff() throws IOException {
        int localCount = localLogCount();
        AppLogger.anomaly(context, TAG, "always saved", null);
        assertEquals(1, provider.names().length);
        assertTrue(provider.read(provider.names()[0]).contains("always saved"));
        assertEquals(localCount, localLogCount());
        AppLogger.init(context);
    }

    private void assertDefaultGpx(Uri saved) throws IOException {
        assertNotEquals(TestOutputDocumentsProvider.TREE.getAuthority(), saved.getAuthority());
        try (InputStream input = context.getContentResolver().openInputStream(saved)) {
            assertEquals(GPX, new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private int localLogCount() {
        return count(new File(context.getFilesDir(), "logs"))
                + count(new File(context.getExternalFilesDir(null), "logs"));
    }

    private static int count(File dir) {
        String[] files = dir.list();
        return files == null ? 0 : files.length;
    }
}
