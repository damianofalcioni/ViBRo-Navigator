package vibro.navigator.nav.orientation;

import org.junit.Test;

import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.NavCompassState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NavigationCompassViewRetentionTest {
    @Test
    public void recalculationKeeps3dZoomAndInclinationWithReplacementRadius() {
        NavigationCompassModeController modes = new NavigationCompassModeController(() -> 0L);
        NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        NavCompassState original = NavigationCompassGestureStateTest.state(true, 400f);
        modes.onCompassTapped(original, 0L, false);
        gestures.apply(modes.resolve(original, 320L, false), modes.isPerspectiveViewEnabled());
        gestures.zoomBy(1);
        gestures.tiltBy(-0.5f);

        assertNull(modes.resolve(null, 1_000L, false));
        assertNull(gestures.apply(null, modes.isPerspectiveViewEnabled()));
        assertNull(modes.resolve(null, 2_000L, false));
        assertFalse(gestures.zoomBy(1));
        assertFalse(gestures.tiltBy(0.1f));
        NavCompassState replacement = NavigationCompassGestureStateTest.state(true, 800f);
        NavCompassState displayed = gestures.apply(modes.resolve(replacement, 3_000L, false),
                modes.isPerspectiveViewEnabled());

        assertTrue(modes.isPerspectiveViewEnabled());
        assertEquals(0.5f, gestures.perspectiveProgress(modes.perspectiveProgress()), 0f);
        assertEquals(400f * CompassPerspectiveScale.maximumViewportMultiplier(),
                displayed.radiusState.visibleRadiusMeters, 0.01f);
        assertFalse(modes.isTransitionInProgress());
    }

    @Test
    public void second2dCycleStepSurvivesMissingCompass() {
        NavigationCompassModeController modes = new NavigationCompassModeController(() -> 0L);
        NavCompassState source = NavigationCompassGestureStateTest.state(true, 400f);
        modes.onCompassTapped(source, 0L, false);
        modes.resolve(source, 320L, false);
        modes.onCompassTapped(source, 400L, false);
        modes.resolve(null, 800L, false);
        modes.resolve(source, 900L, false);
        modes.onCompassTapped(source, 1_000L, false);
        assertFalse(modes.resolve(source, 1_000L, false).displayMode.movingScaleActive);
    }

    @Test
    public void temporaryOverviewStillExpiresDuringRecalculation() {
        NavigationCompassModeController modes = new NavigationCompassModeController(() -> 0L);
        NavCompassState source = NavigationCompassGestureStateTest.state(true, 400f);
        modes.onCompassSwiped(source, false, false);
        modes.resolve(null, 1_000L, false);
        assertFalse(modes.resolve(source, 4_999L, false).displayMode.movingScaleActive);
        assertTrue(modes.resolve(source, 5_000L, false).displayMode.movingScaleActive);
    }

    @Test
    public void explicitNavigationResetClears3dAndGestureAdjustments() {
        NavigationCompassModeController modes = new NavigationCompassModeController(() -> 0L);
        NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        NavCompassState source = NavigationCompassGestureStateTest.state(true, 400f);
        modes.onCompassTapped(source, 0L, false);
        gestures.apply(modes.resolve(source, 100L, false), modes.isPerspectiveViewEnabled());
        gestures.zoomBy(2);
        gestures.tiltBy(-0.5f);

        modes.reset();
        gestures.reset();

        assertFalse(modes.isTransitionInProgress());
        assertFalse(modes.isPerspectiveViewEnabled());
        assertEquals(0f, modes.perspectiveProgress(), 0f);
        assertFalse(gestures.isZoomEnabled());
        assertFalse(gestures.isTiltEnabled());
        assertEquals(400f, gestures.apply(modes.resolve(source, 1_000L, false),
                modes.isPerspectiveViewEnabled()).radiusState.visibleRadiusMeters, 0f);
        assertEquals(1f, gestures.perspectiveProgress(1f), 0f);
    }

    @Test
    public void missingGeometryDuringTiltStillSettlesAtSelected3dView() {
        NavigationCompassModeController modes = new NavigationCompassModeController(() -> 0L);
        NavCompassState source = NavigationCompassGestureStateTest.state(true, 400f);
        modes.onCompassTapped(source, 0L, false);
        assertNull(modes.resolve(null, 100L, false));
        assertTrue(modes.isTransitionInProgress());
        assertNull(modes.resolve(null, 320L, false));
        assertFalse(modes.isTransitionInProgress());
        assertEquals(1f, modes.perspectiveProgress(), 0f);
        modes.resolve(source, 1_000L, false);
        assertTrue(modes.isPerspectiveViewEnabled());
    }
}
