package vibro.navigator.nav.streets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetType;
import vibro.navigator.nav.policy.NavigationSpeedBucket;

public class SurroundingStreetOverlayCacheTest {
    private final SurroundingStreetOverlayCache cache = new SurroundingStreetOverlayCache();

    @Test
    public void unchangedSelectionReusesOverlayAndUpdatedChunksInvalidateIt() {
        SurroundingStreetChunkKey first = key(0d, 0d);
        cache.put(first, overlay(segment(0d, 0d, 0.001d, 0d)));
        CompassStreetOverlay original = cache.overlayFor(Collections.singletonList(first), 10);

        assertSame(original, cache.overlayFor(Collections.singletonList(first), 10));

        cache.put(first, overlay(segment(0d, 0d, 0.002d, 0d)));
        assertNotSame(original, cache.overlayFor(Collections.singletonList(first), 10));
        cache.clear();
        assertTrue(cache.overlayFor(Collections.singletonList(first), 10).isEmpty());
    }

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
    public void overlayFor_reusesSnapshotWhenSelectionOrderChanges() {
        SurroundingStreetChunkKey first = key(0.0d, 0.0d);
        SurroundingStreetChunkKey second = key(0.02d, 0.0d);
        cache.put(first, overlay(segment(0.0d, 0.0d, 0.001d, 0.0d)));
        cache.put(second, overlay(segment(0.02d, 0.0d, 0.021d, 0.0d)));

        cache.setDisplayKeys(Arrays.asList(first, second));
        CompassStreetOverlay original = cache.overlayFor(Arrays.asList(first, second), 10);

        cache.setDisplayKeys(Arrays.asList(second, first));
        assertSame(original, cache.overlayFor(Arrays.asList(second, first), 10));
    }

    @Test
    public void overlayFor_rebuildsWhenSelectedChunkSetChanges() {
        SurroundingStreetChunkKey first = key(0.0d, 0.0d);
        SurroundingStreetChunkKey second = key(0.02d, 0.0d);
        SurroundingStreetChunkKey replacement = key(0.04d, 0.0d);
        cache.put(first, overlay(segment(0.0d, 0.0d, 0.001d, 0.0d)));
        cache.put(second, overlay(segment(0.02d, 0.0d, 0.021d, 0.0d)));

        cache.setDisplayKeys(Arrays.asList(first, second));
        CompassStreetOverlay original = cache.overlayFor(Arrays.asList(first, second), 10);

        cache.setDisplayKeys(Arrays.asList(first, replacement));
        CompassStreetOverlay changed = cache.overlayFor(Arrays.asList(first, replacement), 10);
        assertNotSame(original, changed);
        assertEquals(1, changed.segments.size());

        cache.put(replacement, overlay(segment(0.04d, 0.0d, 0.041d, 0.0d)));
        assertEquals(2, cache.overlayFor(Arrays.asList(first, replacement), 10).segments.size());
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
                segment(CompassStreetType.RACEWAY, 0.006d),
                segment(CompassStreetType.LIVING_STREET, 0.007d),
                segment(CompassStreetType.TRACK, 0.008d),
                segment(CompassStreetType.BRIDLEWAY, 0.009d),
                segment(CompassStreetType.ROUTE_WALKING_CYCLING, 0.010d),
                segment(CompassStreetType.RAILWAY, 0.011d)
        )));

        assertEquals(12, cache.overlayFor(
                Collections.singletonList(first),
                20,
                NavigationSpeedBucket.LOW
        ).segments.size());
        assertTypes(
                cache.overlayFor(Collections.singletonList(first), 20, NavigationSpeedBucket.MEDIUM),
                CompassStreetType.TERTIARY,
                CompassStreetType.SECONDARY,
                CompassStreetType.MOTORWAY,
                CompassStreetType.VIA_FERRATA,
                CompassStreetType.RACEWAY,
                CompassStreetType.BRIDLEWAY,
                CompassStreetType.RAILWAY
        );
        assertTypes(
                cache.overlayFor(Collections.singletonList(first), 20, NavigationSpeedBucket.HIGH),
                CompassStreetType.SECONDARY,
                CompassStreetType.MOTORWAY,
                CompassStreetType.VIA_FERRATA,
                CompassStreetType.RACEWAY,
                CompassStreetType.BRIDLEWAY,
                CompassStreetType.RAILWAY
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
