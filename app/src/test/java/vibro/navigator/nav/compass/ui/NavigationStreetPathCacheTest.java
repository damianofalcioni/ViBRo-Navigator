package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
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
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;

@RunWith(RobolectricTestRunner.class)
public class NavigationStreetPathCacheTest {
    @Test
    public void unchangedGeometryReusesPathAndMovementZoomOrOverlayRebuildsIt() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        CompassStreetOverlay streets = overlay(0d, 0.0005d);
        Path initial = cache.pathFor(streets, 0d, 0d, 100f, 1f);

        assertSame(initial, cache.pathFor(streets, 0d, 0d, 100f, 1f));
        Path moved = cache.pathFor(streets, 0.0001d, 0d, 100f, 1f);
        assertNotSame(initial, moved);
        Path zoomed = cache.pathFor(streets, 0.0001d, 0d, 100f, 2f);
        assertNotSame(moved, zoomed);
        assertNotSame(zoomed, cache.pathFor(overlay(0d, 0.0006d), 0.0001d, 0d, 100f, 2f));
    }

    @Test
    public void cachedNorthUpGeometryMatchesGeographicProjectionAndUpdatesWithLocation() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        CompassStreetOverlay streets = overlay(0d, 0.0005d);
        List<ShadowPath.Point> points = points(cache.pathFor(streets, 0d, 0d, 100f, 1f));

        assertEquals(2, points.size());
        assertEquals(0f, points.get(0).getX(), 0.001f);
        assertEquals(0f, points.get(0).getY(), 0.001f);
        assertEquals(-55.66f, points.get(1).getY(), 0.001f);

        points = points(cache.pathFor(streets, 0.0001d, 0d, 100f, 1f));
        assertEquals(11.132f, points.get(0).getY(), 0.001f);
        assertEquals(-44.528f, points.get(1).getY(), 0.001f);
    }

    @Test
    public void crossingStreetWithBothEndsOffscreenSurvivesAndFarStreetIsCulled() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        Path crossing = cache.pathFor(overlay(-0.01d, 0.01d), 0d, 0d, 100f, 1f);
        assertEquals(2, points(crossing).size());
        assertTrue(points(crossing).get(0).getY() > 124f);
        assertTrue(points(crossing).get(1).getY() < -124f);

        Path far = cache.pathFor(overlay(0.01d, 0.02d), 0d, 0d, 100f, 1f);
        assertTrue(far.isEmpty());
    }

    @Test
    public void clearingCacheReleasesOldGeometry() {
        NavigationStreetPathCache cache = new NavigationStreetPathCache();
        CompassStreetOverlay streets = overlay(0d, 0.0005d);
        Path initial = cache.pathFor(streets, 0d, 0d, 100f, 1f);
        cache.clear();
        assertNotSame(initial, cache.pathFor(streets, 0d, 0d, 100f, 1f));
    }

    private static CompassStreetOverlay overlay(double firstLat, double lastLat) {
        return new CompassStreetOverlay(Collections.singletonList(new CompassStreetSegment(Arrays.asList(
                new LatLon(firstLat, 0d), new LatLon(lastLat, 0d)
        ))));
    }

    private static List<ShadowPath.Point> points(Path path) {
        return Shadows.shadowOf(path).getPoints();
    }
}
