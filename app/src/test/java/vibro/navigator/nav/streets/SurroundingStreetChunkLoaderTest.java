package vibro.navigator.nav.streets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetVisibility;

@RunWith(RobolectricTestRunner.class)
public class SurroundingStreetChunkLoaderTest {
    private static final SurroundingStreetChunkKey FIRST = SurroundingStreetChunkKey.fromIndexes(1, 1);
    private static final SurroundingStreetChunkKey SECOND = SurroundingStreetChunkKey.fromIndexes(1, 2);

    @Test
    public void interruptedReadStopsTheBatchWithoutThrowingOrClearingTheInterrupt() {
        AtomicInteger reads = new AtomicInteger();
        var loader = loader((context, lat, lon, radius, maxSegments, visibility) -> {
            reads.incrementAndGet();
            Thread.currentThread().interrupt();
            throw new CancellationException("Street loading cancelled");
        });
        try {
            loader.load(List.of(FIRST, SECOND), CompassStreetVisibility.all());
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, reads.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void sourceCancellationRetriesTheAffectedChunkAndKeepsSuccessfulReads() {
        AtomicInteger reads = new AtomicInteger();
        var loader = loader((context, lat, lon, radius, maxSegments, visibility) -> {
            if (reads.getAndIncrement() == 0) {
                throw new CancellationException("Provider cancelled this read");
            }
            return CompassStreetOverlay.EMPTY;
        });
        var cache = new SurroundingStreetOverlayCache();
        var keys = List.of(FIRST, SECOND);
        var loaded = loader.load(keys, CompassStreetVisibility.all());
        loaded.putInto(cache, 5_000L);
        assertTrue(cache.missing(keys, 2).isEmpty());
        cache.expireReadFailures(5_000L);
        assertEquals(List.of(FIRST), cache.missing(keys, 2));
    }

    private static SurroundingStreetChunkLoader loader(SurroundingStreetRepository repository) {
        return new SurroundingStreetChunkLoader(ApplicationProvider.getApplicationContext(), repository);
    }
}
