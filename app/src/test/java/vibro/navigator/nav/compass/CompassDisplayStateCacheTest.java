package vibro.navigator.nav.compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Collections;

public class CompassDisplayStateCacheTest {
    @Test
    public void headingAndAccuracyRefreshPreserveAdjustedGeometryAndRadius() {
        CompassDisplayStateCache cache = new CompassDisplayStateCache();
        NavCompassState source = state(2000f, 100f);
        NavCompassState adjusted = cache.resolve(source, true, 200f);
        assertSame(adjusted, cache.resolve(source, true, 200f));

        NavCompassState refreshed = cache.resolve(NavCompassHeadingRefresh.apply(source, 359d, 8f), true, 200f);
        assertSame(adjusted.routePoints, refreshed.routePoints);
        assertSame(adjusted.radiusState, refreshed.radiusState);
        assertEquals(359f, refreshed.displayMode.headingDegrees, 0f);
        assertEquals(8f, refreshed.displayMode.headingAccuracyDegrees, 0f);
        assertEquals(200f, refreshed.radiusState.visibleRadiusMeters, 0f);
    }

    @Test
    public void structuralChangesRadiusChangesOverlayAndResetRebuildAdjustedState() {
        CompassDisplayStateCache cache = new CompassDisplayStateCache();
        NavCompassState source = state(2000f, 100f);
        NavCompassState initial = cache.resolve(source, true, 200f);
        NavCompassState changed = cache.resolve(state(2000f, 300f), true, 200f);
        assertNotSame(initial.routePoints, changed.routePoints);
        assertEquals(300f, changed.progressLabels.destinationEastMeters, 0f);

        NavCompassState zoomed = cache.resolve(source, true, 100f);
        assertEquals(100f, zoomed.radiusState.visibleRadiusMeters, 0f);
        CompassStreetOverlay overlay = new CompassStreetOverlay(Collections.emptyList());
        NavCompassState withStreets = cache.resolve(source.withStreetOverlay(overlay), true, 100f);
        assertSame(overlay, withStreets.streetOverlay);

        cache.clear();
        assertNotSame(withStreets, cache.resolve(source, true, 100f));
    }

    private static NavCompassState state(float radius, float destinationEast) {
        return NavCompassState.fromProjectedPoints(0f, null, 1f, radius, 5f,
                Collections.emptyList(), Collections.singletonList(new CompassRoutePoint(destinationEast, 0f)),
                Collections.emptyList(), destinationEast, 0f, true);
    }
}
