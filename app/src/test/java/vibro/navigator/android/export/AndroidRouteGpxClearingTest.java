package vibro.navigator.android.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import vibro.navigator.android.storage.TestOutputDocumentsProvider;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AndroidRouteGpxClearingTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private TestOutputDocumentsProvider provider;

    @Before
    public void setUp() throws IOException {
        provider = TestOutputDocumentsProvider.install(context);
        AppOutputFolderSettings.set(context, Kind.GPX, TestOutputDocumentsProvider.TREE.toString());
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
    }

    @Test
    public void clearRunsAfterQueuedExportWrites() throws InterruptedException {
        AndroidRouteGpxActions.autoSave(context, "<gpx />");
        assertTrue(clear());
        assertEquals(0, provider.names().length);
    }

    @Test
    public void rejectedDeletionReportsFailure() throws Exception {
        AndroidRouteGpxAutoSaver.saveUri(context, "<gpx />");
        provider.failDeletes = true;
        assertFalse(clear());
        assertEquals(1, provider.names().length);
    }

    private boolean clear() throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean();
        AndroidRouteGpxActions.clearFolder(context, TestOutputDocumentsProvider.TREE.toString(), cleared -> {
            result.set(cleared);
            completed.countDown();
        });
        assertTrue(completed.await(10, TimeUnit.SECONDS));
        return result.get();
    }
}
