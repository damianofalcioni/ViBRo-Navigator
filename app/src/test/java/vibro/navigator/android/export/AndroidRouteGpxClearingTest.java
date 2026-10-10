package vibro.navigator.android.export;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.android.storage.TestDownloadsProvider;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AndroidRouteGpxClearingTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private TestDownloadsProvider provider;

    @Before
    public void setUp() {
        provider = TestDownloadsProvider.install(context);
        AppOutputStorageSettings.disable(context);
        AndroidOutputStorage.resetFailures();
    }

    @Test
    public void clearRunsAfterQueuedExportWrites() throws InterruptedException {
        AndroidRouteGpxActions.autoSave(context, "<gpx />");
        assertTrue(clear());
        assertEquals(0, provider.count());
    }

    @Test
    public void rejectedDeletionReportsFailure() throws Exception {
        AndroidRouteGpxAutoSaver.saveUri(context, "<gpx />");
        provider.failDeletes = true;
        assertFalse(clear());
        assertEquals(1, provider.count());
    }

    private boolean clear() throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean();
        AndroidRouteGpxActions.clearFolder(context, AndroidOutputStorage.current(context, Kind.GPX).token(), cleared -> {
            result.set(cleared);
            completed.countDown();
        });
        assertTrue(completed.await(10, TimeUnit.SECONDS));
        return result.get();
    }
}
