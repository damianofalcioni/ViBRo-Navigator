package vibro.navigator.nav.compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Collections;

import vibro.navigator.nav.orientation.NavigationCompassModeController;
import vibro.navigator.nav.orientation.NavigationHeadingSource;

public class NavigationCompassBeelineHeadingHandoffTest {
    @Test
    public void reachingRoadKeepsSelectedHeadingWithoutATimedRotation() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        controller.resolve(state(84f, true), 0L, false);
        NavCompassState road = state(84f, false);
        assertSame(road, controller.resolve(road, 1_000L, false));
        assertSame(road, controller.resolve(road, 1_500L, false));
        assertFalse(controller.isTransitionInProgress());
        NavCompassState backward = state(180f, false);
        assertSame(backward, controller.resolve(backward, 2_000L, false));
        NavCompassState forward = NavCompassHeadingRefresh.apply(state(0f, false), 0.0, null,
                NavigationHeadingSource.ROUTE);
        assertEquals(180f, controller.resolve(forward, 5_000L, false).displayMode.headingDegrees, 0.001f);
        assertEquals(90f, controller.resolve(forward, 5_250L, false).displayMode.headingDegrees, 0.001f);
        assertSame(forward, controller.resolve(forward, 5_500L, false));
    }

    @Test
    public void perspectiveViewUsesSelectedHeadingThroughoutHandoff() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        NavCompassState beeline = state(84f, true);
        controller.resolve(beeline, 0L, false);
        controller.onCompassTapped(beeline, 0L, false);
        controller.resolve(beeline, 320L, false);
        assertEquals(180f, controller.resolve(state(180f, false), 400L, false)
                .displayMode.headingDegrees, 0.001f);
        assertFalse(controller.isTransitionInProgress());
    }

    private static NavCompassState state(float heading, boolean beeline) {
        NavCompassState state = NavCompassState.fromProjectedPoints(new NavCompassProjectedPointsInput(
                new CompassDisplayMetrics(heading, beeline ? 8f : null, 1f, 1f, 1f, true),
                new CompassRadiusMetrics(300f, 300f, 300f, 5f, 13f),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                new CompassDestinationProjection(100f, 100f, 5f, true),
                beeline ? new CompassDestinationProjection(10f, 10f, 5f, true) : null,
                null));
        return NavCompassHeadingRefresh.apply(state, (double) heading, 8f, NavigationHeadingSource.LOCATION);
    }
}
