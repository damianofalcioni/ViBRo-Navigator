package vibro.navigator.nav.streets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

public class SurroundingStreetOverlayCacheTest {
    private final SurroundingStreetOverlayCache cache = new SurroundingStreetOverlayCache();

    @Test
    public void missing_returnsOnlyUncachedKeysAndKeepsEmptyChunksCached() {
        SurroundingStreetChunkKey first = key(0.0d, 0.0d);
        SurroundingStreetChunkKey second = key(0.02d, 0.0d);

        cache.put(first, CompassStreetOverlay.EMPTY);

        assertTrue(cache.contains(first));
        assertEquals(Collections.singletonList(second), cache.missing(Arrays.asList(first, second), 10));
    }

    @Test
    public void overlayFor_reusesCachedChunksAndDeduplicatesSegments() {
        SurroundingStreetChunkKey first = key(0.0d, 0.0d);
        SurroundingStreetChunkKey second = key(0.02d, 0.0d);
        CompassStreetSegment shared = segment(0.0d, 0.0d, 0.001d, 0.0d);
        CompassStreetSegment other = segment(0.02d, 0.0d, 0.021d, 0.0d);

        cache.put(first, overlay(shared));
        cache.put(second, new CompassStreetOverlay(Arrays.asList(shared, other)));

        CompassStreetOverlay overlay = cache.overlayFor(Arrays.asList(first, second), 10);

        assertFalse(overlay.isEmpty());
        assertEquals(2, overlay.segments.size());
    }

    @Test
    public void overlayFor_honorsDisplaySegmentLimit() {
        SurroundingStreetChunkKey first = key(0.0d, 0.0d);
        SurroundingStreetChunkKey second = key(0.02d, 0.0d);

        cache.put(first, overlay(segment(0.0d, 0.0d, 0.001d, 0.0d)));
        cache.put(second, overlay(segment(0.02d, 0.0d, 0.021d, 0.0d)));

        assertEquals(1, cache.overlayFor(Arrays.asList(first, second), 1).segments.size());
    }

    private static SurroundingStreetChunkKey key(double lat, double lon) {
        return SurroundingStreetChunkKey.from(lat, lon);
    }

    private static CompassStreetOverlay overlay(CompassStreetSegment segment) {
        return new CompassStreetOverlay(Collections.singletonList(segment));
    }

    private static CompassStreetSegment segment(double startLat, double startLon, double endLat, double endLon) {
        return new CompassStreetSegment(Arrays.asList(
                new LatLon(startLat, startLon),
                new LatLon(endLat, endLon)
        ));
    }
}
