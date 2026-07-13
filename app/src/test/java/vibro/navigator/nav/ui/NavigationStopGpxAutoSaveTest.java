package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppGpxSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationStopGpxAutoSaveTest {
    private static final String GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx />";

    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        AppLogger.init(context);
        AppGpxSettings.setAutoSaveOnStopEnabled(context, false);
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir != null) {
            deleteChildren(new File(externalFilesDir, "gpx"));
        }
        deleteChildren(new File(context.getFilesDir(), "gpx"));
    }

    @Test
    public void saveIfEnabled_doesNotBuildGpxWhenSettingDisabled() {
        CountingRouteGpxSource source = new CountingRouteGpxSource(GPX);

        File file = NavigationStopGpxAutoSave.saveIfEnabled(context, source);

        assertNull(file);
        assertEquals(0, source.calls);
    }

    @Test
    public void saveIfEnabled_writesGpxWhenSettingEnabled() throws Exception {
        AppGpxSettings.setAutoSaveOnStopEnabled(context, true);

        File file = NavigationStopGpxAutoSave.saveIfEnabled(context, () -> GPX);

        assertTrue(file.exists());
        assertEquals("gpx", file.getParentFile().getName());
        assertEquals(GPX, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void saveIfEnabled_skipsWhenNoRouteGpxIsAvailable() {
        AppGpxSettings.setAutoSaveOnStopEnabled(context, true);

        File file = NavigationStopGpxAutoSave.saveIfEnabled(context, () -> null);

        assertNull(file);
    }

    private static void deleteChildren(@NonNull File dir) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (!child.delete()) {
                throw new AssertionError("Failed to delete " + child.getAbsolutePath());
            }
        }
    }

    private static final class CountingRouteGpxSource implements NavigationStopGpxAutoSave.RouteGpxSource {
        private final String gpx;
        private int calls;

        CountingRouteGpxSource(@NonNull String gpx) {
            this.gpx = gpx;
        }

        @Override
        public String buildCurrentRouteGpx() {
            calls++;
            return gpx;
        }
    }
}
