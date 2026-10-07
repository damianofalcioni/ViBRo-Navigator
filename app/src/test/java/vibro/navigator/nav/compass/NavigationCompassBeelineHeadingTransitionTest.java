package vibro.navigator.nav.compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

import vibro.navigator.nav.orientation.NavigationCompassModeController;

public class NavigationCompassBeelineHeadingTransitionTest {
    @Test
    public void roadHandoffRotatesOnDisplayFramesWithoutAnotherLocationOrSensorSample() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        controller.resolve(state(90f, true), 0L, false);
        NavCompassState road = state(0f, false);
        NavCompassState initial = controller.resolve(road, 1_000L, false);
        assertEquals(90f, initial.displayMode.headingDegrees, 0.001f);
        assertTrue(controller.isTransitionInProgress());
        assertSame(road.routePoints, initial.routePoints);
        assertSame(road.radiusState, initial.radiusState);
        assertEquals(road.displayMode.headingAccuracyDegrees, initial.displayMode.headingAccuracyDegrees);
        assertEquals(45f, controller.resolve(road, 1_500L, false).displayMode.headingDegrees, 0.001f);
        assertSame(road, controller.resolve(road, 2_000L, false));
        assertFalse(controller.isTransitionInProgress());
    }

    @Test
    public void beelineHeadingUpdatesImmediatelyAndHandoffStartsAtLatestDisplayedHeading() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        controller.resolve(state(80f, true), 0L);
        NavCompassState beeline = state(120f, true);
        assertSame(beeline, controller.resolve(beeline, 100L));
        assertFalse(controller.isTransitionInProgress());
        assertEquals(120f, controller.resolve(state(0f, false), 200L).displayMode.headingDegrees, 0.001f);
    }

    @Test
    public void northCrossingUsesShortestArcInBothDirections() {
        for (float start : new float[]{350f, 10f}) {
            NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
            controller.resolve(state(start, true), 0L);
            NavCompassState road = state(360f - start, false);
            controller.resolve(road, 100L);
            assertEquals(0f, controller.resolve(road, 600L).displayMode.headingDegrees, 0.001f);
            assertEquals(360f - start, controller.resolve(road, 1_100L).displayMode.headingDegrees, 0.001f);
        }
    }

    @Test
    public void changingRoadHeadingDoesNotRestartOrRetainStaleRoadGeometry() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        controller.resolve(state(90f, true), 0L);
        controller.resolve(state(0f, false), 100L);
        NavCompassState updatedRoad = state(30f, false);
        NavCompassState halfway = controller.resolve(updatedRoad, 600L);
        assertEquals(60f, halfway.displayMode.headingDegrees, 0.001f);
        assertSame(updatedRoad.routePoints, halfway.routePoints);
        assertSame(updatedRoad.progressLabels, halfway.progressLabels);
        assertSame(updatedRoad, controller.resolve(updatedRoad, 1_100L));
        assertFalse(controller.isTransitionInProgress());
    }

    @Test
    public void roadUpdateAcrossOppositeBearingKeepsTheChosenRotationDirection() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        controller.resolve(state(0f, true), 0L);
        controller.resolve(state(179f, false), 100L);
        assertEquals(89.5f, controller.resolve(state(179f, false), 600L).displayMode.headingDegrees, 0.001f);
        float updated = controller.resolve(state(181f, false), 610L).displayMode.headingDegrees;
        assertTrue(updated > 89.5f && updated < 100f);
        assertEquals(181f, controller.resolve(state(181f, false), 1_100L).displayMode.headingDegrees, 0.001f);
    }

    @Test
    public void returningToBeelineCancelsAnimationAndNextHandoffUsesThatHeading() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        controller.resolve(state(90f, true), 0L);
        controller.resolve(state(0f, false), 100L);
        controller.resolve(state(0f, false), 600L);
        NavCompassState beeline = state(200f, true);
        assertSame(beeline, controller.resolve(beeline, 700L));
        assertFalse(controller.isTransitionInProgress());
        assertEquals(200f, controller.resolve(state(0f, false), 800L).displayMode.headingDegrees, 0.001f);
    }

    @Test
    public void clearingNavigationOrStartingOnRoadDoesNotAnimateOrdinaryRoadHeadings() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        NavCompassState road = state(90f, false);
        assertSame(road, controller.resolve(road, 0L));
        NavCompassState curve = state(120f, false);
        assertSame(curve, controller.resolve(curve, 100L));
        controller.resolve(state(200f, true), 200L);
        controller.resolve(null, 300L);
        assertSame(road, controller.resolve(road, 400L));
        assertFalse(controller.isTransitionInProgress());
    }

    @Test
    public void delayedFrameSettlesHandoffAndViewModeChangesKeepRotationActive() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        NavCompassState beeline = state(90f, true);
        controller.resolve(beeline, 0L);
        controller.onCompassTapped(beeline, 0L, false);
        controller.resolve(beeline, 320L, false);
        NavCompassState road = state(0f, false);
        assertEquals(90f, controller.resolve(road, 400L, false).displayMode.headingDegrees, 0.001f);
        assertTrue(controller.isPerspectiveViewEnabled());
        assertEquals(45f, controller.resolve(road, 900L, false).displayMode.headingDegrees, 0.001f);
        assertEquals(0f, controller.resolve(road, 60_000L, false).displayMode.headingDegrees, 0.001f);
        assertFalse(controller.isTransitionInProgress());
    }

    private static NavCompassState state(float heading, boolean beeline) {
        return NavCompassState.fromProjectedPoints(new NavCompassProjectedPointsInput(
                new CompassDisplayMetrics(heading, beeline ? 8f : null, 1f, 1f, 1f, true),
                new CompassRadiusMetrics(300f, 300f, 300f, 5f, 13f),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                new CompassDestinationProjection(100f, 100f, 5f, true),
                beeline ? new CompassDestinationProjection(10f, 10f, 5f, true) : null,
                null));
    }
}
