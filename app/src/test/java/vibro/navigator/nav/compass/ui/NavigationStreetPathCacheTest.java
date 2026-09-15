package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.graphics.Path;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetType;

@RunWith(RobolectricTestRunner.class)
public class NavigationStreetPathCacheTest {
    @Test
    public void unchangedGeometryReusesPathAndMovementZoomOrOverlayRebuildsIt() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        CompassStreetOverlay streets = overlay(0d, 0.0005d);
        NavigationStreetPaths initial = cache.pathsFor(streets, 0d, 0d, 100f, 1f);

        assertSame(initial, cache.pathsFor(streets, 0d, 0d, 100f, 1f));
        NavigationStreetPaths moved = cache.pathsFor(streets, 0.0001d, 0d, 100f, 1f);
        assertNotSame(initial, moved);
        NavigationStreetPaths zoomed = cache.pathsFor(streets, 0.0001d, 0d, 100f, 2f);
        assertNotSame(moved, zoomed);
        assertNotSame(zoomed, cache.pathsFor(overlay(0d, 0.0006d), 0.0001d, 0d, 100f, 2f));
    }

    @Test
    public void cachedNorthUpGeometryMatchesGeographicProjectionAndUpdatesWithLocation() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        CompassStreetOverlay streets = overlay(0d, 0.0005d);
        List<ShadowPath.Point> points = points(walkingPath(cache.pathsFor(streets, 0d, 0d, 100f, 1f)));

        assertEquals(2, points.size());
        assertEquals(0f, points.get(0).getX(), 0.001f);
        assertEquals(0f, points.get(0).getY(), 0.001f);
        assertEquals(-55.66f, points.get(1).getY(), 0.001f);

        points = points(walkingPath(cache.pathsFor(streets, 0.0001d, 0d, 100f, 1f)));
        assertEquals(11.132f, points.get(0).getY(), 0.001f);
        assertEquals(-44.528f, points.get(1).getY(), 0.001f);
    }

    @Test
    public void crossingStreetWithBothEndsOffscreenSurvivesAndFarStreetIsCulled() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        Path crossing = walkingPath(cache.pathsFor(overlay(-0.01d, 0.01d), 0d, 0d, 100f, 1f));
        assertEquals(2, points(crossing).size());
        assertTrue(points(crossing).get(0).getY() > 124f);
        assertTrue(points(crossing).get(1).getY() < -124f);

        Path far = walkingPath(cache.pathsFor(overlay(0.01d, 0.02d), 0d, 0d, 100f, 1f));
        assertTrue(far.isEmpty());
    }

    @Test
    public void denseStreetGeometryIsReducedToVisiblePixelPrecision() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        java.util.ArrayList<LatLon> geoPoints = new java.util.ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            geoPoints.add(new LatLon(i * 0.000001d, 0d));
        }
        CompassStreetOverlay streets = new CompassStreetOverlay(Collections.singletonList(
                new CompassStreetSegment(geoPoints, CompassStreetType.FOOTWAY)
        ));

        assertTrue(points(walkingPath(cache.pathsFor(streets, 0d, 0d, 100f, 1f))).size() < geoPoints.size());
    }

    @Test
    public void cachedGeometrySeparatesStreetCategories() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        CompassStreetOverlay streets = new CompassStreetOverlay(Arrays.asList(
                segment(CompassStreetType.MOTORWAY, 0d),
                segment(CompassStreetType.RESIDENTIAL, 0.0002d),
                segment(CompassStreetType.FOOTWAY, 0.0004d),
                segment(CompassStreetType.RAILWAY, 0.0006d)
        ));

        NavigationStreetPaths paths = cache.pathsFor(streets, 0d, 0d, 100f, 1f);

        assertFalse(paths.pathFor(CompassStreetCategory.HIGHWAY).isEmpty());
        assertFalse(paths.pathFor(CompassStreetCategory.NORMAL).isEmpty());
        assertFalse(paths.pathFor(CompassStreetCategory.WALKING_CYCLING).isEmpty());
        assertFalse(paths.pathFor(CompassStreetCategory.SPECIAL_ROUTING).isEmpty());
        assertEquals(2, points(paths.pathFor(CompassStreetCategory.HIGHWAY)).size());
        assertEquals(2, points(paths.pathFor(CompassStreetCategory.NORMAL)).size());
        assertEquals(2, points(paths.pathFor(CompassStreetCategory.WALKING_CYCLING)).size());
        assertEquals(2, points(paths.pathFor(CompassStreetCategory.SPECIAL_ROUTING)).size());
    }

    @Test
    public void clearingCacheReleasesOldGeometry() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        CompassStreetOverlay streets = overlay(0d, 0.0005d);
        NavigationStreetPaths initial = cache.pathsFor(streets, 0d, 0d, 100f, 1f);
        cache.clear();
        assertNotSame(initial, cache.pathsFor(streets, 0d, 0d, 100f, 1f));
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

    private static Path walkingPath(NavigationStreetPaths paths) {
        return paths.pathFor(CompassStreetCategory.WALKING_CYCLING);
    }

    private static List<ShadowPath.Point> points(Path path) {
        return Shadows.shadowOf(path).getPoints();
    }
}
