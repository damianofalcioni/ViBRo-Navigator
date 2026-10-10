package vibro.navigator.android.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
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
import java.nio.file.Files;
import java.util.Arrays;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AndroidOutputFolderCleanerTest {
    private static final String LOG_NAME = "vibro-navigator-log-20261009120000.txt";
    private static final String GPX_NAME = "vibro-navigator-route-20261009120000.gpx";
    private static final String TREE = TestOutputDocumentsProvider.TREE.toString();
    private static final String TAG = "CleanerTest";
    private static final String ROOT = "output";
    private static final String PERSONAL_FILE = "personal.txt";
    private final Context context = ApplicationProvider.getApplicationContext();
    private TestOutputDocumentsProvider provider;

    @Before
    public void setUp() throws IOException {
        provider = TestOutputDocumentsProvider.install(context);
        AppOutputFolderSettings.set(context, Kind.LOGS, TREE);
        AppOutputFolderSettings.set(context, Kind.GPX, TREE);
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
    }

    @Test
    public void customClearPreservesOtherTypesUnrelatedFilesAndSubfolders() throws IOException {
        create(ROOT, LOG_NAME);
        create(ROOT, "vibro-navigator-log-20261009120000-2.txt");
        create(ROOT, GPX_NAME);
        create(ROOT, PERSONAL_FILE);
        create(ROOT, "vibro-navigator-log-notes.txt");
        String directory = provider.createDocument(ROOT, DocumentsContract.Document.MIME_TYPE_DIR,
                "vibro-navigator-log-20261009120001.txt");
        create(directory, LOG_NAME);

        AndroidOutputFolderCleaner.clear(context, Kind.LOGS, TREE);
        assertEquals(4, provider.names().length);
        assertFalse(Arrays.asList(provider.names()).contains(LOG_NAME));
        assertTrue(Arrays.asList(provider.names()).contains(GPX_NAME));
        assertTrue(Arrays.asList(provider.names()).contains(PERSONAL_FILE));
        assertEquals("", provider.read(directory + "/" + LOG_NAME));

        AndroidOutputFolderCleaner.clear(context, Kind.GPX, TREE);
        assertEquals(3, provider.names().length);
        assertFalse(Arrays.asList(provider.names()).contains(GPX_NAME));
    }

    @Test
    public void defaultClearRemovesGeneratedAndLegacyLogsWithoutRecursing() throws IOException {
        File root = new File(context.getFilesDir(), "logs");
        File generated = createFile(root, LOG_NAME);
        File legacy = createFile(root, "app-behavior.log");
        File unrelated = createFile(root, PERSONAL_FILE);
        File gpx = createFile(root, GPX_NAME);
        File nested = createFile(new File(root, "vibro-navigator-log-20261009120001.txt"), LOG_NAME);
        AndroidOutputFolderCleaner.clear(context, Kind.LOGS, null);
        assertFalse(generated.exists());
        assertFalse(legacy.exists());
        assertTrue(unrelated.exists());
        assertTrue(gpx.exists());
        assertTrue(nested.exists());
    }

    @Test
    public void defaultGpxClearHandlesInternalAndExternalFiles() throws IOException {
        File internal = createFile(new File(context.getFilesDir(), "gpx"), GPX_NAME);
        File external = createFile(new File(AndroidAppStorageDirs.preferredExternalFilesDir(context), "gpx"), GPX_NAME);
        File log = createFile(internal.getParentFile(), LOG_NAME);
        AndroidOutputFolderCleaner.clear(context, Kind.GPX, null);
        assertFalse(internal.exists());
        assertFalse(external.exists());
        assertTrue(log.exists());
    }

    @Test
    public void unavailableCustomFolderNeverDeletesDefaultFiles() throws IOException {
        File fallback = createFile(new File(context.getFilesDir(), "gpx"), GPX_NAME);
        provider.unavailable = true;
        assertThrows(IOException.class, () -> AndroidOutputFolderCleaner.clear(context, Kind.GPX, TREE));
        assertTrue(fallback.exists());
        assertEquals(TREE, AppOutputFolderSettings.get(context, Kind.GPX));
    }

    @Test
    public void clearKeepsTheFolderConfirmedEvenIfSelectionChanges() throws IOException {
        create(ROOT, GPX_NAME);
        String other = provider.createDocument(ROOT, DocumentsContract.Document.MIME_TYPE_DIR, "other");
        create(other, GPX_NAME);
        AppOutputFolderSettings.set(context, Kind.GPX, provider.tree(other).toString());
        AndroidOutputFolderCleaner.clear(context, Kind.GPX, TREE);
        assertEquals(1, provider.names().length);
        assertEquals("", provider.read(other + "/" + GPX_NAME));
    }

    @Test
    @Config(sdk = 35)
    public void freshLogAfterClearStartsWithSystemDetails() throws IOException {
        TestDownloadsProvider downloads = freshDownloads();
        AppLogger.setLoggingEnabled(context, true);
        AppLogger.i(TAG, "before clear");
        String confirmed = AndroidOutputStorage.current(context, Kind.LOGS).token();
        AppLogger.clearOutputFolder(context, confirmed);
        AppLogger.i(TAG, "after clear");
        String content = downloads.read(Uri.parse(AppLogger.getLogFilePath(context)));
        AppLogger.setLoggingEnabled(context, false);
        assertSystemDetailsFirst(content);
        assertTrue(content.contains("after clear"));
        assertFalse(content.contains("before clear"));
        assertEquals(1, downloads.count());
    }

    @Test
    @Config(sdk = 35)
    public void nextAnomalyAfterClearAlsoStartsWithSystemDetails() throws IOException {
        TestDownloadsProvider downloads = freshDownloads();
        AppLogger.anomaly(context, TAG, "old anomaly", null);
        AppLogger.clearOutputFolder(context, AndroidOutputStorage.current(context, Kind.LOGS).token());
        assertEquals(0, downloads.count());
        AppLogger.anomaly(context, TAG, "new anomaly", null);
        String content = downloads.read(Uri.parse(AppLogger.getLogFilePath(context)));
        AppLogger.init(context);
        assertSystemDetailsFirst(content);
        assertTrue(content.contains("new anomaly"));
        assertFalse(content.contains("old anomaly"));
    }

    @Test
    @Config(sdk = 35)
    public void failedDeletionDoesNotLeaveLoggingStopped() throws IOException {
        TestDownloadsProvider downloads = freshDownloads();
        AppLogger.setLoggingEnabled(context, true);
        downloads.failDeletes = true;
        String confirmed = AndroidOutputStorage.current(context, Kind.LOGS).token();
        assertThrows(IOException.class, () -> AppLogger.clearOutputFolder(context, confirmed));
        AppLogger.i(TAG, "after rejected deletion");
        String content = downloads.read(Uri.parse(AppLogger.getLogFilePath(context)));
        AppLogger.setLoggingEnabled(context, false);
        assertSystemDetailsFirst(content);
        assertTrue(content.contains("after rejected deletion"));
    }

    private TestDownloadsProvider freshDownloads() {
        TestDownloadsProvider downloads = TestDownloadsProvider.install(context);
        vibro.navigator.settings.AppOutputStorageSettings.disable(context);
        AndroidOutputStorage.resetFailures();
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
        return downloads;
    }

    private void create(String parent, String name) throws IOException {
        provider.createDocument(parent, "text/plain", name);
    }

    private static File createFile(File folder, String name) throws IOException {
        Files.createDirectories(folder.toPath());
        File file = new File(folder, name);
        Files.write(file.toPath(), new byte[0]);
        return file;
    }

    private static void assertSystemDetailsFirst(String content) {
        String first = content.split("\n", 2)[0];
        assertTrue(first.contains("INFO/AppLogger"));
        assertTrue(first.contains("Log session system info"));
        assertTrue(first.contains("androidVersion="));
        assertTrue(first.contains("appVersion="));
    }
}
