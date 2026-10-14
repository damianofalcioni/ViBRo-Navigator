package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.nav.export.NavigationRouteGpxSource;

public class NavigationStopGpxAutoSaveTest {
    private static final String GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx />";

    @Test
    public void disabledSavingDoesNotCaptureOrQueueAnExport() {
        AtomicInteger captures = new AtomicInteger();
        AtomicReference<NavigationRouteGpxSource> queued = new AtomicReference<>();

        NavigationStopGpxAutoSave.saveIfEnabled(false, () -> {
            captures.incrementAndGet();
            return () -> GPX;
        }, queued::set);

        assertEquals(0, captures.get());
        assertNull(queued.get());
    }

    @Test
    public void enabledSavingCapturesNowAndSerializesOnlyWhenWorkerRuns() {
        AtomicInteger builds = new AtomicInteger();
        AtomicReference<NavigationRouteGpxSource> queued = new AtomicReference<>();

        NavigationStopGpxAutoSave.saveIfEnabled(true, () -> () -> {
            builds.incrementAndGet();
            return GPX;
        }, queued::set);

        assertNotNull(queued.get());
        assertEquals(0, builds.get());
        assertEquals(GPX, queued.get().build());
        assertEquals(1, builds.get());
    }

    @Test
    public void missingSnapshotDoesNotQueueAnExport() {
        AtomicReference<NavigationRouteGpxSource> queued = new AtomicReference<>();

        NavigationStopGpxAutoSave.saveIfEnabled(true, () -> null, queued::set);

        assertNull(queued.get());
    }
}
