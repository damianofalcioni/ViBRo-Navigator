package vibro.navigator.nav.compass;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import vibro.navigator.nav.orientation.NavigationCompassModeController;
import vibro.navigator.nav.orientation.NavigationHeadingSource;

public class NavigationCompassHeadingSourceTransitionTest {
    @Test
    public void everySourceHandoffRotatesForHalfASecondOnAnimationFrames() {
        NavigationHeadingSource[] sources = {NavigationHeadingSource.ROUTE,
                NavigationHeadingSource.LOCATION, NavigationHeadingSource.COMPASS};
        for (NavigationHeadingSource from : sources) {
            for (NavigationHeadingSource to : sources) {
                if (from != to) {
                    assertHandoff(from, to);
                }
            }
        }
    }

    private static void assertHandoff(NavigationHeadingSource from, NavigationHeadingSource to) {
        NavigationCompassModeController controller = controller();
        controller.resolve(state(90f, from), 0L, false);
        NavCompassState target = state(270f, to);
        NavCompassState initial = controller.resolve(target, 100L, false);
        assertEquals(90f, initial.displayMode.headingDegrees, 0.001f);
        assertSame(target.routePoints, initial.routePoints);
        assertEquals(target.displayMode.headingAccuracyDegrees, initial.displayMode.headingAccuracyDegrees);
        assertTrue(controller.isTransitionInProgress());
        assertEquals(0f, controller.resolve(target, 350L, false).displayMode.headingDegrees, 0.001f);
        assertSame(target, controller.resolve(target, 600L, false));
        assertFalse(controller.isTransitionInProgress());
        NavCompassState live = state(280f, to);
        assertSame(live, controller.resolve(live, 700L, false));
    }

    @Test
    public void northCrossingUsesTheShortArcAndEqualHeadingsNeedNoFrames() {
        for (float start : new float[]{350f, 10f}) {
            NavigationCompassModeController controller = controller();
            controller.resolve(state(start, NavigationHeadingSource.ROUTE), 0L, false);
            NavCompassState target = state(360f - start, NavigationHeadingSource.LOCATION);
            controller.resolve(target, 100L, false);
            assertEquals(0f, controller.resolve(target, 350L, false).displayMode.headingDegrees, 0.001f);
            assertSame(target, controller.resolve(target, 600L, false));
            NavCompassState compass = state(360f - start, NavigationHeadingSource.COMPASS);
            assertSame(compass, controller.resolve(compass, 700L, false));
            assertFalse(controller.isTransitionInProgress());
        }
    }

    @Test
    public void targetChangesKeepDurationAndChosenRotationAcrossOppositeBearing() {
        NavigationCompassModeController controller = controller();
        controller.resolve(state(0f, NavigationHeadingSource.ROUTE), 0L, false);
        controller.resolve(state(179f, NavigationHeadingSource.LOCATION), 100L, false);
        assertEquals(89.5f, controller.resolve(state(179f, NavigationHeadingSource.LOCATION), 350L, false)
                .displayMode.headingDegrees, 0.001f);
        float updated = controller.resolve(state(181f, NavigationHeadingSource.LOCATION), 360L, false)
                .displayMode.headingDegrees;
        assertTrue(updated > 89.5f && updated < 100f);
        assertEquals(181f, controller.resolve(state(181f, NavigationHeadingSource.LOCATION), 600L, false)
                .displayMode.headingDegrees, 0.001f);
    }

    @Test
    public void anotherSourceSwitchDuringRotationStartsFromTheDisplayedHeading() {
        NavigationCompassModeController controller = controller();
        controller.resolve(state(90f, NavigationHeadingSource.ROUTE), 0L, false);
        NavCompassState location = state(270f, NavigationHeadingSource.LOCATION);
        controller.resolve(location, 100L, false);
        assertEquals(0f, controller.resolve(location, 350L, false).displayMode.headingDegrees, 0.001f);
        NavCompassState compass = state(180f, NavigationHeadingSource.COMPASS);
        assertEquals(0f, controller.resolve(compass, 350L, false).displayMode.headingDegrees, 0.001f);
        assertEquals(270f, controller.resolve(compass, 600L, false).displayMode.headingDegrees, 0.001f);
        assertSame(compass, controller.resolve(compass, 850L, false));
    }

    @Test
    public void firstHeadingAndGeometryLossDoNotAnimateAndLateFramesSettle() {
        NavigationCompassModeController controller = controller();
        NavCompassState road = state(90f, NavigationHeadingSource.ROUTE);
        assertSame(road, controller.resolve(road, 0L, false));
        NavCompassState target = state(270f, NavigationHeadingSource.LOCATION);
        controller.resolve(target, 100L, false);
        assertSame(target, controller.resolve(target, 60_000L, false));
        assertFalse(controller.isTransitionInProgress());
        controller.resolve(road, 60_100L, false);
        controller.resolve(null, 60_200L, false);
        assertSame(target, controller.resolve(target, 60_300L, false));
        assertFalse(controller.isTransitionInProgress());
    }

    @Test
    public void unknownSourceCancelsAnimationAndOrdinaryUpdatesStayLive() {
        NavigationCompassModeController controller = controller();
        controller.resolve(state(90f, NavigationHeadingSource.ROUTE), 0L, false);
        NavCompassState ordinary = state(270f, NavigationHeadingSource.ROUTE);
        assertSame(ordinary, controller.resolve(ordinary, 100L, false));
        assertFalse(controller.isTransitionInProgress());
        controller.resolve(state(0f, NavigationHeadingSource.LOCATION), 200L, false);
        NavCompassState paused = state(0f, NavigationHeadingSource.UNKNOWN);
        assertSame(paused, controller.resolve(paused, 300L, false));
        assertFalse(controller.isTransitionInProgress());
    }

    @Test
    public void perspectiveAndCachedHeadingUpdatesPreserveSourceChanges() {
        NavigationCompassModeController controller = controller();
        NavCompassState road = state(90f, NavigationHeadingSource.ROUTE);
        controller.resolve(road, 0L, false);
        controller.onCompassTapped(road, 0L, false);
        controller.resolve(road, 320L, false);
        NavCompassState target = NavCompassHeadingRefresh.apply(road, 270.0, 12f, NavigationHeadingSource.LOCATION);
        assertEquals(90f, controller.resolve(target, 400L, false).displayMode.headingDegrees, 0.001f);
        assertEquals(0f, controller.resolve(target, 650L, false).displayMode.headingDegrees, 0.001f);
        assertEquals(270f, controller.resolve(target, 900L, false).displayMode.headingDegrees, 0.001f);
        assertEquals(NavigationHeadingSource.LOCATION, target.withDisplayMode(false).displayMode.headingSource);
    }

    private static NavigationCompassModeController controller() {
        return new NavigationCompassModeController(() -> 0L);
    }

    private static NavCompassState state(float heading, NavigationHeadingSource source) {
        Float accuracy = source == NavigationHeadingSource.ROUTE ? null : 12f;
        NavCompassState state = NavCompassState.fromProjectedPoints(heading, accuracy,
                1f, 300f, 5f, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), 100f, 100f, true);
        return NavCompassHeadingRefresh.apply(state, (double) heading, accuracy, source);
    }
}
