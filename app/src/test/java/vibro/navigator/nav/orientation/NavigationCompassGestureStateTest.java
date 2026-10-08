package vibro.navigator.nav.orientation;

import org.junit.Test;

import java.util.Collections;

import vibro.navigator.nav.compass.NavCompassState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NavigationCompassGestureStateTest {
    @Test
    public void fiveLevelsAreRelativeToTheCurrentAdaptiveRadius() {
        NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        NavCompassState source = state(true, 400f);
        assertSame(source, gestures.apply(source, false));
        gestures.zoomBy(-100);
        assertEquals(1600f, gestures.apply(source, false).radiusState.visibleRadiusMeters, 0.01f);
        for (float radius : new float[]{800f, 400f, 200f, 100f}) {
            assertTrue(gestures.zoomBy(1));
            assertEquals(radius, gestures.apply(source, false).radiusState.visibleRadiusMeters, 0.01f);
        }
        assertFalse(gestures.zoomBy(1));
        assertEquals(200f, gestures.apply(state(true, 800f), true).radiusState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void fullRoutePreservesItsRadiusAndRestoresMovingZoomOnReturn() {
        NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        NavCompassState moving = state(true, 400f);
        gestures.apply(moving, false);
        gestures.zoomBy(1);
        NavCompassState overview = state(false, 2000f);
        assertSame(overview, gestures.apply(overview, true));
        assertFalse(gestures.isZoomEnabled());
        assertFalse(gestures.zoomBy(-1));
        assertTrue(gestures.isTiltEnabled());
        assertEquals(200f, gestures.apply(moving, false).radiusState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void tiltIsBoundedAndOnlyRespondsIn3d() {
        NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        gestures.apply(state(true, 400f), false);
        assertFalse(gestures.tiltBy(-0.5f));
        gestures.apply(state(true, 400f), true);
        assertTrue(gestures.tiltBy(-0.5f));
        assertEquals(0.25f, gestures.perspectiveProgress(0.5f), 0.001f);
        gestures.tiltBy(-100f);
        assertEquals(0.25f, gestures.perspectiveProgress(1f), 0.001f);
        assertFalse(gestures.tiltBy(Float.NaN));
        gestures.tiltBy(100f);
        assertEquals(1.25f, gestures.perspectiveProgress(1f), 0.001f);
        assertFalse(gestures.tiltBy(0.1f));
    }

    @Test
    public void defaultInclinationCanAdjustInBothDirectionsWithoutRebuildingViewport() {
        NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        NavCompassState source = state(true, 400f);
        gestures.apply(source, true);
        gestures.zoomBy(1);
        NavCompassState zoomed = gestures.apply(source, true);
        assertTrue(gestures.tiltBy(0.125f));
        assertEquals(1.125f, gestures.perspectiveProgress(1f), 0.001f);
        assertSame(zoomed, gestures.apply(source, true));
        assertTrue(gestures.tiltBy(-0.25f));
        assertEquals(0.875f, gestures.perspectiveProgress(1f), 0.001f);
        assertSame(zoomed, gestures.apply(source, true));
    }

    @Test
    public void zoomReusesGeometryAndUpdatesDestinationVisibility() {
        NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        NavCompassState source = state(true, 400f);
        gestures.apply(source, true);
        gestures.zoomBy(1);
        NavCompassState zoomed = gestures.apply(source, true);
        assertSame(zoomed, gestures.apply(source, true));
        assertEquals(source.displayMode.headingDegrees, zoomed.displayMode.headingDegrees, 0f);
        assertEquals(source.radiusState.accuracyRadiusMeters, zoomed.radiusState.accuracyRadiusMeters, 0f);
        assertEquals(source.radiusState.movingScaleVisibleRadiusMeters,
                zoomed.radiusState.movingScaleVisibleRadiusMeters, 0f);
        assertFalse(zoomed.progressLabels.destinationWithinRadius);
    }

    @Test
    public void displayResetClearsAdjustments() {
        NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        NavCompassState source = state(true, 400f);
        gestures.apply(source, true);
        gestures.zoomBy(2);
        gestures.tiltBy(-0.5f);
        gestures.reset();
        assertFalse(gestures.isZoomEnabled());
        assertFalse(gestures.isTiltEnabled());
        assertSame(source, gestures.apply(source, true));
        assertEquals(1f, gestures.perspectiveProgress(1f), 0f);
    }

    static NavCompassState state(boolean moving, float radius) {
        return NavCompassState.fromProjectedPoints(0f, null, 5f, radius, 5f, moving, 10f,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                300f, 0f, radius >= 300f);
    }
}
