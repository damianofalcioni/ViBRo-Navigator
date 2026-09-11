package vibro.navigator.nav.streets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

public class SurroundingStreetCacheBudgetTest {
    private final SurroundingStreetOverlayCache cache = new SurroundingStreetOverlayCache();

    @Test
    public void denseViewportRetainsNearestChunksWithoutReloadingEvictedChunks() {
        List<SurroundingStreetChunkKey> keys = keys(64);
        load(keys, denseOverlay(4));

        assertTrue(cache.contains(keys.get(0)));
        assertTrue(cache.contains(keys.get(39)));
        assertFalse(cache.contains(keys.get(40)));
        assertFalse(cache.contains(keys.get(63)));
        assertTrue(cache.missing(keys, 64).isEmpty());
    }

    @Test
    public void pointBudgetAlsoStopsReloadsAndKeepsNearestChunks() {
        List<SurroundingStreetChunkKey> keys = keys(64);
        load(keys, denseOverlay(8));

        assertTrue(cache.contains(keys.get(19)));
        assertFalse(cache.contains(keys.get(20)));
        assertTrue(cache.missing(keys, 64).isEmpty());
    }

    @Test
    public void prefetchCannotEvictVisibleStreetsFromFullCache() {
        List<SurroundingStreetChunkKey> keys = keys(40);
        CompassStreetOverlay overlay = denseOverlay(4);
        load(keys, overlay);
        SurroundingStreetChunkKey ahead = SurroundingStreetChunkKey.fromIndexes(100, 0);

        cache.put(ahead, overlay);

        assertTrue(cache.contains(keys.get(0)));
        assertTrue(cache.contains(keys.get(39)));
        assertFalse(cache.contains(ahead));
        assertTrue(cache.missing(Collections.singletonList(ahead), 64).isEmpty());
    }

    @Test
    public void movingViewportCanLoadPreviouslyEvictedArea() {
        List<SurroundingStreetChunkKey> keys = keys(64);
        CompassStreetOverlay overlay = denseOverlay(4);
        load(keys, overlay);
        SurroundingStreetChunkKey next = keys.get(63);
        List<SurroundingStreetChunkKey> nextViewport = Arrays.asList(next, keys.get(62));

        cache.setDisplayKeys(nextViewport);

        assertEquals(nextViewport, cache.missing(nextViewport, 64));
        cache.put(next, overlay);
        assertTrue(cache.contains(next));
        assertFalse(cache.contains(keys.get(0)));
    }

    private void load(List<SurroundingStreetChunkKey> keys, CompassStreetOverlay overlay) {
        cache.setDisplayKeys(keys);
        for (SurroundingStreetChunkKey key : keys) {
            cache.put(key, overlay);
        }
    }

    private static List<SurroundingStreetChunkKey> keys(int count) {
        List<SurroundingStreetChunkKey> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            keys.add(SurroundingStreetChunkKey.fromIndexes(i, 0));
        }
        return keys;
    }

    private static CompassStreetOverlay denseOverlay(int pointCount) {
        List<LatLon> points = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            points.add(new LatLon(i * 0.0001d, 0d));
        }
        return new CompassStreetOverlay(Collections.nCopies(1_000, new CompassStreetSegment(points)));
    }
}
