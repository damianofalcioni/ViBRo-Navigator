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
import vibro.navigator.nav.compass.CompassStreetType;

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

    @Test
    public void overlayFor_filtersCachedSegmentsBySpeedBucket() {
        SurroundingStreetChunkKey first = key(0.0d, 0.0d);
        cache.put(first, new CompassStreetOverlay(Arrays.asList(
                segment(CompassStreetType.FOOTWAY, 0.0d),
                segment(CompassStreetType.TERTIARY, 0.001d),
                segment(CompassStreetType.SECONDARY, 0.002d),
                segment(CompassStreetType.MOTORWAY, 0.003d),
                segment(CompassStreetType.ELEVATOR, 0.004d),
                segment(CompassStreetType.VIA_FERRATA, 0.005d),
                segment(CompassStreetType.RACEWAY, 0.006d)
        )));

        assertEquals(7, cache.overlayFor(
                Collections.singletonList(first),
                10,
                SurroundingStreetSpeedBucket.LOW
        ).segments.size());
        assertTypes(
                cache.overlayFor(Collections.singletonList(first), 10, SurroundingStreetSpeedBucket.MEDIUM),
                CompassStreetType.TERTIARY,
                CompassStreetType.SECONDARY,
                CompassStreetType.MOTORWAY,
                CompassStreetType.RACEWAY
        );
        assertTypes(
                cache.overlayFor(Collections.singletonList(first), 10, SurroundingStreetSpeedBucket.HIGH),
                CompassStreetType.SECONDARY,
                CompassStreetType.MOTORWAY,
                CompassStreetType.RACEWAY
        );
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

    private static CompassStreetSegment segment(CompassStreetType type, double offset) {
        return new CompassStreetSegment(Arrays.asList(
                new LatLon(offset, 0.0d),
                new LatLon(offset + 0.0001d, 0.0d)
        ), type);
    }

    private static void assertTypes(CompassStreetOverlay overlay, CompassStreetType... types) {
        assertEquals(types.length, overlay.segments.size());
        for (int i = 0; i < types.length; i++) {
            assertEquals(types[i], overlay.segments.get(i).type);
        }
    }
}
