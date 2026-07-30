package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.io.File;

public class NavigationStopGpxAutoSaveTest {
    private static final String GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx />";

    @Test
    public void saveIfEnabled_doesNotBuildGpxWhenSettingDisabled() {
        CountingRouteGpxSource source = new CountingRouteGpxSource(GPX);
        RecordingRouteGpxSaver saver = new RecordingRouteGpxSaver();

        File file = NavigationStopGpxAutoSave.saveIfEnabled(false, source, saver);

        assertNull(file);
        assertEquals(0, source.calls);
        assertEquals(0, saver.calls);
    }

    @Test
    public void saveIfEnabled_writesGpxWhenSettingEnabled() {
        RecordingRouteGpxSaver saver = new RecordingRouteGpxSaver();

        File file = NavigationStopGpxAutoSave.saveIfEnabled(true, () -> GPX, saver);

        assertEquals(saver.file, file);
        assertEquals(GPX, saver.savedGpx);
    }

    @Test
    public void saveIfEnabled_skipsWhenNoRouteGpxIsAvailable() {
        RecordingRouteGpxSaver saver = new RecordingRouteGpxSaver();

        File file = NavigationStopGpxAutoSave.saveIfEnabled(true, () -> null, saver);

        assertNull(file);
        assertEquals(0, saver.calls);
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

    private static final class RecordingRouteGpxSaver implements NavigationStopGpxAutoSave.RouteGpxSaver {
        private final File file = new File("route.gpx");
        private int calls;
        private String savedGpx;

        @NonNull
        @Override
        public File save(@NonNull String gpx) {
            calls++;
            savedGpx = gpx;
            return file;
        }
    }
}
