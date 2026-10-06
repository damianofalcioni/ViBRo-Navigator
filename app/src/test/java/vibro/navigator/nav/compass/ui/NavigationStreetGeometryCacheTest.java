package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetType;

@RunWith(RobolectricTestRunner.class)
public class NavigationStreetGeometryCacheTest {
    @Test
    public void unchangedGeometryReusesBatchesAndMovementZoomOrOverlayRebuildsThem() {
        NavigationStreetGeometryCache cache = new NavigationStreetGeometryCache();
        CompassStreetOverlay streets = overlay(0d, 0.0005d);
        NavigationStreetBatches initial = cache.batchesFor(streets, 0d, 0d, 100f, 1f);

        assertSame(initial, cache.batchesFor(streets, 0d, 0d, 100f, 1f));
        NavigationStreetBatches moved = cache.batchesFor(streets, 0.0001d, 0d, 100f, 1f);
        assertNotSame(initial, moved);
        NavigationStreetBatches zoomed = cache.batchesFor(streets, 0.0001d, 0d, 100f, 2f);
        assertNotSame(moved, zoomed);
        assertNotSame(zoomed, cache.batchesFor(overlay(0d, 0.0006d), 0.0001d, 0d, 100f, 2f));
    }

    @Test
    public void cachedNorthUpGeometryMatchesGeographicProjectionAndUpdatesWithLocation() {
        NavigationStreetGeometryCache cache = new NavigationStreetGeometryCache();
        CompassStreetOverlay streets = overlay(0d, 0.0005d);
        float[] points = walkingLines(cache.batchesFor(streets, 0d, 0d, 100f, 1f));

        assertEquals(4, points.length);
        assertEquals(0f, points[0], 0.001f);
        assertEquals(0f, points[1], 0.001f);
        assertEquals(-55.66f, points[3], 0.001f);

        points = walkingLines(cache.batchesFor(streets, 0.0001d, 0d, 100f, 1f));
        assertEquals(11.132f, points[1], 0.001f);
        assertEquals(-44.528f, points[3], 0.001f);
    }

    @Test
    public void crossingStreetWithBothEndsOffscreenSurvivesAndFarStreetIsCulled() {
        NavigationStreetGeometryCache cache = new NavigationStreetGeometryCache();
        float[] crossing = walkingLines(cache.batchesFor(overlay(-0.01d, 0.01d), 0d, 0d, 100f, 1f));
        assertEquals(4, crossing.length);
        assertTrue(crossing[1] > 124f);
        assertTrue(crossing[3] < -124f);

        float[] far = walkingLines(cache.batchesFor(overlay(0.01d, 0.02d), 0d, 0d, 100f, 1f));
        assertEquals(0, far.length);
    }

    @Test
    public void denseStreetGeometryIsReducedToVisiblePixelPrecisionWithoutLosingItsEndpoint() {
        NavigationStreetGeometryCache cache = new NavigationStreetGeometryCache();
        ArrayList<LatLon> geoPoints = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            geoPoints.add(new LatLon(i * 0.000001d, 0d));
        }
        CompassStreetOverlay streets = new CompassStreetOverlay(Collections.singletonList(
                new CompassStreetSegment(geoPoints, CompassStreetType.FOOTWAY)
        ));
        float[] points = walkingLines(cache.batchesFor(streets, 0d, 0d, 100f, 1f));

        assertTrue(points.length / 4 + 1 < geoPoints.size());
        assertEquals(-2.2264f, points[points.length - 1], 0.001f);
    }

    @Test
    public void cachedGeometrySeparatesStreetCategories() {
        NavigationStreetGeometryCache cache = new NavigationStreetGeometryCache();
        CompassStreetOverlay streets = new CompassStreetOverlay(Arrays.asList(
                segment(CompassStreetType.MOTORWAY, 0d),
                segment(CompassStreetType.RESIDENTIAL, 0.0002d),
                segment(CompassStreetType.FOOTWAY, 0.0004d),
                segment(CompassStreetType.RAILWAY, 0.0006d)
        ));
        NavigationStreetBatches batches = cache.batchesFor(streets, 0d, 0d, 100f, 1f);

        for (CompassStreetCategory category : CompassStreetCategory.values()) {
            assertEquals(4, lines(batches, category).length);
        }
    }

    @Test
    public void clearingCacheReleasesOldGeometry() {
        NavigationStreetGeometryCache cache = new NavigationStreetGeometryCache();
        CompassStreetOverlay streets = overlay(0d, 0.0005d);
        NavigationStreetBatches initial = cache.batchesFor(streets, 0d, 0d, 100f, 1f);
        cache.clear();
        assertNotSame(initial, cache.batchesFor(streets, 0d, 0d, 100f, 1f));
    }

    private static CompassStreetOverlay overlay(double firstLat, double lastLat) {
        return new CompassStreetOverlay(Collections.singletonList(new CompassStreetSegment(
                Arrays.asList(new LatLon(firstLat, 0d), new LatLon(lastLat, 0d)),
                CompassStreetType.FOOTWAY
        )));
    }

    private static CompassStreetSegment segment(CompassStreetType type, double firstLat) {
        return new CompassStreetSegment(Arrays.asList(
                new LatLon(firstLat, 0d),
                new LatLon(firstLat + 0.0001d, 0d)
        ), type);
    }

    private static float[] walkingLines(NavigationStreetBatches batches) {
        return lines(batches, CompassStreetCategory.WALKING_CYCLING);
    }

    private static float[] lines(NavigationStreetBatches batches, CompassStreetCategory category) {
        RecordingCanvas canvas = new RecordingCanvas();
        batches.draw(canvas, category, new Paint());
        return canvas.points;
    }

    private static final class RecordingCanvas extends Canvas {
        float[] points = new float[0];

        @Override
        public void drawLines(float[] points, int offset, int count, Paint paint) {
            this.points = Arrays.copyOfRange(points, offset, offset + count);
        }
    }
}
